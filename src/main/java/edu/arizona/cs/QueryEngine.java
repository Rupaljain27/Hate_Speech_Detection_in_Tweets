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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
    static String filePathcsv = "src/main/resources/input.csv";
    static String NewQueryFile = "new_dataset_hatespeech_query.txt";
    static String CombinedQueryFile = "combined_dataset_hatespeech_query.txt";
    static String OriginalQueryFile = "old_dataset_hatespeech_query.txt";
    static String indexPath = "src/main/index";
    static String lexiconFilePath = "hatespeech_lexicon.txt";
    static String lexiconQueryFile = "expanded_hatespeech_lexicon.txt";
    static Directory index;
    boolean similarity = false;

    public static void main(String[] args) throws Exception {

        String NewNNquery = "";
        String CombinedNNquery = "";
        String OriginalNNquery = "";
        // Creating a query string, this will contain all the tweets from Neural Network
        // which will work as a query
        InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + CombinedQueryFile);
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                NewNNquery += line + " ";
            }
            inputScanner.close();
        }
        NewNNquery = NewNNquery.trim();

        inputStream = QueryEngine.class.getResourceAsStream("/" + OriginalQueryFile);
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                CombinedNNquery += line + " ";
            }
            inputScanner.close();
        }
        CombinedNNquery = CombinedNNquery.trim();

        inputStream = QueryEngine.class.getResourceAsStream("/" + NewQueryFile);
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
        // System.out.println("New NN query");
        // HateSpeechDetector(NewNNquery, "New");
        // System.out.println("Combined NN query");
        // HateSpeechDetector(CombinedNNquery, "Combined");
        // System.out.println("Original NN query");
        // HateSpeechDetector(OriginalNNquery, "Original");

        errorAnalysis();
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

            File file = new File(filePathcsv);
            boolean isNewFile = file.length() == 0;
            try (FileWriter filewriter = new FileWriter(filePathcsv, true)) {
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
                Reader reader = new FileReader(filePathcsv);
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
        // Creating an index reader
        try (IndexReader reader = DirectoryReader.open(index);
                BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false))) {

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
            // Printing the tweets having Hate Speech
            // for (ResultClass result : ans) {
            // System.out.println(
            // result.DocName.get("Tweetid") + " : " + result.DocName.get("text") + " : " +
            // result.docScore);
            // }

            // improvement of traditional method - load lexicon from hate speech text file
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
            System.out.println("Total number of tweets retrieved from Lexicon: " + score + " : " + hits);

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
                    // System.out.println("existingResult.DocName: " + existingResult.DocName + "
                    // Score: " + existingResult.docScore);
                    if (existingResult.DocName.equals(result.DocName)) {
                        entryExists = true;
                        break;
                    }
                }
                if (!entryExists) {
                    ans.add(result);
                } else {
                    ans.add(result);
                }
            }
            System.out.println("Hate Speech tweets retrieved: " + ans.size());
            // Printing the tweets having Hate Speech
            // for (ResultClass result : ans) {
            // System.out.println(
            // result.DocName.get("Tweetid") + " : " + result.DocName.get("text") + " : " +
            // result.docScore);
            // }

            // Printing the tweets having Hate Speech and writing to output.txt
            for (ResultClass result : ans) {
                String output = result.DocName.get("Tweetid") + " : " + result.docScore + " : "
                        + result.DocName.get("text");
                System.out.println(output);
                writer.write(output);
                writer.newLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void errorAnalysis() {
        String groundTruthFilePath = "src/main/resources/input.csv";
        String ansFilePath_Combined = "src/main/resources/output_Combined.txt";
        String ansFilePath_New = "src/main/resources/output_New.txt";
        String ansFilePath_Original = "src/main/resources/output_Original.txt";

        Set<String> groundTruthHate0 = readGroundTruthHate0(groundTruthFilePath, 0);
        Set<String> groundTruthHate1 = readGroundTruthHate0(groundTruthFilePath, 1);
        Set<String> retrievedTweets = readRetrievedTweets(ansFilePath_New);
        Map<String, Integer> hateValues0 = getHateValues(retrievedTweets, groundTruthFilePath, 0);
        Map<String, Integer> hateValues1 = getHateValues(retrievedTweets, groundTruthFilePath, 1);

        System.out.println("groundTruthHate0: " + groundTruthHate0.size());
        System.out.println("groundTruthHate1: " + groundTruthHate1.size());
        System.out.println("retrievedTweets: " + retrievedTweets.size());
        System.out.println("hateValues: " + hateValues0.size());
        System.out.println("hateValues: " + hateValues1.size());

        // int totalRetrievedTweets = groundTruthHate0.size() + groundTruthHate1.size();
        int totalRetrievedTweets = retrievedTweets.size();

        // Calculate true positives
        int truePositives = hateValues1.size();
        // for (String tweet : retrievedTweets) {
        //     if (groundTruthHate0.contains(tweet)) {
        //         truePositives++;
        //     }
        // }

        // Calculate false positives
        int falsePositives = hateValues0.size();
        // int falsePositives = totalRetrievedTweets - truePositives;

        // Calculate false negatives
        int falseNegatives = groundTruthHate1.size() - hateValues1.size();
        // int falseNegatives = totalGroundTruthHate1 - truePositives;

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
                        // int hateValue = Integer.parseInt(hateValueStr);
                        // if (hateValue == value) {
                        // groundTruthHate0.add(tweetId);
                        // }
                    } catch (NumberFormatException e) {
                        // Handle the case where the hate value is not a valid integer
                        // You can choose to ignore it, log a warning, or handle it in any other way
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return groundTruthHate0;
    }

    private static Set<String> readRetrievedTweets(String filePath) {
        Set<String> retrievedTweets = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // String tweetId = extractTweetId(line);
                String[] parts = line.split(":");
                String tweetId = parts[0].trim();
                retrievedTweets.add(tweetId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retrievedTweets;
    }

    private static Map<String, Integer> getHateValues(Set<String> ans, String filePath, int value) {
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
                if (ans.contains(tweetId)) {
                    if (hateValueStr.equalsIgnoreCase("hate")) {
                        // Assuming that "hate" represents a hate value of 1
                        int hateValue = 1;
                        System.out.println("tweet: " + 1);
                        if (hateValue == value) {
                            hateValuesHate0.put(tweet, hateValue);
                            // } else if (hateValue == 1) {
                            // hateValuesHate1.put(tweet, hateValue);
                        }
                    } else {
                        int hateValue = Integer.parseInt(hateValueStr);
                        if (hateValue == value) {
                            hateValuesHate0.put(tweet, hateValue);
                            // } else if (hateValue == 1) {
                            // hateValuesHate1.put(tweet, hateValue);
                        }
                    }
                }
                // String[] values = line.split(",");
                // String tweetId = values[0];
                // int hateValue = Integer.parseInt(values[1]);
                // if (ans.contains(tweetId)) {
                // if (hateValue == 0) {
                // hateValuesHate0.put(tweetId, hateValue);
                // } else if (hateValue == 1) {
                // hateValuesHate1.put(tweetId, hateValue);
                // }
                // }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Map<String, Integer> hateValues = new HashMap<>();
        // hateValues.putAll(hateValuesHate0);
        // hateValues.putAll(hateValuesHate1);

        return hateValuesHate0;
    }

}
