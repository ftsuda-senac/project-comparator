package com.example.projectcomparator;

import com.example.projectcomparator.model.ComparisonResult;
import com.example.projectcomparator.model.Project;
import com.example.projectcomparator.service.HtmlReportService;
import com.example.projectcomparator.service.ProjectComparerService;
import com.example.projectcomparator.service.ProjectFinderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SpringBootApplication
public class ProjectComparisonApplication /* implements CommandLineRunner */ {

    private static final Logger logger = LoggerFactory.getLogger(ProjectComparisonApplication .class);

    private static final String BASE_OUTPUT_FILE_NAME = "comparison_results";
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ProjectFinderService projectFinderService;
    private final ProjectComparerService projectComparerService;

    public ProjectComparisonApplication (ProjectFinderService projectFinderService,
            ProjectComparerService projectComparerService) {
        this.projectFinderService = projectFinderService;
        this.projectComparerService = projectComparerService;
    }

    public static void main(String[] args) {
        Locale.setDefault(new Locale("pt", "BR"));
        SpringApplication.run(ProjectComparisonApplication.class, args);
    }

    /*
    @Override
    public void run(String... args) throws Exception {

        // if (args.length == 0) {
        //     logger.error("Por favor, forneça o caminho para o diretório pai como argumento.");
        //     System.err.println("Uso: java -jar project-comparator.jar <caminho_para_diretorio_pai>");
        //     return;
        // }

        String diretorio = "E:\\projetos\\senac\\24-2\\dswb\\projetos\\teste";

        Path parentDirectory = Paths.get(diretorio);
        logger.info("Iniciando verificação no diretório: {}", parentDirectory);

        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String outputFileNameWithTimestamp = BASE_OUTPUT_FILE_NAME + "_" + timestamp + ".txt";

        Path outputFilePath = Paths.get(outputFileNameWithTimestamp);

        List<Project> projects = projectFinderService.findProjects(parentDirectory);
        logger.info("Resultados da comparação serão gravados em: {}", outputFilePath.toAbsolutePath());

        if (projects.size() < 2) {
            logger.warn("Menos de dois projetos encontrados. Nenhuma comparação a ser feita.");
            System.out.println("Menos de dois projetos encontrados para comparação."); // Feedback no console
            // Grava esta informação no arquivo também
            try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write("--- Resultados da Comparação ---\n\n");
                writer.write(String.format("Diretório verificado: %s\n", parentDirectory.toAbsolutePath()));
                writer.write("Menos de dois projetos encontrados para comparação.\n");
                writer.write("\n--- Fim da Comparação ---\n");
                System.out.println("Informação gravada em: " + outputFilePath.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Erro ao gravar informação no arquivo '{}': {}", outputFilePath.toAbsolutePath(), e.getMessage());
            }
            return;
        }

        logger.info("{} projetos encontrados para comparação.", projects.size());
        System.out.println(String.format("%d projetos encontrados. Iniciando comparações...", projects.size())); // Feedback no console

        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write("--- Resultados da Comparação de Projetos ---\n");
            writer.write(String.format("Diretório Pai Verificado: %s\n", parentDirectory.toAbsolutePath()));
            writer.write(String.format("Data da Comparação: %s\n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            writer.write(String.format("Total de Projetos Identificados para Comparação: %d\n\n", projects.size()));

            for (int i = 0; i < projects.size(); i++) {
                for (int j = i + 1; j < projects.size(); j++) {
                    Project p1 = projects.get(i);
                    Project p2 = projects.get(j);

                    logger.info("Comparando '{}' (em {}) com '{}' (em {})", p1.getName(), p1.getRootPath(), p2.getName(), p2.getRootPath());
                    ComparisonResult result = projectComparerService.compareProjects(p1, p2);

                    String resultString = String.format("Comparação entre Projeto '%s' (Caminho: %s) e Projeto '%s' (Caminho: %s):\n%s\n\n",
                                                        p1.getName(), p1.getRootPath(),
                                                        p2.getName(), p2.getRootPath(),
                                                        result.toString().replaceFirst("Projetos ", "")); // Remove redundância
                    writer.write(resultString);
                    logger.debug("Resultado da comparação entre '{}' e '{}' gravado.", p1.getName(), p2.getName());
                }
                writer.write("""
                        ================================================================================
                        ================================================================================
                        ================================================================================
                        """);
            }
            writer.write("--- Fim da Comparação ---");
            System.out.println("Resultados da comparação gravados com sucesso em: " + outputFilePath.toAbsolutePath()); // Feedback final no console
        } catch (IOException e) {
            logger.error("Erro ao gravar resultados da comparação no arquivo '{}': {}", outputFilePath.toAbsolutePath(), e.getMessage());
            System.err.println("Falha ao gravar resultados no arquivo. Verifique os logs para detalhes.");
        }
        System.out.println("\n--- Fim da Comparação ---");
    }
    */
}
