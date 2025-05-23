package com.example.projectcomparator.model;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

public class Project {
    private final String name;
    private final Path rootPath; // Caminho absoluto para a raiz do projeto (onde pom.xml/build.gradle foi encontrado)
    private final Map<String, FileInfo> files; // Caminho relativo -> FileInfo

    public Project(String name, Path rootPath, Map<String, FileInfo> files) {
        this.name = name;
        this.rootPath = rootPath;
        this.files = files;
    }

    // Getters
    public String getName() { return name; }
    public Path getRootPath() { return rootPath; }
    public Map<String, FileInfo> getFiles() { return files; }

	public String printFileInfo() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("Files: " + files.size()  + "\r\n");
		for (Entry<String, FileInfo> fileEntry : files.entrySet()) {
			strBuilder.append(fileEntry.getValue().printFileInfo() + "\r\n");
		}
		return strBuilder.toString();
	}

    @Override
    public String toString() {
        return "Project{" +
               "name='" + name + '\'' +
               ", rootPath=" + rootPath +
               ", fileCount=" + files.size() +
               '}';
    }
}
