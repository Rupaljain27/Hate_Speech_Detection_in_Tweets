package edu.arizona.cs;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;

public class IndexBuilder {
    static String indexPath = "src/main/index";
    static Directory index;
    static String inputfilePathCSV = "src/main/resources/input.csv";
    static boolean indexExists = false;

    // Function to build index
    public static void buildIndex() throws IOException {

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

}
