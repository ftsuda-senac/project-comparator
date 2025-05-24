package com.example.projectcomparator.model;

import java.nio.file.Path; // Novo import
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FileInfo {
    private final String relativePath;
    // Removido: private final String contentHash;
    private final Path absolutePath; // Adicionado
    private final FileTime creationTime;
    private final FileTime lastModifiedTime;

    public FileInfo(String relativePath, Path absolutePath, FileTime creationTime, FileTime lastModifiedTime) { // Construtor atualizado
        this.relativePath = relativePath;
        this.absolutePath = absolutePath; // Adicionado
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
    }

    // Getters
    public String getRelativePath() { return relativePath; }
    public Path getAbsolutePath() { return absolutePath; } // Adicionado
    public FileTime getCreationTime() { return creationTime; }
    public FileTime getLastModifiedTime() { return lastModifiedTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        // A igualdade de FileInfo agora se baseia mais em metadados e caminho,
        // a comparação de conteúdo será feita separadamente.
        return relativePath.equals(fileInfo.relativePath) &&
               absolutePath.equals(fileInfo.absolutePath) && // Comparar caminhos absolutos
               Objects.equals(creationTime, fileInfo.creationTime) &&
               Objects.equals(lastModifiedTime, fileInfo.lastModifiedTime);
    }

    @Override
    public int hashCode() {
        // O hashcode também reflete as mudanças
        return Objects.hash(relativePath, absolutePath, creationTime, lastModifiedTime);
    }

	private String formatDate(FileTime fileTime) {
		LocalDateTime localDate = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return localDate.format(formatter);
	}

	public String printFileInfo() {
		return relativePath + " " + formatDate(creationTime) + " " + formatDate(lastModifiedTime);
	}

    @Override
    public String toString() {
        return "FileInfo{" +
               "relativePath='" + relativePath + '\'' +
               ", absolutePath=" + absolutePath + // Adicionado
               ", creationTime=" + creationTime +
               ", lastModifiedTime=" + lastModifiedTime +
               '}';
    }
}
