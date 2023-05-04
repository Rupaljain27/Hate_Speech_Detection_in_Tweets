package edu.arizona.cs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Output {

    static String inputfilePathCSV = "src/main/resources/input.csv";

    public static void OUTPUT() {

        System.out.println("Output.java");
        String ansFilePath_Combined = "src/main/resources/output_Combined.txt";
        String ansFilePath_New = "src/main/resources/output_New.txt";
        String ansFilePath_Original = "src/main/resources/output_Original.txt";

        // Neural network
        String ansFilePath_Combined_NN = "src/main/resources/output_Combined_NN.txt";
        String ansFilePath_New_NN = "src/main/resources/output_New_NN.txt";
        String ansFilePath_Original_NN = "src/main/resources/output_Original_NN.txt";

        // Lexicon
        String ansFilePath_Combined_Lex = "src/main/resources/output_Combined_Lex.txt";
        String ansFilePath_New_Lex = "src/main/resources/output_New_Lex.txt";
        String ansFilePath_Original_Lex = "src/main/resources/output_Original_Lex.txt";

        Map<String, Integer> tweets = readTweets(inputfilePathCSV);
        Map<String, Float> combinedRetrieved = Evaluation.readRetrievedTweets(ansFilePath_Combined);
        Map<String, Float> newRetrieved = Evaluation.readRetrievedTweets(ansFilePath_New);
        Map<String, Float> originalRetrieved = Evaluation.readRetrievedTweets(ansFilePath_Original);

        // Neural Network
        Map<String, Float> combinedRetrieved_NN = Evaluation.readRetrievedTweets(ansFilePath_Combined_NN);
        Map<String, Float> newRetrieved_NN = Evaluation.readRetrievedTweets(ansFilePath_New_NN);
        Map<String, Float> originalRetrieved_NN = Evaluation.readRetrievedTweets(ansFilePath_Original_NN);

        // Lexicon
        Map<String, Float> combinedRetrieved_Lex = Evaluation.readRetrievedTweets(ansFilePath_Combined_Lex);
        Map<String, Float> newRetrieved_Lex = Evaluation.readRetrievedTweets(ansFilePath_New_Lex);
        Map<String, Float> originalRetrieved_Lex = Evaluation.readRetrievedTweets(ansFilePath_Original_Lex);

        createExcel(tweets, combinedRetrieved_NN, newRetrieved_NN,
                originalRetrieved_NN,
                "src/main/resources/output_without_lexicon.xlsx");

        createExcelWithLexicon(tweets, combinedRetrieved_NN, newRetrieved_NN,
                originalRetrieved_NN, combinedRetrieved_Lex,
                newRetrieved_Lex, originalRetrieved_Lex,
                "src/main/resources/output_with_lexicon.xlsx");
    }

    private static Map<String, Integer> readTweets(String filePath) {
        Map<String, Integer> tweets = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    String tweetId = values[0];
                    String hateValueStr = values[1];
                    try {
                        int hateValue = Integer.parseInt(hateValueStr);
                        tweets.put(tweetId, hateValue);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tweets;
    }

    public static void createExcel(Map<String, Integer> tweets, Map<String, Float> combinedRetrieved,
            Map<String, Float> newRetrieved, Map<String, Float> originalRetrieved, String fileName) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Output");

            // Create the header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("tweet");
            headerRow.createCell(1).setCellValue("hate");
            headerRow.createCell(2).setCellValue("Original_retrieved(0/1)");
            headerRow.createCell(3).setCellValue("score");
            headerRow.createCell(3).setCellValue("category");
            headerRow.createCell(4).setCellValue("New_retrieved(0/1)");
            headerRow.createCell(5).setCellValue("score");
            headerRow.createCell(3).setCellValue("category");
            headerRow.createCell(6).setCellValue("Combined_retrieved(0/1)");
            headerRow.createCell(7).setCellValue("score");
            headerRow.createCell(3).setCellValue("category");

            // Iterate over the tweets and write each row
            int rowIndex = 1;
            for (Map.Entry<String, Integer> entry : tweets.entrySet()) {
                String tweet = entry.getKey();
                int hateValue = entry.getValue();

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(tweet);
                row.createCell(1).setCellValue(hateValue);

                // Check if the tweet is in originalRetrieved and write the corresponding values
                writeRetrievedValueAndScore(originalRetrieved, tweet, row, 2, 3);

                // Check if the tweet is in newRetrieved and write the corresponding values
                writeRetrievedValueAndScore(newRetrieved, tweet, row, 4, 5);

                // Check if the tweet is in combinedRetrieved and write the corresponding values
                writeRetrievedValueAndScore(combinedRetrieved, tweet, row, 6, 7);
            }

            // Auto-size columns for better readability
            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the workbook to the output file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeRetrievedValueAndScore(Map<String, Float> retrievedTweets, String tweet, Row row,
            int valueCellIndex, int scoreCellIndex) {
        Cell scoreCell = row.createCell(scoreCellIndex);
        Cell valueCell = row.createCell(valueCellIndex);

        if (retrievedTweets.containsKey(tweet)) {
            valueCell.setCellValue(1);
            scoreCell.setCellValue(retrievedTweets.get(tweet));
        } else {
            valueCell.setCellValue(0);
            scoreCell.setCellValue(0);
        }
    }

    public static void createExcelWithLexicon(Map<String, Integer> tweets,
            Map<String, Float> combinedRetrieved_NN,
            Map<String, Float> newRetrieved_NN, Map<String, Float> originalRetrieved_NN,
            Map<String, Float> combinedRetrieved_Lex, Map<String, Float> newRetrieved_Lex,
            Map<String, Float> originalRetrieved_Lex, String fileName) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Output");

            // Create the header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("tweet");
            headerRow.createCell(1).setCellValue("hate");
            headerRow.createCell(2).setCellValue("Original_retrieved(0/1)");
            headerRow.createCell(3).setCellValue("score");
            headerRow.createCell(4).setCellValue("category");
            headerRow.createCell(5).setCellValue("New_retrieved(0/1)");
            headerRow.createCell(6).setCellValue("score");
            headerRow.createCell(7).setCellValue("category");
            headerRow.createCell(8).setCellValue("Combined_retrieved(0/1)");
            headerRow.createCell(9).setCellValue("score");
            headerRow.createCell(10).setCellValue("category");

            // Iterate over the tweets and write each row
            int rowIndex = 1;
            for (Map.Entry<String, Integer> entry : tweets.entrySet()) {
                String tweet = entry.getKey();
                int hateValue = entry.getValue();

                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(tweet);
                row.createCell(1).setCellValue(hateValue);

                // Check if the tweet is in originalRetrieved and write the corresponding values
                writeRetrievedValueAndScoreWithLexicon(originalRetrieved_NN,
                        originalRetrieved_Lex, tweet, row, 2, 3,
                        4);

                // Check if the tweet is in newRetrieved and write the corresponding values
                writeRetrievedValueAndScoreWithLexicon(newRetrieved_NN, newRetrieved_Lex,
                        tweet, row, 5, 6, 7);

                // Check if the tweet is in combinedRetrieved and write the corresponding values
                writeRetrievedValueAndScoreWithLexicon(combinedRetrieved_NN,
                        combinedRetrieved_Lex, tweet, row, 8, 9,
                        10);
            }

            // Auto-size columns for better readability
            for (int i = 0; i <= 10; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write the workbook to the output file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeRetrievedValueAndScoreWithLexicon(Map<String, Float> retrievedTweets_NN,
            Map<String, Float> retrievedTweets_Lex, String tweet, Row row, int valueCellIndex, int scoreCellIndex,
            int categoryCellIndex) {
        Cell scoreCell = row.createCell(scoreCellIndex);
        Cell valueCell = row.createCell(valueCellIndex);
        Cell categoryCell = row.createCell(categoryCellIndex);

        if (retrievedTweets_NN.containsKey(tweet) &&
                retrievedTweets_Lex.containsKey(tweet)) {
            valueCell.setCellValue(1);
            scoreCell.setCellValue(retrievedTweets_Lex.get(tweet));
            categoryCell.setCellValue("NN+Lex");
        } else if (retrievedTweets_NN.containsKey(tweet)) {
            valueCell.setCellValue(1);
            scoreCell.setCellValue(retrievedTweets_NN.get(tweet));
            categoryCell.setCellValue("NN");
        } else if (retrievedTweets_Lex.containsKey(tweet)) {
            valueCell.setCellValue(1);
            scoreCell.setCellValue(retrievedTweets_Lex.get(tweet));
            categoryCell.setCellValue("Lexicon");
        } else {
            valueCell.setCellValue(0);
            scoreCell.setCellValue(0);
            categoryCell.setCellValue("");
        }
    }
}
