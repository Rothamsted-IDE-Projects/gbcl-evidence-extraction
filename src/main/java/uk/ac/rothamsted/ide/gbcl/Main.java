package uk.ac.rothamsted.ide.gbcl;

import gate.Factory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    static Set<String> processOnlyDocuments = new HashSet<>(Collections.singletonList(
            "x"
    ));

    /**
     * Accepts the command line arguments. For correct arguments invoke the method for running the text mining pipeline.
     * @param args formal arguments with values
     */
    public static void main(String[] args) {

        if (args.length > 0) {
            if (args.length > 1) {
                String arguments = Arrays.toString(args).replace(", ", " ");
                arguments = arguments.substring(1, arguments.length() - 1);
                logger.info("ARGUMENTS: " + arguments);
                runGbclEvidenceExtraction(arguments);
            } else {
                System.out.println("Insufficient number of arguments. See README.");
            }
        } else {
            System.out.println("No arguments submitted! See README.");
        }
        System.out.println("\nAll done.");
    }

    /**
     * Verify the arguments and start the pipeline if they are correct.
     * @param args Arguments to run the pipeline.
     */
    private static void runGbclEvidenceExtraction(String args) {


        try {
            long time = System.currentTimeMillis();

            String inputFileOrDirName;
            String xmlOutputDirName;

            String annotationResultsFileName;

            boolean runPipe;
            boolean runExport;

            {
                JSAP jsap = new JSAP();
                {
                    FlaggedOption s = new FlaggedOption("input").setStringParser(JSAP.STRING_PARSER).setLongFlag("input").setShortFlag('i');
                    s.setHelp("The name of input file or directory.");
                    jsap.registerParameter(s);
                }
                {
                    FlaggedOption s = new FlaggedOption("xmldir").setStringParser(JSAP.STRING_PARSER).setLongFlag("xmldir").setShortFlag('x');
                    s.setHelp("The name of xml file or directory.");
                    jsap.registerParameter(s);
                }
                {
                    FlaggedOption s = new FlaggedOption("annotationResults").setStringParser(JSAP.STRING_PARSER).setLongFlag("annotation").setShortFlag('o');
                    s.setHelp("Name of the CSV file for saving annotation results.");
                    jsap.registerParameter(s);
                }
                {
                    Switch s = new Switch("runPipe").setLongFlag("pipe");
                    s.setHelp("runPipe");
                    jsap.registerParameter(s);
                }
                {
                    Switch s = new Switch("runExport").setLongFlag("export");
                    s.setHelp("runExport");
                    jsap.registerParameter(s);
                }

                // Parse the command-line arguments.
                JSAPResult config = jsap.parse(args);

                // Help messages
                if (!config.success()) {
                    System.err.println();
                    System.err.println(" " + jsap.getUsage());
                    System.err.println();
                    System.err.println(jsap.getHelp());
                    System.exit(1);
                }

                inputFileOrDirName = config.getString("input");
                if (inputFileOrDirName != null) {
                    logger.info("corpus path: " + inputFileOrDirName);
                }

                xmlOutputDirName = config.getString("xmldir");
                if (xmlOutputDirName != null) {
                    logger.info("xml output path: " + xmlOutputDirName);
                }

                annotationResultsFileName = config.getString("annotationResults");
                if (annotationResultsFileName != null) {
                    logger.info("annotationResults File path: " + annotationResultsFileName);
                }

                runPipe = config.getBoolean("runPipe");
                if (runPipe) {
                    logger.info("runPipe: " + true);
                }

                runExport = config.getBoolean("runExport");
                if (runExport) {
                    logger.info("runExport: " + true);
                }


                // Create xml output directory if it does not exist.
                File xmlOutputDir = null;
                if (xmlOutputDirName != null) {
                    xmlOutputDir = Utils.createFileOrDirectoryIfNotExist(xmlOutputDirName);
                }

                // RUN PIPELINE
                if (runPipe) {
                    // Set GATE-X.X installation directory from project.properties in /src/main/resources
                    Properties pro = new Properties();
                    try {
                        pro.load(Files.newInputStream(new File(logger.getClass().getClassLoader().getResource("project.properties").toURI()).toPath()));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    String gateHome = pro.getProperty("GATE_HOME");

                    // Initialize pipeline.
                    Pipeline pipeline = new Pipeline(gateHome);
                /*
                 boolean runDocumentResetter,
                 boolean runTokenizer,
                 boolean runGazetteer,
                 boolean runSentenceSplitter,
                 boolean runPosTagger,
                 boolean runSemanticTagger,
                 boolean runCooccurrenceExtractor
                 */
                    pipeline.init(
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true
                    );

                    //
                    // Prepare and Process corpus.
                    //
                    Map<String, String> fileIndex = Utils.initFileIndex(inputFileOrDirName);

                    // Monitor which file is currently processed.
                    int numberOfFilesToProcess = fileIndex.size();

                    if (processOnlyDocuments.size() > 1) {
                        numberOfFilesToProcess = processOnlyDocuments.size() - 1;
                    }
                    int numberOfFileProcessed = 0;

                    for (String fileName : fileIndex.keySet()) {

                        if (processOnlyDocuments.size() > 1 && !processOnlyDocuments.contains(fileName)) {
                            continue;
                        }

                        numberOfFileProcessed++;

                        logger.info("Annotating \n===============================================================\n"
                                + "Document (" + numberOfFileProcessed + " of " + numberOfFilesToProcess + "): " + fileName + " AT " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())

                                + "\n===============================================================\n");

                        gate.Document doc = Factory.newDocument(new URL(fileIndex.get(fileName)));
                        doc.setName(fileName);

                        //log.info(doc.getContent());
                        pipeline.execute(doc);

                        if (xmlOutputDir != null) {
                            Utils.saveGateXml(doc, new File(xmlOutputDir + "/" + doc.getName() + ".xml"), false);
                        }

                        Factory.deleteResource(doc);

                        logger.info("Processing Document Time " + (System.currentTimeMillis() - time) / 1000 + " seconds = " + (System.currentTimeMillis() - time) / 1000 / 60 + " minutes");
                    }

                    logger.info("Processing Corpus Time " + (System.currentTimeMillis() - time) / 1000 + " seconds");
                }

                // export annotations into a tsv-formatted file
                if (runExport) {
                    // initialize GATE
                    AnnotationExporter exporter = new AnnotationExporter();
                    // initialize directory with annotated XML files
                    Map<String, String> fileIndex = Utils.initFileIndex(xmlOutputDirName);

                    // Monitor file currently being processed.
                    int numberOfFilesToProcess = fileIndex.size();
                    if(processOnlyDocuments.size()>1){
                        numberOfFilesToProcess = processOnlyDocuments.size()-1;
                    }
                    int numberOfFileProcessed = 0;

                    // append to the result tsv
                    boolean append = false;

                    BufferedWriter annOutputWriter;
                    assert annotationResultsFileName != null;
                    FileWriter fileWriter = new FileWriter(annotationResultsFileName, append);
                    annOutputWriter = new BufferedWriter(fileWriter);

                    // Add headers to the columns in the spreadsheet
                    String headers = (
                            "document" +
                            "\t" + "Annotation Type" +
                            "\t" + "Sentence" +
                            "\t" + "Pest" +
                            "\t" + "Crop" +
                            "\t" + "Location" +
                            "\t" + "ImpactNumber" +
                            "\t" + "ImpactNumberUnit" +
                            "\t" + "ImpactDirection" +
                            "\t" + "YieldMention" +
                            "\n");
                    annOutputWriter.write(headers);

                    // Sentences of sliding windows length 1/2/3
                    // See CooccurrenceMatcher.jape for the available annotations
                    List<String> sourceAnnTypes = Arrays.asList(
                            "Coo_P_1_sent",
                            "Coo_P_2_sent",
                            "Coo_P_3_sent",
                            "Coo_PC_1_sent",
                            "Coo_PC_2_sent",
                            "Coo_PC_3_sent",
                            "Coo_PYm_1_sent",
                            "Coo_PYm_2_sent",
                            "Coo_PYm_3_sent",
                            "Coo_PCL_1_sent",
                            "Coo_PCL_2_sent",
                            "Coo_PCL_3_sent",
                            "Coo_PIdYm_1_sent",
                            "Coo_PPIdYm_2_sent",
                            "Coo_PPIdYm_3_sent",
                            "Coo_PCLIn_1_sent",
                            "Coo_PCLIn_2_sent",
                            "Coo_PCLIn_3_sent",
                            "Coo_PCLInU_1_sent",
                            "Coo_PCLInU_2_sent",
                            "Coo_PCLInU_3_sent",
                            "Coo_PCLInUId_1_sent",
                            "Coo_PCLInUId_2_sent",
                            "Coo_PCLInUId_3_sent",
                            "Coo_PCLInUIdYm_1_sent",
                            "Coo_PCLInUIdYm_2_sent",
                            "Coo_PCLInUIdYm_3_sent"
                    );
                    // Individual annotations present in the sentence annotations of sliding windows lengths 1/2/3
                    List<String> targetAnnotations = Arrays.asList(
                            "Pest",
                            "Crop",
                            "Location",
                            "ImpactNumber",
                            "ImpactNumberUnit",
                            "ImpactDirection",
                            "YieldMention"
                    );

                    for (String fileName : fileIndex.keySet()) {
                        if(processOnlyDocuments.size()>1 && !processOnlyDocuments.contains(fileName)){
                            continue;
                        }
                        numberOfFileProcessed++;

                        logger.info("Exporting annotations from \n===============================================================\n"
                                + "Document (" + numberOfFileProcessed + " of " + numberOfFilesToProcess + "): " + fileName + " AT " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())

                                + "\n===============================================================\n");

                        gate.Document doc = Factory.newDocument(new URL(fileIndex.get(fileName)));
                        doc.setName(fileName);



                        for (String annotation: sourceAnnTypes){
                            exporter.collectAnnotationRows(doc, annotation, targetAnnotations, annOutputWriter);
                        }

                        Factory.deleteResource(doc);
                    }

                    annOutputWriter.close();
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}