package uk.ac.rothamsted.ide.gbcl;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import gate.util.OffsetComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnnotationExporter {
    private static final Logger logger = LogManager.getLogger(AnnotationExporter.class);

    public AnnotationExporter() throws Exception {

        if (!Gate.isInitialised()) {
            Properties pro = new Properties();
            try {
                //pro.load(new FileInputStream(new File("resources/project.properties")));
                pro.load(Files.newInputStream(new File(logger.getClass().getClassLoader().getResource("project.properties").toURI()).toPath()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //
            // Load GATE.
            //
            String gateHome = pro.getProperty("GATE_HOME");
            logger.info("Initializing GATE...");
            // Set GATE HOME directory.
            Gate.setGateHome(new File(gateHome));
            // Set GATE plugins directory.
            Gate.setPluginsHome(new File(gateHome, "plugins"));
            System.out.println(Gate.getPluginsHome());
            // Initialise GATE.
            Gate.init();
            logger.info("Initializing GATE... Done.");
        }
    }



    protected void collectAnnotationRows(Document doc, String sourceAnnotation, Collection<String> individualAnnotation, BufferedWriter resultsWriter) throws InvalidOffsetException, IOException {

        // Row of annotations retrieved
        String allAnnotations;

        // List of all source annotations
        AnnotationSet sourceAS = doc.getAnnotations().get(sourceAnnotation);
        List<Annotation> sourceAnns = new ArrayList<>(sourceAS);
        sourceAnns.sort(new OffsetComparator());

        // list for each of the column header to hold multiple annotation instances in the same sentences
        // There may be multiple pests [pest1, pest2, pest3, ...] in the same sentence
        Set<String> pestAnnContent = new HashSet<>();
        Set<String> cropAnnContent = new HashSet<>();
        Set<String> locationAnnContent = new HashSet<>();
        Set<String> impactNumberAnnContent = new HashSet<>();
        Set<String> impactNumberUnitAnnContent = new HashSet<>();
        Set<String> impactDirectionAnnContent = new HashSet<>();
        Set<String> yieldMentionAnnContent = new HashSet<>();

        for (Annotation sourceAnn : sourceAnns) {
            long start = sourceAnn.getStartNode().getOffset();
            long end = sourceAnn.getEndNode().getOffset();


            //logger.info("sourceAnnotationType: " + sourceAnnotation);
            for (String annotationType : individualAnnotation) {
                //logger.info("annotationType: " + annotationType);
                //AnnotationSet annotationTypeMentions = doc.getAnnotations().get(annotationType,start,end);
                Set<Annotation> annotationTypeMentions = doc.getAnnotations().get(annotationType, start, end);
                if (annotationTypeMentions.isEmpty()) {
                    //containsAll = false;
                    break;
                } else {
                    //features.put("fake", "fake");

                    for (Annotation ann : annotationTypeMentions) {
                        String annString = getCleanAnnotationContent(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()).toString());
                        //logger.info("annString: " + annString);
                        if (annotationType.equals("Pest")) {
                            pestAnnContent.add(annString);
                        }
                        if (annotationType.equals("Crop")) {
                            cropAnnContent.add(annString);
                        }
                        if (annotationType.equals("Location")) {
                            locationAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactNumber")) {
                            impactNumberAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactNumberUnit")) {
                            impactNumberUnitAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactDirection")) {
                            impactDirectionAnnContent.add(annString);
                        }
                        if (annotationType.equals("YieldMention")) {
                            yieldMentionAnnContent.add(annString);
                        }
                    }
                }
            }



            allAnnotations = doc.getName() +
                        "\t" + sourceAnnotation +
                        // replace sentence line breaks with a whitespace
                        "\t" + getCleanAnnotationContent(doc.getContent().getContent(start, end).toString()) +
                        "\t" + String.join(",", pestAnnContent) + //pestAnnContent.toString() +
                        "\t" + String.join(",", cropAnnContent) + //+ cropAnnContent.toString() +
                        "\t" + String.join(",", locationAnnContent) + //+ locationAnnContent.toString() +
                        "\t" + String.join(",", impactNumberAnnContent) + //+ impactNumberAnnContent.toString() +
                        "\t" + String.join(",", impactNumberUnitAnnContent) + //+ impactNumberUnitAnnContent.toString() +
                        "\t" + String.join(",", impactDirectionAnnContent) + //+ impactDirectionAnnContent.toString() +
                        "\t" + String.join(",", yieldMentionAnnContent) + //+ yieldMentionAnnContent.toString() +
                        "\n";
            // write row to the spreadsheet
            resultsWriter.write(allAnnotations);

            // reset lists
            pestAnnContent.clear();
            cropAnnContent.clear();
            locationAnnContent.clear();
            impactNumberAnnContent.clear();
            impactNumberUnitAnnContent.clear();
            impactDirectionAnnContent.clear();
            yieldMentionAnnContent.clear();

        }

    }


    private String getCleanAnnotationContent(String originalAnnotationContent) {
        // New line Unix and Windows
        final String NEW_LINE_UNIX = "\n";
        final String NEW_LINE_WINDOWS = "\r\n";

        if (originalAnnotationContent.contains(NEW_LINE_WINDOWS)){
            return originalAnnotationContent.replace(NEW_LINE_WINDOWS, " ").trim();
        } else if (originalAnnotationContent.contains(NEW_LINE_UNIX)){
            return originalAnnotationContent.replace(NEW_LINE_UNIX, " ").trim();
        } else
            return originalAnnotationContent;
    }

}
