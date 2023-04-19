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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
    static String filePath = "src/main/resources/input.txt";
    static String inputFilePath = "input.txt";
    static String QueryFilePath = "improved_NN_hatespeech_query.txt";
    static String indexPath = "src/main/index";
    static String lexiconFilePath = "hatespeech_lexicon.txt";
    static Directory index;
    boolean similarity = false;

    public static void main(String[] args) throws Exception {

        String NNquery = "";
        // Creating a query string, this will contain all the tweets from Neural Network
        // which will work as a query
        InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + QueryFilePath);
        try (Scanner inputScanner = new Scanner(inputStream)) {
            while (inputScanner.hasNextLine()) {
                String line = inputScanner.nextLine();
                NNquery += line + " ";
            }
            inputScanner.close();
        }
        NNquery = NNquery.trim();

        // Step 1: Retrieve random tweets from Twitter API (not implemented)
        getRandomTweetsFromAPI(50);

        // Step 2: Build index
        buildIndex();

        // Step 3: Detect hate speech
        HateSpeechDetector(NNquery);
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

        // Create a FileWriter object
        FileWriter writer = new FileWriter(filePath, true);

        try {
            // List<Status> statuses = twitter.getHomeTimeline();
            List<Status> statuses = ((TimelinesResources) twitter).getHomeTimeline();

            // Create a query object and set its parameters
            // Query query = new Query("hate OR offensive");
            // query.setCount(numTweets);
            // query.setResultType(Query.RECENT); // Set result type to recent tweets
            // query.setLang("en"); // Set language to English
            // // Execute the query and retrieve the search results
            // QueryResult result = twitter.search(query);
            // List<Status> statuses = result.getTweets();

            System.out.println("Number of tweets retrieved: " + statuses.size());

            // Iterate through the tweets and get the last tweet ID
            InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + inputFilePath);
            String line = "";
            try (Scanner inputScanner = new Scanner(inputStream)) {
                while (inputScanner.hasNextLine()) {
                    line = inputScanner.nextLine();
                    // if (!inputScanner.hasNextLine()) {
                    // }
                }
            }
            String[] tokens = line.split(" ", 2);
            Integer counter = 1;
            if (line != "") {
                counter = Integer.parseInt(tokens[0].substring(5)) + 1;
            }
            for (Status status : statuses) {
                // Remove punctuation from tweet text
                String text = status.getText().replaceAll("\\p{Punct}", "");
                text = text.replaceAll("\n", "");

                // Write new line of text to the file
                text = normalizeText(text);
                text = "Tweet" + Integer.toString(counter) + " " + text;

                writer.write(text + "\n");
                counter += 1;
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

    // Function to build index
    private static void buildIndex() throws IOException {
        
        // Creating an index writer
        StandardAnalyzer analyzer = new StandardAnalyzer();
        // index = new ByteBuffersDirectory();
        index = FSDirectory.open(Paths.get(indexPath));

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        try (IndexWriter writer = new IndexWriter(index, config)) {

            // Reading the input file
            InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + inputFilePath);

            try (Scanner inputScanner = new Scanner(inputStream)) {
                while (inputScanner.hasNextLine()) {
                    String line = inputScanner.nextLine();
                    String[] tokens = line.split(" ", 2);
                    String docName = tokens[0];
                    String docText = tokens[1];
                    Document doc = new Document();
                    doc.add(new TextField("text", docText, Field.Store.YES));
                    doc.add(new StringField("Tweetid", docName, Field.Store.YES));
                    writer.addDocument(doc);
                }
                inputScanner.close();
            }
            indexExists = true;
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // load the query string by using the hate speech lexicon
    private static String loadHateSpeechLexicon() {
        // Reading the input file
        InputStream inputStream = QueryEngine.class.getResourceAsStream("/" + lexiconFilePath);
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
    private static void HateSpeechDetector(String queryString) throws Exception {

        // Create index
        index = FSDirectory.open(Paths.get(indexPath));

        // Creating an index reader
        try (IndexReader reader = DirectoryReader.open(index)) {

            // Creating a list to store the results
            List<ResultClass> ans = new ArrayList<ResultClass>();

            // improvement of traditional method - load lexicon from hate speech text file
            String lexiconQueryString = loadHateSpeechLexicon();

            // Creating a searcher to search the index
            IndexSearcher searcher = new IndexSearcher(reader);

            // Setting the similarity function to Cosine Similarity
            searcher.setSimilarity(new ClassicSimilarity());

            // Set up the query parser for the query
            org.apache.lucene.search.Query query = new QueryParser("text", new StandardAnalyzer()).parse(queryString);

            // Searching the index
            TopDocs hits = searcher.search(query, 20);
            ScoreDoc[] score = hits.scoreDocs;

            // Storing the results in the list
            for (int i = 0; i < score.length; ++i) {
                int docId = score[i].doc;
                Document doc = searcher.doc(docId);
                ResultClass result = new ResultClass();
                result.DocName = doc;
                result.docScore = score[i].score;
                ans.add(result);
            }
            System.out.println("Hate Speech Tweets: ***************");
            System.out.println("Hate Speech tweets retrieved: " + ans.size());
            // Printing the tweets having Hate Speech
            for (ResultClass result : ans) {
                System.out.println(
                        result.DocName.get("Tweetid") + " : " + result.DocName.get("text") + " : " + result.docScore);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
