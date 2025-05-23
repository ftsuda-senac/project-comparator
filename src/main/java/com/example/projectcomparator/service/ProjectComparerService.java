package com.example.projectcomparator.service;

import com.example.projectcomparator.model.ComparisonResult;
import com.example.projectcomparator.model.FileInfo;
import com.example.projectcomparator.model.Project;
import org.apache.commons.text.similarity.JaroWinklerSimilarity; // Novo import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException; // Novo import
import java.nio.charset.StandardCharsets; // Novo import
import java.nio.file.Files; // Novo import
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProjectComparerService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectComparerService.class);

    private static final double WEIGHT_CONTENT = 0.70;
    private static final double WEIGHT_CREATION_TIME = 0.15;
    private static final double WEIGHT_MODIFICATION_TIME = 0.15;

    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    /**
     * Normaliza o conteúdo do texto para comparação.
     * - Remove espaços/tabs no início/fim de cada linha.
     * - Substitui múltiplos espaços/tabs internos por um único espaço.
     * - Reduz múltiplas linhas em branco a uma única linha em branco.
     * - Normaliza quebras de linha para '\n'.
     */
    private String normalizeTextContent(String content) {
        if (content == null) return "";

        // Normaliza quebras de linha
        content = content.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        String[] lines = content.split("\n");
        StringBuilder normalized = new StringBuilder();
        boolean lastLineWasEffectivelyBlank = false;

        for (String line : lines) {
            String trimmedLine = line.trim().replaceAll("\\s+", " ");
            if (trimmedLine.isEmpty()) {
                if (!lastLineWasEffectivelyBlank) {
                    normalized.append("\n"); // Adiciona uma única linha em branco
                    lastLineWasEffectivelyBlank = true;
                }
            } else {
                normalized.append(trimmedLine).append("\n");
                lastLineWasEffectivelyBlank = false;
            }
        }
        // Remove a última nova linha se o StringBuilder não estiver vazio e terminar com \n
        if (normalized.length() > 0 && normalized.charAt(normalized.length() - 1) == '\n') {
            normalized.setLength(normalized.length() - 1);
        }
        return normalized.toString();
    }

    private double calculateFileContentSimilarity(FileInfo f1, FileInfo f2) {
        String content1 = "";
        String content2 = "";

        try {
            content1 = Files.readString(f1.getAbsolutePath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("Não foi possível ler o arquivo {}: {}", f1.getAbsolutePath(), e.getMessage());
            // Se não puder ler o arquivo 1, a similaridade depende se o arquivo 2 também não pode ser lido ou está vazio.
        }
        try {
            content2 = Files.readString(f2.getAbsolutePath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("Não foi possível ler o arquivo {}: {}", f2.getAbsolutePath(), e.getMessage());
            // Se não puder ler o arquivo 2...
        }
        
        // Se ambos os arquivos não puderam ser lidos, ou um não pode ser lido e o outro é vazio, considere-os similares nesse aspecto.
        // Ou, se um falhou e o outro tem conteúdo, são 0% similares.
        // Para simplificar: se um falha, tratamos seu conteúdo como vazio para fins de cálculo de similaridade.
        // Se ambos falham, conteúdo vazio vs vazio = 1.0 de similaridade.

        String normalizedContent1 = normalizeTextContent(content1);
        String normalizedContent2 = normalizeTextContent(content2);

        if (normalizedContent1.isEmpty() && normalizedContent2.isEmpty()) {
            return 1.0; // Dois arquivos vazios (ou que falharam na leitura) são considerados "idênticos" em conteúdo
        }
        if (normalizedContent1.isEmpty() || normalizedContent2.isEmpty()) {
             // Um vazio e outro não, após normalização
            if (!content1.isEmpty() && !content2.isEmpty()) { // Ambos tinham conteúdo original, mas um se tornou vazio após normalização
                 // e o outro não. Ex: um arquivo só com espaços e outro com texto.
                 // O Jaro-Winkler já lidaria com isso, mas podemos ser explícitos.
            } else { // Um era originalmente vazio/ilegível e o outro não.
                return 0.0;
            }
        }

        // Usar JaroWinklerSimilarity para obter um score entre 0.0 e 1.0
        return jaroWinkler.apply(normalizedContent1, normalizedContent2);
    }

    public ComparisonResult compareProjects(Project p1, Project p2) {
        Map<String, FileInfo> files1 = p1.getFiles();
        Map<String, FileInfo> files2 = p2.getFiles();

        Set<String> allRelativePaths = Stream.concat(files1.keySet().stream(), files2.keySet().stream())
                                             .collect(Collectors.toSet());

        if (allRelativePaths.isEmpty()) {
            if (files1.isEmpty() && files2.isEmpty()) {
                 return new ComparisonResult(p1, p2, true, 100.0);
            }
            return new ComparisonResult(p1, p2, false, 0.0);
        }

        double totalScore = 0;
        boolean allFilesPerfectMatchAccordingToCriteria = true; // Renomeado para clareza

        for (String path : allRelativePaths) {
            FileInfo f1 = files1.get(path);
            FileInfo f2 = files2.get(path);

            double currentFileWeightedScore = 0;

            if (f1 != null && f2 != null) {
                double fileContentScore = calculateFileContentSimilarity(f1, f2);
                double fileCreationTimeScore = java.util.Objects.equals(f1.getCreationTime(), f2.getCreationTime()) ? 1.0 : 0.0;
                double fileModificationTimeScore = java.util.Objects.equals(f1.getLastModifiedTime(), f2.getLastModifiedTime()) ? 1.0 : 0.0;

                currentFileWeightedScore = (fileContentScore * WEIGHT_CONTENT) +
                                           (fileCreationTimeScore * WEIGHT_CREATION_TIME) +
                                           (fileModificationTimeScore * WEIGHT_MODIFICATION_TIME);
                
                if (fileContentScore < 1.0 || fileCreationTimeScore < 1.0 || fileModificationTimeScore < 1.0) {
                    allFilesPerfectMatchAccordingToCriteria = false;
                }
            } else { // Arquivo existe em apenas um dos projetos
                allFilesPerfectMatchAccordingToCriteria = false;
                // Não adiciona ao score, pois é uma diferença. O denominador (allRelativePaths.size()) considera isso.
                // currentFileWeightedScore permanece 0 para este 'slot' de arquivo.
            }
            totalScore += currentFileWeightedScore;
        }

        double similarityPercentage = (allRelativePaths.isEmpty()) ? 100.0 : (totalScore / allRelativePaths.size()) * 100.0;
        
        // Para ser cópia exata, todos os arquivos devem existir em ambos, ter metadados idênticos,
        // e conteúdo perfeitamente similar (score 1.0 após normalização/comparação).
        // E as estruturas de arquivos devem ser idênticas.
        boolean exactCopy = allFilesPerfectMatchAccordingToCriteria &&
                            files1.size() == files2.size() &&
                            files1.size() == allRelativePaths.size(); 

        // Ajuste fino para o caso de 100%
        if (exactCopy && Math.abs(similarityPercentage - 100.0) > 0.0001) {
             // Se marcado como cópia exata, mas o percentual não é exatamente 100 (devido a precisão de float),
             // pode ser um falso positivo para exactCopy ou um falso negativo para 100%.
             // Se todos os scores individuais (conteúdo, criação, modificação) foram 1.0, então deveria ser 100.
             // A flag allFilesPerfectMatchAccordingToCriteria já garante isso.
             similarityPercentage = 100.0; // Força 100% se os critérios de exatidão foram atendidos.
        }
        if (!exactCopy && Math.abs(similarityPercentage - 100.0) < 0.0001 && files1.size() == files2.size() && files1.size() == allRelativePaths.size()) {
            // Caso onde a porcentagem é 99.999... mas não foi marcado como exactCopy (talvez por um critério não coberto)
            // Se a estrutura é idêntica e a similaridade é virtualmente 100%, pode ser considerado cópia exata.
            // No entanto, a lógica atual de allFilesPerfectMatchAccordingToCriteria deve ser suficiente.
            // Vamos manter a definição atual de exactCopy.
        }


        return new ComparisonResult(p1, p2, exactCopy, similarityPercentage);
    }
}