package edu.arizona.cs;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

public class HateSpeechDetector {
    static String lexiconQueryFile = "expanded_hatespeech_lexicon.txt";
    static Directory index;
    static String indexPath = "src/main/index";

    // load the query string by using the hate speech lexicon
    public static String loadHateSpeechLexicon(String lexiconFile) {
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
    public static void HateSpeechDetector(String queryString, String name) throws Exception {
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
            TopDocs hits = searcher.search(query, 40);
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

            // Writing the tweets having Hate Speech to output.txt
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

}
