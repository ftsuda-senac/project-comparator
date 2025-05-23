package com.example.projectcomparator.service;

import com.example.projectcomparator.model.Project; // Se precisar acessar o objeto Project diretamente
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HtmlReportService {

    private static final Logger logger = LoggerFactory.getLogger(HtmlReportService.class);
    private static final String DEFAULT_OUTPUT_FILENAME = "relatorio_similaridade_projetos.html";

    // Helper para escapar caracteres HTML, caso os nomes dos projetos os contenham
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    public void generateReport(List<String> sortedProjectNames,
                               Map<String, Project> projectMap,
                               Map<String, Map<String, Double>> similarityScores,
							   double similarityThreshold,
                               String outputFilePath) throws IOException {

        if (sortedProjectNames == null || sortedProjectNames.isEmpty()) {
            logger.warn("Nenhum nome de projeto fornecido para o relatório HTML.");
            System.out.println("Nenhum nome de projeto para gerar o relatório HTML.");
            return;
        }
        if (similarityScores == null) {
            similarityScores = new HashMap<>(); // Evita NullPointerException
        }


        StringBuilder htmlBuilder = new StringBuilder();

        // Cabeçalho HTML e Estilos
        htmlBuilder.append("<!DOCTYPE html>\n");
        htmlBuilder.append("<html lang=\"pt-BR\">\n");
        htmlBuilder.append("<head>\n");
        htmlBuilder.append("    <meta charset=\"UTF-8\">\n");
        htmlBuilder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        htmlBuilder.append("    <title>Relatório de Similaridade de Projetos</title>\n");
        htmlBuilder.append("    <style>\n");
        htmlBuilder.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f4f7f6; color: #333; }\n");
        htmlBuilder.append("        h1 { text-align: center; color: #2c3e50; margin-bottom: 30px; }\n");
        htmlBuilder.append("        .table-container { overflow-x: auto; }\n"); // Para rolagem em telas pequenas
        htmlBuilder.append("        table { border-collapse: collapse; width: 100%; max-width: 1200px; margin: 20px auto; box-shadow: 0 4px 15px rgba(0,0,0,0.1); background-color: #fff; }\n");
        htmlBuilder.append("        th, td { border: 1px solid #ddd; padding: 12px 15px; text-align: center; min-width: 100px; }\n");
        htmlBuilder.append("        th { background-color: #3498db; color: #ffffff; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        htmlBuilder.append("        th:first-child { background-color: #2c3e50; text-align: left; }\n"); // Canto superior esquerdo e cabeçalhos de linha
        htmlBuilder.append("        tr:nth-child(even) td { background-color: #ecf0f1; }\n"); // Linhas alternadas
        htmlBuilder.append("        tr:hover td { background-color: #d1e8f7; }\n");
        htmlBuilder.append("        td.highlight-similarity { background-color: #d4edda !important; color: #155724; font-weight: bold; }\n"); // Similaridade > 20%
        htmlBuilder.append("        td.diagonal { background-color: #bdc3c7 !important; color: #2c3e50; font-weight: bold; }\n"); // Diagonal
        htmlBuilder.append("        td.low-similarity { /* Estilo base já aplicado por tr:nth-child ou tr:hover */ }\n");
        htmlBuilder.append("    </style>\n");
        htmlBuilder.append("</head>\n");
        htmlBuilder.append("<body>\n");
        htmlBuilder.append("    <h1>Relatório de Similaridade de Projetos</h1>\n");
        htmlBuilder.append("    <div class=\"table-container\">\n");
        htmlBuilder.append("        <table>\n");

        // Linha de Cabeçalho da Tabela (Nomes dos projetos nas colunas)
        htmlBuilder.append("            <thead>\n");
        htmlBuilder.append("                <tr>\n");
        htmlBuilder.append("                    <th>Projetos</th>\n"); // Célula do canto superior esquerdo
        for (String projName : sortedProjectNames) {
            htmlBuilder.append("                    <th>").append(escapeHtml(projName)).append("</th>\n");
        }
        htmlBuilder.append("                </tr>\n");
        htmlBuilder.append("            </thead>\n");

        // Corpo da Tabela
        htmlBuilder.append("            <tbody>\n");
        for (String rowProjName : sortedProjectNames) {
            htmlBuilder.append("            <tr>\n");
            // Cabeçalho da Linha (Nome do projeto)
            htmlBuilder.append("                <th style=\"text-align: left; background-color: #7f8c8d;\">").append(escapeHtml(rowProjName)).append("</th>\n");
            for (String colProjName : sortedProjectNames) {
                String cellClass = "";
                String cellValue;

                // Obtem a similaridade do mapa pré-calculado
                double similarity = similarityScores
                                        .getOrDefault(rowProjName, new HashMap<>())
                                        .getOrDefault(colProjName, 0.0);

                if (rowProjName.equals(colProjName)) {
                    cellClass = "diagonal";
                    // O valor já será 100.0% vindo do mapa se pré-calculado corretamente.
                    // Se não, podemos forçar aqui, mas o ideal é que o mapa reflita isso.
                    cellValue = String.format(Locale.US, "%.2f%%", 100.0);
                } else {
                    cellValue = String.format(Locale.US, "%.2f%%", similarity);
                    if (similarity > similarityThreshold) {
                        cellClass = "highlight-similarity";
                    } else {
                        cellClass = "low-similarity"; // Classe para manter consistência, pode não ter estilo específico
                    }
                }
                htmlBuilder.append("                <td class=\"").append(cellClass).append("\">").append(cellValue).append("</td>\n");
            }
            htmlBuilder.append("            </tr>\n");
        }
        htmlBuilder.append("        </tbody>\n");
        htmlBuilder.append("        </table>\n");
        htmlBuilder.append("    </div>\n"); // Fim table-container
        htmlBuilder.append("</body>\n");
        htmlBuilder.append("</html>\n");

        Path outputPathObj = Paths.get(outputFilePath.isEmpty() ? DEFAULT_OUTPUT_FILENAME : outputFilePath);
        Files.writeString(outputPathObj, htmlBuilder.toString(), StandardCharsets.UTF_8);
        logger.info("Relatório HTML gerado com sucesso em: {}", outputPathObj.toAbsolutePath());
        System.out.println("Relatório HTML gerado com sucesso em: " + outputPathObj.toAbsolutePath());
    }
}
