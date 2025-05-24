package com.example.projectcomparator;

import com.example.projectcomparator.model.ComparisonResult;
import com.example.projectcomparator.model.Project;
import com.example.projectcomparator.service.ExcelReportService;
import com.example.projectcomparator.service.HtmlReportService; // Importar novo serviço
import com.example.projectcomparator.service.ProjectComparerService;
import com.example.projectcomparator.service.ProjectFinderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException; // Importar IOException
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap; // Importar HashMap
import java.util.List;
import java.util.Locale;
import java.util.Map; // Importar Map
import java.util.function.Function;
import java.util.stream.Collectors; // Importar Collectors

// Base do projeto gerado no Google Gemini
@SpringBootApplication
public class ProjectComparatorCliApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProjectComparatorCliApplication.class);
    private static final String DEFAULT_BASE_FILENAME = "relatorio_similaridade_projetos";
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ProjectFinderService projectDiscoveryService;
    private final ProjectComparerService comparisonService;
    private final HtmlReportService htmlReportService; // Adicionar o novo serviço
    private final ExcelReportService excelReportService;

    // Atualizar construtor para injetar HtmlReportService
    public ProjectComparatorCliApplication(ProjectFinderService projectDiscoveryService, ProjectComparerService comparisonService,
                                           HtmlReportService htmlReportService, ExcelReportService excelReportService) {
        this.projectDiscoveryService = projectDiscoveryService;
        this.comparisonService = comparisonService;
        this.htmlReportService = htmlReportService;
        this.excelReportService = excelReportService;
    }

    public static void main(String[] args) {
        Locale.setDefault(new Locale("pt", "BR"));
        SpringApplication.run(ProjectComparatorCliApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        /*
        if (args.length == 0) {
            String usageMessage = "Uso: java -jar <seu-app>.jar <caminho_diretorio_pai> [arquivo_html_saida (opcional)]";
            logger.error(usageMessage);
            System.err.println("Por favor, forneça o caminho para o diretório pai dos projetos.");
            System.err.println(usageMessage);
            return;
        }
        */

        String parentPath = "E:\\senac\\pwb-25-1\\ado3";
		boolean webProject = true;
		// String parentPath = "E:\\projetos\\senac\\24-2\\dswa\\projetos\\teste";
		// boolean webProject = false;
		double similarityThreshold = webProject ? 40.0 : 20.0; // Usar 40 para HTML/CSS/JS e 20 para Java

        Path parentDirectory = Paths.get(parentPath);
        if (!Files.isDirectory(parentDirectory)) {
            logger.error("O caminho fornecido não é um diretório válido: {}", parentDirectory);
            System.err.println("O caminho fornecido não é um diretório válido: " + parentDirectory);
            return;
        }

        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String baseOutputFilename = (args.length > 1 && args[1] != null && !args[1].trim().isEmpty())
                                    ? args[1].trim()
                                    : DEFAULT_BASE_FILENAME + '_' + timestamp;

        // String htmlOutputFileName = baseOutputFilename + ".html";
        String excelOutputFileName = baseOutputFilename + ".xlsx";


        logger.info("Iniciando varredura de projetos em: {}", parentDirectory);
        List<Project> projects = projectDiscoveryService.findProjects(parentDirectory, webProject);

        if (projects.isEmpty()) {
            logger.info("Nenhum projeto encontrado. Nenhum relatório será gerado.");
            System.out.println("Nenhum projeto encontrado. Nenhum relatório será gerado.");
            // Gerar um HTML vazio ou com mensagem se desejado, mesmo sem projetos
            try {
               // htmlReportService.generateReport(Collections.emptyList(), Collections.emptyMap(), new HashMap<>(), similarityThreshold, htmlOutputFileName);
                excelReportService.generateReport(Collections.emptyList(), Collections.emptyMap(), new HashMap<>(), similarityThreshold, excelOutputFileName);
            } catch (IOException e) {
                logger.error("Falha ao tentar gerar relatório HTML vazio: {}", e.getMessage(), e);
            }
            return;
        }

        // Ordenar nomes de projetos para cabeçalhos da tabela
        List<String> sortedProjectNames = projects.stream()
                                                .map(Project::getName)
                                                .sorted()
                                                .collect(Collectors.toList());

        // Mapear nomes para objetos Project para fácil acesso
        Map<String, Project> projectMap = projects.stream()
                                                .collect(Collectors.toMap(Project::getName, Function.identity()));

        // Estrutura para armazenar todos os resultados de similaridade
        Map<String, Map<String, Double>> similarityScores = new HashMap<>();
        for (String name : sortedProjectNames) {
            similarityScores.put(name, new HashMap<>()); // Inicializa mapas internos
        }

        logger.info("{} projetos encontrados. Iniciando comparações...", projects.size());
        System.out.println(String.format("\n%d projetos encontrados. Calculando similaridades:", projects.size()));

        // Calcular similaridades e preencher o mapa `similarityScores`
        // Este loop calcula cada par (A,B) uma vez e preenche simetricamente
        for (int i = 0; i < sortedProjectNames.size(); i++) {
            String projNameA = sortedProjectNames.get(i);
            Project projectA = projectMap.get(projNameA);

            for (int j = i; j < sortedProjectNames.size(); j++) { // j começa em i
                String projNameB = sortedProjectNames.get(j);
                Project projectB = projectMap.get(projNameB);

                double similarity;
                if (projNameA.equals(projNameB)) { // Comparação de um projeto com ele mesmo
                    similarity = 100.0;
                } else {
                    ComparisonResult result = comparisonService.compareProjects(projectA, projectB);
                    similarity = result.getSimilarityPercentage();

                    // Imprimir no console (opcional, apenas para pares distintos)
                    System.out.println(String.format("\nComparando '%s' com '%s':", projNameA, projNameB));
                    if (Math.abs(similarity - 100.0) < 0.001) {
                        System.out.println("  Resultado: São cópias um do outro.");
                    } else {
                        System.out.println(String.format(Locale.US, "  Resultado: Percentual de similaridade: %.2f%%", similarity));
                    }
                }
                // Preencher o mapa simetricamente
                similarityScores.get(projNameA).put(projNameB, similarity);
                similarityScores.get(projNameB).put(projNameA, similarity);
            }
        }

        logger.info("Análise de similaridade concluída.");

        // Gerar Relatório HTML
        // logger.info("Gerando relatório HTML...");
        // try {
        //     htmlReportService.generateReport(sortedProjectNames, projectMap, similarityScores, similarityThreshold, htmlOutputFileName);
        // } catch (IOException e) {
        //     logger.error("Falha ao gerar relatório HTML: {}", e.getMessage(), e);
        //     System.err.println("Falha ao gerar relatório HTML: " + e.getMessage());
        // }

        // Gerar Relatório Excel
        logger.info("Gerando relatório Excel...");
        try {
            excelReportService.generateReport(sortedProjectNames, projectMap, similarityScores, similarityThreshold, excelOutputFileName);
        } catch (IOException e) {
            logger.error("Falha ao gerar relatório Excel: {}", e.getMessage(), e);
            System.err.println("Falha ao gerar relatório Excel: " + e.getMessage());
        }
        logger.info("Processo finalizado.");
    }
}
