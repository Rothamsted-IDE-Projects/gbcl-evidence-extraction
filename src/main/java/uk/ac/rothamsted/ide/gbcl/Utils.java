package uk.ac.rothamsted.ide.gbcl;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Utils {
    private static final Logger logger = LogManager.getLogger(Utils.class);

    public static Map<String, String> initFileIndex(String fileOrDirName) throws IOException {

        File fileOrDir = new File(fileOrDirName);
        Map<String, String> corpus = new TreeMap<>();

        if (fileOrDir.isDirectory()) {
            String[] list = fileOrDir.list();
            assert list != null;
            List<String> files = Arrays.asList(list);
            Collections.sort(files);

            for (String fileName : files) {//System.out.println(fileOrDir.getCanonicalPath()+"\n"+fileOrDir.getAbsoluteFile());
                if (fileName.endsWith(".xml")) {
                    String documentId = fileName.replaceFirst("\\.xml", "");
                    URL u = new File(fileOrDirName + "/" + fileName).toURI().toURL();
                    corpus.put(documentId, u.toString());
                }
                if (fileName.endsWith(".txt")) {
                    String documentId = fileName.replaceFirst("\\.txt", "");
                    URL u = new File(fileOrDirName + "/" + fileName).toURI().toURL();
                    corpus.put(documentId, u.toString());
                }
            }
        } else {
            String fileName = fileOrDir.getName().split("/")[fileOrDir.getName().split("/").length - 1];
            URL u = new File(fileOrDirName + "/" + fileName).toURI().toURL();
            corpus.put(fileName, u.toString());
        }

        return corpus;
    }

    static File createFileOrDirectoryIfNotExist(String fileOrDirName) {
        File newFileOrDir = null;
        if (fileOrDirName != null) {
            try {
                newFileOrDir = new File(fileOrDirName);
                if (!newFileOrDir.exists()) {
                    boolean success = (new File(fileOrDirName)).mkdir();
                    if (success) {
                        System.out.println("File or Directory: " + newFileOrDir + " created");
                    } else {
                        logger.warn("File or Directory is not set.");
                    }
                }
            } catch (Exception e) {
                logger.warn("File or Directory is not set.");
            }
        }
        return newFileOrDir;
    }

    public static void saveGateXml(gate.Document document, File outputFile, boolean preservingFormat) throws IOException {
        FileWriter fstream = new FileWriter(outputFile);
        BufferedWriter out = new BufferedWriter(fstream);
        if (preservingFormat) {
            out.write(document.toXml(document.getAnnotations(), true));
        } else {
            out.write(document.toXml());
        }
        out.close();
    }

}
