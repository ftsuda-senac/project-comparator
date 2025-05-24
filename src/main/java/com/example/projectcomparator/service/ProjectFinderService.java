package com.example.projectcomparator.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.projectcomparator.model.FileInfo;
import com.example.projectcomparator.model.Project;

@Service
public class ProjectFinderService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectFinderService.class);
    private static final Set<String> PROJECT_MARKERS = Set.of("pom.xml", "build.gradle", "build.gradle.kts");
    private static final Set<String> RELEVANT_EXTENSIONS = Set.of(
            ".java", ".html", ".css", ".js", ".xml", ".properties", ".yml", ".yaml", ".json", ".md", ".txt", ".sql"
            // Adicione outras extensões relevantes aqui
    );
    private static final Set<String> EXCLUDED_DIRS = Set.of(
		"target", "build", "bin", ".git", ".svn", ".idea", ".vscode", ".settings", "node_modules", "out", ".mvn", ".gradle", ".angular"
            // Adicione outros diretórios a serem ignorados
    );
    private static final Set<String> EXCLUDED_FILES_BY_NAME = Set.of( // Files to exclude by exact name
        // e.g. ".DS_Store"
    );
     private static final Set<String> EXCLUDED_EXTENSIONS = Set.of( // Extensions to always exclude
        ".class", ".jar", ".log", ".tmp", ".bak", ".zip", ".gz", ".png", ".jpg", ".jpeg", ".gif"
        // Adicione outras extensões a serem ignoradas
    );

    public List<Project> findProjects(Path parentDirectory, boolean webProject) {
        List<Project> projects = new ArrayList<>();
        if (!Files.isDirectory(parentDirectory)) {
            logger.error("O caminho fornecido não é um diretório: {}", parentDirectory);
            return projects;
        }

        try (Stream<Path> subDirectories = Files.list(parentDirectory).filter(Files::isDirectory)) {
            subDirectories.forEach(subDir -> {
				Path projectRoot = webProject ? findHtmlFile(subDir) : findProjectRootMarker(subDir);
                if (projectRoot != null) {
                    logger.info("Projeto {} encontrado em: {}", subDir, projectRoot.toAbsolutePath());
                    try {
                        Map<String, FileInfo> projectFiles = loadProjectFiles(projectRoot);
                        if (!projectFiles.isEmpty()) {
                            projects.add(new Project(subDir.getFileName().toString(), projectRoot, projectFiles));
                        } else {
                            logger.warn("Nenhum arquivo relevante encontrado para o projeto em: {}", projectRoot);
                        }
                    } catch (IOException | NoSuchAlgorithmException e) {
                        logger.error("Erro ao carregar arquivos do projeto {}: {}", projectRoot, e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Erro ao listar subdiretórios de {}: {}", parentDirectory, e.getMessage());
        }
        return projects;
    }

	private Path findHtmlFile(Path directoryToSearch) {
		try {
			logger.info("Directory to find: {}", directoryToSearch.toString());
			Optional<Path> markerFile = Files.walk(directoryToSearch, 3) // Limitar profundidade para otimizar
                    .filter(Files::isRegularFile)
					.filter(p -> {
						String fileName = p.getFileName().toString().toLowerCase();
						int extBegin = fileName.lastIndexOf('.');
						if (extBegin < 0) {
							logger.warn("Filename {} without extension", p.getFileName().toAbsolutePath().toString());
							return false;
						}
						String fileExt = fileName.substring(extBegin);
						return ".html".equals(fileExt) || ".htm".equals(fileExt);
					})
					.findFirst();
			return markerFile.map(Path::getParent).orElse(null);
		} catch (IOException e) {
            logger.error("Erro ao procurar por arquivo HTML de projeto em {}: {}", directoryToSearch, e.getMessage());
            return null;
        }
	}

    private Path findProjectRootMarker(Path directoryToSearch) {
        try {
            Optional<Path> markerFile = Files.walk(directoryToSearch, 3) // Limitar profundidade para otimizar
                    .filter(Files::isRegularFile)
                    .filter(p -> PROJECT_MARKERS.contains(p.getFileName().toString().toLowerCase()))
                    .findFirst();
            return markerFile.map(Path::getParent).orElse(null);
        } catch (IOException e) {
            logger.error("Erro ao procurar por marcador de projeto em {}: {}", directoryToSearch, e.getMessage());
            return null;
        }
    }

    private Map<String, FileInfo> loadProjectFiles(Path projectRoot) throws IOException, NoSuchAlgorithmException {
        Map<String, FileInfo> files = new LinkedHashMap<>();
        Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (EXCLUDED_DIRS.contains(dir.getFileName().toString().toLowerCase()) &&
                    !dir.equals(projectRoot)) { // Don't skip the project root itself if it matches an excluded name by chance
                    logger.debug("Ignorando diretório excluído: {}", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                String fileExtension = getFileExtension(fileName);

                if (EXCLUDED_FILES_BY_NAME.contains(fileName) || EXCLUDED_EXTENSIONS.contains(fileExtension) || !RELEVANT_EXTENSIONS.contains(fileExtension)) {
                    logger.trace("Ignorando arquivo: {}", file);
                    return FileVisitResult.CONTINUE;
                }

                try {
                    String relativePath = projectRoot.relativize(file).toString().replace("\\", "/"); // Normalizar separadores
                    String contentHash = calculateSHA256(file);
                    FileTime creationTime = attrs.creationTime();
                    FileTime lastModifiedTime = attrs.lastModifiedTime();

                    files.put(relativePath, new FileInfo(relativePath, file, creationTime, lastModifiedTime));

                    logger.debug("Arquivo adicionado: {}", relativePath);
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException("Erro ao calcular hash para " + file, e);
                } catch (Exception e) {
                    logger.error("Erro ao processar arquivo {}: {}", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                logger.warn("Não foi possível acessar o arquivo: {}. Causa: {}", file, exc != null ? exc.getMessage() : "N/A");
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot).toLowerCase();
        }
        return "";
    }

    private String calculateSHA256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(is, md)) {
            //noinspection StatementWithEmptyBody
            while (dis.read() != -1) ; // Ler o arquivo para calcular o hash
        }
        byte[] digest = md.digest();
        return bytesToHex(digest);
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
