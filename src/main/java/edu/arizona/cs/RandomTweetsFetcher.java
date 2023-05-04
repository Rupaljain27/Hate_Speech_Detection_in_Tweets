package edu.arizona.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.api.TimelinesResources;

public class RandomTweetsFetcher {
    private static String CONSUMER_KEY = "";
    private static String CONSUMER_SECRET = "";
    private static String ACCESS_TOKEN = "";
    private static String ACCESS_TOKEN_SECRET = "";

    static String inputfilePathCSV = "src/main/resources/input.csv";

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
    public void getRandomTweetsFromAPI(int numTweets) throws TwitterException, IOException {

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

}
