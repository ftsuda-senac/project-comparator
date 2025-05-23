package com.example.projectcomparator.model;

public class ComparisonResult {
    private final Project project1;
    private final Project project2;
    private final boolean exactCopy;
    private final double similarityPercentage;

    public ComparisonResult(Project project1, Project project2, boolean exactCopy, double similarityPercentage) {
        this.project1 = project1;
        this.project2 = project2;
        this.exactCopy = exactCopy;
        this.similarityPercentage = similarityPercentage;
    }

    // Getters
    public Project getProject1() { return project1; }
    public Project getProject2() { return project2; }
    public boolean isExactCopy() { return exactCopy; }
    public double getSimilarityPercentage() { return similarityPercentage; }

    @Override
    public String toString() {
        if (exactCopy) {
            return String.format("Projetos '%s' e '%s' SÃO CÓPIAS EXATAS um do outro.",
                                 project1.getName(), project2.getName());
        } else {
            return String.format("Projetos '%s' e '%s': Similaridade de %.2f%%.",
                                 project1.getName(), project2.getName(), similarityPercentage);
        }
    }
}