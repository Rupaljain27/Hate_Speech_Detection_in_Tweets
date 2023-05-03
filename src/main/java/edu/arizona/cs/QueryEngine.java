package edu.arizona.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.nodes.ValueQueryNode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.api.TimelinesResources;

public class QueryEngine {

    private static String CONSUMER_KEY = "";
    private static String CONSUMER_SECRET = "";
    private static String ACCESS_TOKEN = "";
    private static String ACCESS_TOKEN_SECRET = "";

    static boolean indexExists = false;
    static String inputfilePathCSV = "src/main/resources/input.csv";
    static String NewQueryFile = "new_dataset_hatespeech_query.txt";
    static String CombinedQueryFile = "combined_dataset_hatespeech_query.txt";
    static String OriginalQueryFile = "old_dataset_hatespeech_query.txt";
    static String indexPath = "src/main/index";
    static String lexiconFilePath = "hatespeech_lexicon.txt";
    static String lexiconQueryFile = "expanded_hatespeech_lexicon.txt";

    static String groundTruthFilePath = "src/main/resources/input.csv";

    static String ansFilePath_Combined_NN = "src/main/resources/output_Combined_NN.txt";
    static String ansFilePath_New_NN = "src/main/resources/output_New_NN.txt";
    static String ansFilePath_Original_NN = "src/main/resources/output_Original_NN.txt";
    static String ansFilePath_Combined_Lex = "src/main/resources/output_Combined_Lex.txt";
    static String ansFilePath_New_Lex = "src/main/resources/output_New_Lex.txt";
    static String ansFilePath_Original_Lex = "src/main/resources/output_Original_Lex.txt";
    static String ansFilePath_Combined = "src/main/resources/output_Combined.txt";
    static String ansFilePath_New = "src/main/resources/output_New.txt";
    static String ansFilePath_Original = "src/main/resources/output_Original.txt";
    static Directory index;
    boolean similarity = false;

    public static void main(String[] args) throws Exception {

        String NewNNquery = "";
        String CombinedNNquery = "";
        String OriginalNNquery = "";
        // Creating a query string, this will contain all the tweets from Neural Network
        // which will work as a query
        InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + NewQueryFile);
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                NewNNquery += line + " ";
            }
            inputScanner.close();
        }
        NewNNquery = NewNNquery.trim();

        inputStream = QueryEngine.class.getResourceAsStream("/" + CombinedQueryFile);
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                CombinedNNquery += line + " ";
            }
            inputScanner.close();
        }
        CombinedNNquery = CombinedNNquery.trim();

        inputStream = QueryEngine.class.getResourceAsStream("/" + OriginalQueryFile);
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                OriginalNNquery += line + " ";
            }
            inputScanner.close();
        }
        OriginalNNquery = OriginalNNquery.trim();

        // Step 1: Retrieve random tweets from Twitter API (not implemented)
        // getRandomTweetsFromAPI(50);

        // Step 2: Build index
        // buildIndex();

        // Step 3: Detect hate speech
        System.out.println("New NN query");
        HateSpeechDetector(NewNNquery, "New");
        System.out.println("Combined NN query");
        HateSpeechDetector(CombinedNNquery, "Combined");
        System.out.println("Original NN query");
        HateSpeechDetector(OriginalNNquery, "Original");

        // Evaluation();
        // OUTPUT();
    }

    public static Twitter getTwitterInstance() {
        Properties props = new Properties();
        try (InputStream inputStream = new FileInputStream("config.properties")) {
            props.load(inputStream);
            // System.out.println(props);
            CONSUMER_KEY = props.getProperty("consumerKey");
            CONSUMER_SECRET = props.getProperty("consumerSecret");
            ACCESS_TOKEN = props.getProperty("accessToken");
            ACCESS_TOKEN_SECRET = props.getProperty("accessTokenSecret");

            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(CONSUMER_KEY)
                    .setOAuthConsumerSecret(CONSUMER_SECRET)
                    .setOAuthAccessToken(ACCESS_TOKEN)
                    .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);

            // Creating a Twitter factory with the configuration
            TwitterFactory tf = new TwitterFactory(cb.build());

            // Creating a Twitter instance
            Twitter twitter = tf.getInstance();
            return twitter;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String normalizeText(String inputText) throws IOException {

        // Define the mapping for the apostrophe
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        builder.add("'", "");
        NormalizeCharMap charMap = builder.build();

        // Create a CharFilter to remove the apostrophe
        CharFilter charFilter = new MappingCharFilter(charMap, new StringReader(inputText));

        // create a new instance of the StandardAnalyzer
        Analyzer analyzer = new StandardAnalyzer();

        // tokenize the input text
        TokenStream tokenStream = analyzer.tokenStream("field", new StringReader(inputText));

        // create a new StringBuilder to store the normalized text
        StringBuilder outputText = new StringBuilder();

        // iterate through the tokens and add them to the outputText
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            outputText.append(charTermAttribute.toString());
            outputText.append(" ");
        }

        // close the tokenStream and analyzer
        tokenStream.end();
        tokenStream.close();
        analyzer.close();

        // return the normalized text
        return outputText.toString().trim();
    }

    // Functions to retrieve random tweets from Twitter API
    private static void getRandomTweetsFromAPI(int numTweets) throws TwitterException, IOException {

        Twitter twitter = getTwitterInstance();

        try {
            // Retrieve the 20 most recent tweets from the home timeline
            List<Status> statuses = ((TimelinesResources) twitter).getHomeTimeline();

            // // Print the 50 most recent tweets using Lexicon query
            // String lexiconQueryString = loadHateSpeechLexicon(lexiconFilePath);
            // lexiconQueryString = lexiconQueryString.replaceFirst(" OR ", "");
            // System.out.println("String: " + lexiconQueryString);
            // // Create a query object and set its parameters
            // Query query = new Query(lexiconQueryString);
            // query.setCount(numTweets);
            // query.setResultType(Query.RECENT); // Set result type to recent tweets
            // query.setLang("en"); // Set language to English
            // // Execute the query and retrieve the search results
            // QueryResult result = twitter.search(query);
            // List<Status> statuses = result.getTweets();

            System.out.println("Number of tweets retrieved: " + statuses.size());

            File file = new File(inputfilePathCSV);
            boolean isNewFile = file.length() == 0;
            try (FileWriter filewriter = new FileWriter(inputfilePathCSV, true)) {
                // Write CSV header if the file is empty
                if (isNewFile) {
                    filewriter.write("tweet,hate_level\n");
                }

                for (Status status : statuses) {
                    // Remove punctuation from tweet text
                    String text = status.getText().replaceAll("\\p{Punct}", "");
                    text = text.replaceAll("\n", "");

                    // Write new tweet to the CSV file
                    String normalizedText = normalizeText(text);

                    // Determine hate level based on your criteria
                    int hateLevel = 0;

                    // Write tweet and hate level to CSV
                    filewriter.write(normalizedText + "," + hateLevel + "\n");
                }
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        } finally {
        }
    }

    // Function to build index
    private static void buildIndex() throws IOException {

        // Creating an index writer
        StandardAnalyzer analyzer = new StandardAnalyzer();

        // Opening the index directory
        index = FSDirectory.open(Paths.get(indexPath));

        // Creating the index writer configuration
        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        // Creating the index writer, reader for the CSV file and CSV parser
        try (IndexWriter writer = new IndexWriter(index, config);
                Reader reader = new FileReader(inputfilePathCSV);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

            // Iterate over each CSV record
            for (CSVRecord csvRecord : csvParser) {
                // Extracting the record number and converting it to a string
                String docName = Long.toString(csvRecord.getRecordNumber());

                // Extracting the text from the CSV record
                String docText = csvRecord.get(0);

                // Creating a Lucene document
                Document doc = new Document();

                // Adding the text and Tweet field to the document
                doc.add(new TextField("text", docText, Field.Store.YES));
                doc.add(new StringField("Tweetid", docName, Field.Store.YES));

                // Adding the document to the index
                writer.addDocument(doc);
            }

            // Setting the indexExists flag to true
            indexExists = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // load the query string by using the hate speech lexicon
    private static String loadHateSpeechLexicon(String lexiconFile) {
        // Reading the input file
        InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + lexiconFile);
        StringBuilder sb = new StringBuilder();
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                sb.append(" ");
                sb.append(line);
            }
        }
        String lexiconQueryString = sb.toString();
        return lexiconQueryString;
    }

    // Function to detect hate speech
    private static void HateSpeechDetector(String queryString, String name) throws Exception {
        // Create index
        index = FSDirectory.open(Paths.get(indexPath));

        String filename = "src/main/resources/output_" + name + ".txt";
        String filename_NN = "src/main/resources/output_" + name + "_NN.txt";
        String filename_Lex = "src/main/resources/output_" + name + "_Lex.txt";
        // Creating an index reader
        try (IndexReader reader = DirectoryReader.open(index);
                BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false));
                BufferedWriter writer_NN = new BufferedWriter(new FileWriter(filename_NN, false));
                BufferedWriter writer_Lex = new BufferedWriter(new FileWriter(filename_Lex, false))) {

            // Creating a list to store the results
            List<ResultClass> ans = new ArrayList<ResultClass>();

            // Creating a searcher to search the index
            IndexSearcher searcher = new IndexSearcher(reader);

            // Setting the similarity function to Cosine Similarity
            searcher.setSimilarity(new ClassicSimilarity());

            queryString = QueryParser.escape(queryString);

            // Set up the query parser for the query
            org.apache.lucene.search.Query query = new QueryParser("text", new StandardAnalyzer())
                    .parse(queryString);

            // Searching the index
            TopDocs hits = searcher.search(query, 20);
            ScoreDoc[] score = hits.scoreDocs;
            System.out.println("Total number of tweets retrieved: " + score + " : " + hits);

            // Storing the results in the list
            for (int i = 0; i < score.length; ++i) {
                int docId = score[i].doc;
                Document doc = searcher.doc(docId);
                ResultClass result = new ResultClass();
                result.DocName = doc;
                result.docScore = score[i].score;
                // Check if the entry already exists in ans
                boolean entryExists = false;
                for (ResultClass existingResult : ans) {
                    if (existingResult.DocName.equals(result.DocName)) {
                        entryExists = true;
                        break;
                    }
                }

                if (!entryExists) {
                    ans.add(result);
                }
                // ans.add(result);
            }
            System.out.println("Hate Speech Tweets: ***************");
            System.out.println("Hate Speech tweets retrieved from NN: " + ans.size());
            // Writing the results to a file for NN
            for (ResultClass result : ans) {
                String output = result.DocName.get("Tweetid") + " : " + result.docScore + " : "
                        + result.DocName.get("text");
                // System.out.println(output);
                writer_NN.write(output);
                writer_NN.newLine();
            }

            // Improvement of traditional method - load lexicon from hate speech text file
            String lexiconQueryString = loadHateSpeechLexicon(lexiconQueryFile);

            // Creating a list to store the results
            // List<ResultClass> ans1 = new ArrayList<ResultClass>();

            // Creating a searcher to search the index
            searcher = new IndexSearcher(reader);

            // Setting the similarity function to Cosine Similarity
            searcher.setSimilarity(new ClassicSimilarity());

            // Set up the query parser for the query
            query = new QueryParser("text", new StandardAnalyzer())
                    .parse(lexiconQueryString);

            // Searching the index
            hits = searcher.search(query, 20);
            score = hits.scoreDocs;

            List<ResultClass> temp = new ArrayList<ResultClass>();
            for (int i = 0; i < score.length; ++i) {
                int docId = score[i].doc;
                Document doc = searcher.doc(docId);
                ResultClass result = new ResultClass();
                result.DocName = doc;
                result.docScore = score[i].score;
                temp.add(result);
            }
            System.out.println("Hate Speech tweets retrieved from Lex: " + temp.size());
            for (ResultClass result : temp) {
                String output = result.DocName.get("Tweetid") + " : " + result.docScore + " : "
                        + result.DocName.get("text");
                // System.out.println(output);
                writer_Lex.write(output);
                writer_Lex.newLine();
            }

            // Storing the results in the list
            for (int i = 0; i < score.length; ++i) {
                int docId = score[i].doc;
                Document doc = searcher.doc(docId);
                ResultClass result = new ResultClass();
                result.DocName = doc;
                result.docScore = score[i].score;
                // Check if the entry already exists in ans
                boolean entryExists = false;
                for (ResultClass existingResult : ans) {
                    if (existingResult.DocName.equals(result.DocName)) {
                        entryExists = true;
                        break;
                    }
                }
                if (!entryExists) {
                    ans.add(result);
                }
            }
            System.out.println("Hate Speech tweets retrieved: " + ans.size());

            // Printing the tweets having Hate Speech and writing to output.txt
            for (ResultClass result : ans) {
                String output = result.DocName.get("Tweetid") + " : " + result.docScore + " : "
                        + result.DocName.get("text");
                writer.write(output);
                writer.newLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void OUTPUT() {
        String ansFilePath_Combined = "src/main/resources/output_Combined.txt";
        String ansFilePath_New = "src/main/resources/output_New.txt";
        String ansFilePath_Original = "src/main/resources/output_Original.txt";

        Map<String, Integer> tweets = readTweets(inputfilePathCSV);
        Map<String, Float> combinedRetrieved = readRetrievedTweets(ansFilePath_Combined);
        Map<String, Float> newRetrieved = readRetrievedTweets(ansFilePath_New);
        Map<String, Float> originalRetrieved = readRetrievedTweets(ansFilePath_Original);

        createExcel(tweets, combinedRetrieved, newRetrieved, originalRetrieved,
        "src/main/resources/output_without_lexicon.xlsx");

        createExcelWithLexicon(tweets, combinedRetrieved, newRetrieved, originalRetrieved, combinedRetrieved,
                newRetrieved, originalRetrieved,
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

    public static void createExcelWithLexicon(Map<String, Integer> tweets, Map<String, Float> combinedRetrieved_NN,
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
                writeRetrievedValueAndScoreWithLexicon(originalRetrieved_NN, originalRetrieved_Lex, tweet, row, 2, 3,
                        4);

                // Check if the tweet is in newRetrieved and write the corresponding values
                writeRetrievedValueAndScoreWithLexicon(newRetrieved_NN, newRetrieved_Lex, tweet, row, 5, 6, 7);

                // Check if the tweet is in combinedRetrieved and write the corresponding values
                writeRetrievedValueAndScoreWithLexicon(combinedRetrieved_NN, combinedRetrieved_Lex, tweet, row, 8, 9,
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

        if (retrievedTweets_NN.containsKey(tweet) && retrievedTweets_Lex.containsKey(tweet)) {
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

    public static void Evaluation() {
        Set<String> groundTruthHate0 = readGroundTruthHate0(inputfilePathCSV, 0);
        Set<String> groundTruthHate1 = readGroundTruthHate0(inputfilePathCSV, 1);

        Map<String, Float> retrievedTweets_New = readRetrievedTweets(ansFilePath_New);
        Map<String, Float> retrievedTweets_Combined = readRetrievedTweets(ansFilePath_Combined);
        Map<String, Float> retrievedTweets_Original = readRetrievedTweets(ansFilePath_Original);

        System.out.println("groundTruthHate0: " + groundTruthHate0.size());
        System.out.println("groundTruthHate1: " + groundTruthHate1.size());
        System.out.println("retrievedTweets New: " + retrievedTweets_New.size());
        System.out.println("retrievedTweets Combined: " + retrievedTweets_Combined.size());
        System.out.println("retrievedTweets Original: " + retrievedTweets_Original.size());

        System.out.println("New NN: ");
        mearusingPRA(groundTruthHate0, groundTruthHate1, retrievedTweets_New);

        System.out.println("Combined NN: ");
        mearusingPRA(groundTruthHate0, groundTruthHate1, retrievedTweets_Combined);

        System.out.println("Original NN: ");
        mearusingPRA(groundTruthHate0, groundTruthHate1, retrievedTweets_Original);
    }

    public static void mearusingPRA(Set<String> groundTruthHate0, Set<String> groundTruthHate1,
            Map<String, Float> retrievedTweets) {

        Map<String, Integer> hateValues0 = getHateValues(retrievedTweets, inputfilePathCSV, 0);
        Map<String, Integer> hateValues1 = getHateValues(retrievedTweets, inputfilePathCSV, 1);

        System.out.println("hateValues0: " + hateValues0.size());
        System.out.println("hateValues1: " + hateValues1.size());

        int totalRetrievedTweets = retrievedTweets.size();

        // Calculate TP, TF, FP, FN
        int truePositives = hateValues1.size();
        int falsePositives = hateValues0.size();
        int falseNegatives = groundTruthHate1.size() - hateValues1.size();
        int trueNegatives = groundTruthHate0.size() - hateValues0.size();

        // Calculate precision, recall, F1 score, and accuracy
        double precision = (double) truePositives / (truePositives + falsePositives);
        double recall = (double) truePositives / (truePositives + falseNegatives);
        double f1Score = 2 * (precision * recall) / (precision + recall);
        double accuracy = (double) (truePositives + trueNegatives) / totalRetrievedTweets;

        // Print the metrics
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F1 Score: " + f1Score);
        System.out.println("Accuracy: " + accuracy);
    }

    private static Set<String> readGroundTruthHate0(String filePath, int value) {
        Set<String> groundTruthHate0 = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    String tweetId = values[0];
                    String hateValueStr = values[1];
                    try {
                        if (hateValueStr.equalsIgnoreCase("hate")) {
                            // Assuming that "hate" represents a hate value of 1
                            int hateValue = 1;
                            if (hateValue == value) {
                                groundTruthHate0.add(tweetId);
                            }
                        } else {
                            int hateValue = Integer.parseInt(hateValueStr);
                            if (hateValue == value) {
                                groundTruthHate0.add(tweetId);
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return groundTruthHate0;
    }

    private static Map<String, Float> readRetrievedTweets(String filePath) {
        Map<String, Float> retrievedTweets = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                Float tweetScore = Float.parseFloat(parts[1]);
                String tweet = parts[2].trim();
                retrievedTweets.put(tweet, tweetScore);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retrievedTweets;
    }

    private static Map<String, Integer> getHateValues(Map<String, Float> retrievedTweet, String filePath, int value) {
        Map<String, Integer> hateValuesHate0 = new HashMap<>();
        // Map<String, Integer> hateValuesHate1 = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            int rowNumber = 2; // Start from the second row
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String tweetId = Integer.toString(rowNumber); // Use row number as tweet ID
                rowNumber++;
                String tweet = values[0];
                String hateValueStr = values[1];
                if (retrievedTweet.containsKey(tweet)) {
                    if (hateValueStr.equalsIgnoreCase("hate")) {
                        // Assuming that "hate" represents a hate value of 1
                        int hateValue = 1;
                        if (hateValue == value) {
                            hateValuesHate0.put(tweet, hateValue);
                        }
                    } else {
                        int hateValue = Integer.parseInt(hateValueStr);
                        if (hateValue == value) {
                            hateValuesHate0.put(tweet, hateValue);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hateValuesHate0;
    }

}
