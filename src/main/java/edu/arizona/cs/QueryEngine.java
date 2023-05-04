package edu.arizona.cs;

import edu.arizona.cs.RandomTweetsFetcher;
import edu.arizona.cs.HateSpeechDetector;
import edu.arizona.cs.IndexBuilder;
import edu.arizona.cs.Evaluation;
import edu.arizona.cs.Output;

import org.apache.lucene.store.Directory;
import java.io.InputStream;
import java.util.*;

public class QueryEngine {

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
        // RandomTweetsFetcher tweetsFetcher = new RandomTweetsFetcher();
        // tweetsFetcher.getRandomTweetsFromAPI(50);

        // Step 2: Build index
        // IndexBuilder indexbuilder = new IndexBuilder();
        // IndexBuilder.buildIndex();

        // Step 3: Detect hate speech
        // HateSpeechDetector hateSpeechDetector = new HateSpeechDetector();
        // System.out.println("New NN query");
        // hateSpeechDetector.HateSpeechDetector(NewNNquery, "New");
        // System.out.println("Combined NN query");
        // hateSpeechDetector.HateSpeechDetector(CombinedNNquery, "Combined");
        // System.out.println("Original NN query");
        // hateSpeechDetector.HateSpeechDetector(OriginalNNquery, "Original");

        // Step 4: Evaluation of the complete model using the 3 queries
        Evaluation evaluation = new Evaluation();
        evaluation.Evaluation(ansFilePath_New, ansFilePath_Combined,
        ansFilePath_Original);
        evaluation.Evaluation(ansFilePath_New_NN, ansFilePath_Combined_NN,
        ansFilePath_Original_NN);
        evaluation.Evaluation(ansFilePath_New_Lex, ansFilePath_Combined_Lex,
        ansFilePath_Original_Lex);

        // Step 5: Creating a list of tweets retrieved to be passed to Classifier for
        // classification and predicting the label(hate/normal)
        // Output output = new Output();
        // output.OUTPUT();
    }
}
