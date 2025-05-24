package com.example.projectcomparator.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.projectcomparator.model.Project;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelReportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelReportService.class);
    private static final String DEFAULT_SHEET_NAME = "Similaridade de Projetos";

    private void addComment(Workbook workbook, Sheet sheet, Cell cell, String commentText) {
        CreationHelper factory = workbook.getCreationHelper();

        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 2);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 2);
        // i found it useful to show the comment box at the bottom right corner
        // anchor.setCol1(cell.getColumnIndex() + 1); // the box f the comment starts at this given column...
        // anchor.setCol2(cell.getColumnIndex() + 3); // ...and ends at that given column
        // anchor.setRow1(rowIdx + 1); // one row below the cell...
        // anchor.setRow2(rowIdx + 5); // ...and 4 rows high

        Drawing<?> drawing = sheet.createDrawingPatriarch();
        Comment comment = drawing.createCellComment(anchor);
        // set the comment text and author
        comment.setString(factory.createRichTextString(commentText));
        comment.setAuthor("ftsuda");
        cell.setCellComment(comment);
    }

    private void setCellBorders(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }

    public void generateReport(List<String> sortedProjectNames,
							   Map<String, Project> projectMap,
                               Map<String, Map<String, Double>> similarityScores,
							   double similarityThreshold,
                               String outputFilePath) throws IOException {

        if (sortedProjectNames == null || sortedProjectNames.isEmpty()) {
            logger.warn("Nenhum nome de projeto fornecido para o relatório Excel.");
            System.out.println("Nenhum nome de projeto para gerar o relatório Excel.");
            // Opcionalmente, criar um arquivo Excel vazio com uma mensagem
            // return;
        }
        if (similarityScores == null) {
            similarityScores = new HashMap<>();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(DEFAULT_SHEET_NAME);
            sheet.createFreezePane(1, 1); // (qtde de cima para baixo, qtde esquerda para direita)

            // --- Criação de Estilos ---
            DataFormat dataFormat = workbook.createDataFormat();
            short percentageDataFormat = dataFormat.getFormat("0.00%");

            // Estilo para cabeçalhos da tabela (nomes dos projetos)
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(new XSSFColor(new java.awt.Color(255, 255, 255), null)); // Texto branco
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(44, 62, 80), null)); // #2c3e50 (Azul escuro)
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setCellBorders(headerStyle);

            // Estilo para cabeçalhos de linha (nomes dos projetos na primeira coluna)
            XSSFCellStyle rowHeaderStyle = workbook.createCellStyle();
            rowHeaderStyle.cloneStyleFrom(headerStyle); // Baseia-se no headerStyle
            rowHeaderStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(127, 140, 141), null)); // #7f8c8d (Cinza)
            rowHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            rowHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

			// Estilo para células com informações do projeto
			// Estilo padrão para outras células de dados
            XSSFCellStyle infoDataStyle = workbook.createCellStyle();
            infoDataStyle.setAlignment(HorizontalAlignment.LEFT);
			infoDataStyle.setWrapText(true);
            setCellBorders(infoDataStyle);

            // Estilo para células de dados com similaridade > 20%
            XSSFCellStyle highlightStyle = workbook.createCellStyle();
            highlightStyle.setDataFormat(percentageDataFormat);
            highlightStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(254, 203, 203), null)); //rgb(254, 203, 203) (Verde claro)
            highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont highlightFont = workbook.createFont();
            highlightFont.setColor(new XSSFColor(new java.awt.Color(255, 0, 0), null)); //rgb(255, 0, 0) (Verde escuro)
            highlightStyle.setFont(highlightFont);
            highlightStyle.setAlignment(HorizontalAlignment.CENTER);
            setCellBorders(highlightStyle);

            // Estilo para células da diagonal (100%)
            XSSFCellStyle diagonalStyle = workbook.createCellStyle();
            diagonalStyle.setDataFormat(percentageDataFormat);
            diagonalStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(189, 195, 199), null)); // #bdc3c7 (Cinza claro/médio)
            diagonalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont diagonalFont = workbook.createFont();
            diagonalFont.setBold(true);
            diagonalFont.setColor(new XSSFColor(new java.awt.Color(44, 62, 80), null)); // #2c3e50
            diagonalStyle.setFont(diagonalFont);
            diagonalStyle.setAlignment(HorizontalAlignment.CENTER);
            setCellBorders(diagonalStyle);

            // Estilo padrão para outras células de dados
            XSSFCellStyle defaultDataStyle = workbook.createCellStyle();
            defaultDataStyle.setDataFormat(percentageDataFormat);
            defaultDataStyle.setAlignment(HorizontalAlignment.CENTER);
            setCellBorders(defaultDataStyle);


            // --- Preenchimento da Planilha ---

            // Linha de cabeçalho (nomes dos projetos nas colunas)
            Row headerRow = sheet.createRow(0);
            Cell topLeftCell = headerRow.createCell(0); // Célula do canto superior esquerdo
            topLeftCell.setCellValue("Projetos");
            topLeftCell.setCellStyle(headerStyle); // Usar o mesmo estilo dos outros cabeçalhos

			// // Linha com dados do projeto
			// Cell topLeftCell2 = headerRow.createCell(1); // Célula do canto superior esquerdo
			// topLeftCell2.setCellValue("Informações");
            // topLeftCell2.setCellStyle(headerStyle); // Usar o mesmo estilo dos outros cabeçalhos

            for (int i = 0; i < sortedProjectNames.size(); i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(sortedProjectNames.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Linhas de dados
            for (int i = 0; i < sortedProjectNames.size(); i++) {
                String rowProjName = sortedProjectNames.get(i);
                Row dataRow = sheet.createRow(i + 1);

                // Cabeçalho da linha (nome do projeto na primeira coluna)
                Cell rowHeaderCell = dataRow.createCell(0);
                rowHeaderCell.setCellValue(rowProjName);
                rowHeaderCell.setCellStyle(rowHeaderStyle);

				String fileInfo = projectMap.get(rowProjName).printFileInfo();
				if (fileInfo.length() > 32767) {
					fileInfo = fileInfo.substring(0,32767);
				}
				addComment(workbook, sheet, rowHeaderCell, fileInfo);

				// Cell rowHeaderCell2 = dataRow.createCell(1);
                // rowHeaderCell2.setCellValue(fileInfo);
                // rowHeaderCell2.setCellStyle(infoDataStyle);

                for (int j = 0; j < sortedProjectNames.size(); j++) {
                    String colProjName = sortedProjectNames.get(j);
                    Cell dataCell = dataRow.createCell(j + 1);

                    double similarity = similarityScores
                                            .getOrDefault(rowProjName, new HashMap<>())
                                            .getOrDefault(colProjName, 0.0);

                    dataCell.setCellValue(similarity / 100.0); // POI espera valor decimal para % (ex: 0.75 para 75%)

                    if (rowProjName.equals(colProjName)) {
                        dataCell.setCellStyle(diagonalStyle);
                    } else if (similarity > similarityThreshold) {
                        dataCell.setCellStyle(highlightStyle);
                    } else {
                        dataCell.setCellStyle(defaultDataStyle);
                    }
                }
            }

            // Ajustar largura das colunas
            sheet.setColumnWidth(0, 25 * 256); // Largura para a primeira coluna (nomes dos projetos)
			// sheet.autoSizeColumn(1);
            for (int i = 0; i < sortedProjectNames.size(); i++) {
                sheet.setColumnWidth(i + 1, 15 * 256); // Largura para colunas de dados
                // Alternativamente, usar autoSizeColumn, mas pode ser lento para muitas colunas/linhas
                // sheet.autoSizeColumn(i + 1);
            }
             if (sortedProjectNames.isEmpty()) { // Se não há projetos, ajuste a primeira coluna pelo menos
                sheet.setColumnWidth(0, 25 * 256);
            }


            // Escrever o arquivo
            Path outputPathObj = Paths.get(outputFilePath);
            try (FileOutputStream fileOut = new FileOutputStream(outputPathObj.toFile())) {
                workbook.write(fileOut);
            }
            logger.info("Relatório Excel gerado com sucesso em: {}", outputPathObj.toAbsolutePath());
            System.out.println("Relatório Excel gerado com sucesso em: " + outputPathObj.toAbsolutePath());

        } catch (IOException e) {
            logger.error("Erro ao gerar relatório Excel: {}", e.getMessage(), e);
            throw e; // Propaga a exceção para ser tratada pelo chamador
        }
    }
}
