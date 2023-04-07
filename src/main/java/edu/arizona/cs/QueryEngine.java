package edu.arizona.cs;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.v1.Query;
import twitter4j.v1.QueryResult;
import twitter4j.v1.Status;
import twitter4j.*;

public class QueryEngine {

    private static final String CONSUMER_KEY = "6PihFV771WbP6KTBJYxDFbJ2Q";
    private static final String CONSUMER_SECRET = "NkDKlnA0RB2x81JeZkOmsq4i3ArsWPnf8WEsVoWyYd2qLFONlH";
    private static final String ACCESS_TOKEN = "1644134884032794625-ME0ScOeLoFBI67lecMbR1gkOTNQkLb";
    private static final String ACCESS_TOKEN_SECRET = "P23JUx21rQNe5Ok103ePB3n7jwcBOP156EktBCxzGuXOU";

    static boolean indexExists = false;
    static String inputFilePath = "input.txt";
    static String indexPath = "/path/to/index/dir";
    static Directory index;
    boolean similarity = false;

    public static void main(String[] args) throws Exception {

        // Step 1: Retrieve random tweets from Twitter API (not implemented) -- Passing random tweets for now
        List<String> randomTweets = Arrays.asList("hate", "offensive");// getRandomTweetsFromAPI(10);

        // Step 2: Build index
        buildIndex();

        // Step 3: Detect hate speech
        HateSpeechDetector(randomTweets);
    }

    // Functions to get Twitter API Instance
    // private static Twitter getTwitterInstance() throws TwitterException {
    // Twitter twitter = Twitter.newBuilder()
    // // .debugEnabled(true)
    // .oAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    // .oAuthAccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET)
    // .build();
    // // twitter.v2().tweets().updateStatus("Hello Twitter API!");
    // // TwitterFactory tf = new TwitterFactory(twitter.build());
    // return twitter.getInstance();
    // }

    // Functions to retrieve random tweets from Twitter API
    // private static List<String> getRandomTweetsFromAPI(int numTweets) throws
    // TwitterException {
    // Twitter twitter = getTwitterInstance();
    // List<String> tweets = new ArrayList<>();
    // Query query;
    // try {
    // query = new Query("hate");
    // query.count(numTweets);
    // QueryResult result = twitter.searchTweets(query).getTweets();
    // for (Status status : result.getTweets()) {
    // System.out.println(status.getText());
    // tweets.add(status.getText());
    // }
    // } catch (ParseException | TwitterException | IOException e) {
    // e.printStackTrace();
    // }
    // return tweets;
    // }

    // Function to build index
    private static void buildIndex() throws IOException {

        // Creating an index writer
        StandardAnalyzer analyzer = new StandardAnalyzer();
        index = new ByteBuffersDirectory();
        // Currently FSDirectory is not working, so using BufferedDirectory, will fix this soon
        // index = FSDirectory.open(Paths.get(indexPath));

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
                    doc.add(new StringField("docid", docName, Field.Store.YES));
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

    // Function to detect hate speech
    private static void HateSpeechDetector(List<String> hateSpeechTweets) throws Exception {

        // Creating an index reader
        try (IndexReader reader = DirectoryReader.open(index)) {

            // Creating a list to store the results
            List<ResultClass> ans = new ArrayList<ResultClass>();

            // Creating a query string, this will contain all the tweets from Neural Network which will work as a query
            String queryString = String.join(" ", hateSpeechTweets);

            // Creating a searcher to search the index
            IndexSearcher searcher = new IndexSearcher(reader);

            // Setting the similarity function to Cosine Similarity
            searcher.setSimilarity(new ClassicSimilarity());

            // Set up the query parser for the query
            org.apache.lucene.search.Query query = new QueryParser("text", new StandardAnalyzer())
                    .parse(queryString);

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
            
            // Printing the tweets having Hate Speech
            for (ResultClass result : ans) {
                System.out.println(result.DocName.get("Tweetid") + " : " + result.docScore);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
