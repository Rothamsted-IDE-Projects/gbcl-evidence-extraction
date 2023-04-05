package uk.ac.rothamsted.ide.gbcl;

import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import gate.util.OffsetComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

public class CooccurrenceMatcher {
    private static final Logger logger = LogManager.getLogger(CooccurrenceMatcher.class);
    // line in csv file for annotations results
    private static String annLine;
    private static String sentenceAnnType;
    // annotated sentence content
    private static String sentenceAnnInstance;
    Set<String> pestAnnInstance = new HashSet<>();
    Set<String> cropAnnInstance = new HashSet<>();
    Set<String> locationAnnInstance = new HashSet<>();
    Set<String> impactNumberAnnInstance = new HashSet<>();
    Set<String> impactNumberUnitAnnInstance = new HashSet<>();
    Set<String> impactDirectionAnnInstance = new HashSet<>();
    Set<String> yieldMentionAnnInstance = new HashSet<>();
    // contents for individual annotations
    private static String allAnnTypeContent;
    private static Set<String> annotationTypeContent = new HashSet<>();
    private static BufferedWriter annOutputWriter;
    public static FileWriter fileWriter;
    private static String annotationResultsFileName = "annotation_spreadsheet.csv";
    public CooccurrenceMatcher() throws IOException {
        boolean append = true;
        fileWriter = new FileWriter(annotationResultsFileName, append);
        annOutputWriter = new BufferedWriter(fileWriter);
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
    }

    public void annotate(gate.Document doc, Collection<String> annotationTypes, String resultAnnotationTypeName, int contextSize) throws InvalidOffsetException, IOException {

        //String newAnnotationType = annotationTypes+"_"+contextSize+"_sent";
        // add document name to csv
        //annLine = doc.getName();

        AnnotationSet sentenceAs = doc.getAnnotations().get("Sentence");
        List<Annotation> sentencesAnns = new ArrayList<>(sentenceAs);
        sentencesAnns.sort(new OffsetComparator());

        for (int i = 0; i < sentencesAnns.size() - (contextSize-1); i++) {
            long start = sentencesAnns.get(i).getStartNode().getOffset();
            long end = sentencesAnns.get(i+(contextSize-1)).getEndNode().getOffset();

            // add sentence content to csv
            sentenceAnnInstance = getCleanAnnotationContent(doc.getContent().getContent(start, end).toString());

            FeatureMap features = Factory.newFeatureMap();
            // logger.info("Searching for annotations "+annotationTypes +" in sentence window "+contextSize);
            // System.out.println("Search for annotations "+annotationTypes +" in sentence window "+contextSize);

            boolean containsAll = true;
            for(String annotationType : annotationTypes){
                AnnotationSet annotationTypeMentions = doc.getAnnotations().get(annotationType,start,end);
                if(annotationTypeMentions.isEmpty()){
                    containsAll = false;
                    break;
                } else {
                    features.put("fake", "fake");
                    for (Annotation ann : annotationTypeMentions) {
                        String annString = getCleanAnnotationContent(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()).toString());
                        //annotationTypeContent.add(annString);
                        if (annotationType.equals("Pest")) {
                            pestAnnInstance.add(annString);
                        }
                        if (annotationType.equals("Crop")) {
                            cropAnnInstance.add(annString);
                        }
                        if (annotationType.equals("Location")) {
                            locationAnnInstance.add(annString);
                        }
                        if (annotationType.equals("ImpactNumber")) {
                            impactNumberAnnInstance.add(annString);
                        }
                        if (annotationType.equals("ImpactNumberUnit")) {
                            impactNumberUnitAnnInstance.add(annString);
                        }
                        if (annotationType.equals("ImpactDirection")) {
                            impactDirectionAnnInstance.add(annString);
                        }
                        if (annotationType.equals("YieldMention")) {
                            yieldMentionAnnInstance.add(annString);
                        }
                    }
                    allAnnTypeContent += annotationType + ":" + annotationTypeContent.toString();

                }
                //annotationTypeContent.clear();


            }
            if(containsAll){
                //System.err.println(annotationTypes);
                String resultAnnotationTypeNameToUse;
                if(resultAnnotationTypeName != null)
                    resultAnnotationTypeNameToUse = resultAnnotationTypeName;
                else
                    resultAnnotationTypeNameToUse = annotationTypes.toString();


                if(contextSize == 1){
                    sentenceAnnType = resultAnnotationTypeNameToUse+"_"+1+"_sent";
                    doc.getAnnotations().add(start, end, resultAnnotationTypeNameToUse+"_"+1+"_sent", Factory.newFeatureMap());
                }
                if(contextSize == 2){
                    AnnotationSet annotationTypeMentions1 = doc.getAnnotations().get(resultAnnotationTypeNameToUse+"_"+1+"_sent",start,end);
                    sentenceAnnType = resultAnnotationTypeNameToUse+"_"+2+"_sent";
                    //if(annotationTypeMentions1.isEmpty()){
                    doc.getAnnotations().add(start, end, resultAnnotationTypeNameToUse+"_"+2+"_sent", Factory.newFeatureMap());
                    //}
                }
                if(contextSize == 3){
                    AnnotationSet annotationTypeMentions1 = doc.getAnnotations().get(resultAnnotationTypeNameToUse+"_"+1+"_sent",start,end);
                    AnnotationSet annotationTypeMentions2 = doc.getAnnotations().get(resultAnnotationTypeNameToUse+"_"+2+"_sent",start,end);
                    sentenceAnnType = resultAnnotationTypeNameToUse+"_"+3+"_sent";
                    //if(annotationTypeMentions1.isEmpty() && annotationTypeMentions2.isEmpty()){
                    doc.getAnnotations().add(start, end, resultAnnotationTypeNameToUse+"_"+3+"_sent", Factory.newFeatureMap());
                    //}
                }

                annLine = (doc.getName() +
                        "\t" + sentenceAnnType +
                        "\t" + sentenceAnnInstance +
                        "\t" + pestAnnInstance +
                        "\t" + cropAnnInstance +
                        "\t" + locationAnnInstance +
                        "\t" + impactNumberAnnInstance +
                        "\t" + impactNumberUnitAnnInstance +
                        "\t" + impactDirectionAnnInstance +
                        "\t" + yieldMentionAnnInstance +
                        "\n");
                annOutputWriter.write(annLine);
                //logger.debug(contextSize+" sentence(s): "+doc.getContent().getContent(start, end));
            } // if

            logger.info(annLine);
            sentenceAnnType = "";
            sentenceAnnInstance = "";
            //allAnnTypeContent = "";

            pestAnnInstance.clear();
            cropAnnInstance.clear();
            locationAnnInstance.clear();
            impactNumberAnnInstance.clear();
            impactNumberUnitAnnInstance.clear();
            impactDirectionAnnInstance.clear();
            yieldMentionAnnInstance.clear();

        } // for
    } // public

    public void close() throws IOException {
        //annOutputWriter.flush();
        annOutputWriter.close();
    }
    private String getCleanAnnotationContent(String originalAnnotationContent) {
        String field;
        // New line Unix and Windows
        final String NEW_LINE_UNIX = "\n";
        final String NEW_LINE_WINDOWS = "\r\n";
        final String DOUBLE_QUOTES = "\"";
        final String EMBEDDED_DOUBLE_QUOTES = "\"\"";

        if (originalAnnotationContent.contains(NEW_LINE_WINDOWS)){
            return originalAnnotationContent.replace(NEW_LINE_WINDOWS, " ").trim();
        } else if (originalAnnotationContent.contains(NEW_LINE_UNIX)){
            return originalAnnotationContent.replace(NEW_LINE_UNIX, " ").trim();
        } else if (originalAnnotationContent.contains(DOUBLE_QUOTES)){
            field = originalAnnotationContent.replace(DOUBLE_QUOTES, EMBEDDED_DOUBLE_QUOTES);
            field = '"' + field + '"';
            return field;
        }else
            return originalAnnotationContent;
    }

    public static void main(String[] args) throws Exception {

        // String text = "Should it be used with Indinavir? I worked at an HIV clinic for two years. We used St. John's with great results right alongside conventional anti-virals with no adverse interaction. The major side-effects of Indinavir are headaches, nausea, vomiting, diarrhea, dizziness and insomnia. Again, after consulting with your physician, you may consider trying a good quality St. John's Wort with regular monitoring. What about Digoxin and St. John's? This is probably referring to a small (162) study of in which St. John's demonstrated its ability to promote stable heart function while decreasing depression. So, yes, it's an herb trying to stabilize the organism and may tonify the heart. This is a beneficial side effect. Why is Theophylline, primarily an asthmatic drug, listed? Probably for the same reason as digoxin. Theophylline can cause life-threatening ventricular arrythmias. (And frankly, don't use St. John's when using Theophylline because if the ventricular arrythmias occur, St. John's will be blamed.) Should St. John's be used with immune suppressants such as Cyclosporin? Absolutely not. The overwhelming majority of medicinal herbs should be avoided with immune suppressants. Herbs strengthen the human organism. They would work directly against drugs designed to weaken the human organism. What about using St. John's with chemotherapy? In my practice, I tend to avoid most herbs during the actual process of chemotherapy. I use them before hand to build up the body and afterward to repair the damage to the normal cells. Periodically, the pharmaceutical industry feels threatened by a natural product and goes after it in a very deliberate way. About 10 years ago, it was L-Tryptophan. L-Tryptophan was an inexpensive amino acid that was extremely effective in increasing seratonin levels and was virtually without side effects. A Japanese company produced a single bad batch after failing to complete the chemical process and several deaths occurred. The FDA was able to trace the bad batch back to the Japanese company, but still decided to ban the supplement. Two years ago, the media flooded the public with warnings that St. John's Wort would cause birth control pills to fail resulting in a host of unwanted pregnancies. When the pregnancies didn't occur, the pharmaceuticals tried a different angle. If it were patentable and the pharmaceutical companies could make money off of it, this Internet email warning about herbs would not have occurred. Consider Viagra. It has caused over 60 deaths and continues to be on the market.";
        // String text = "Concentrations of the alcohols, especially of the higher-moiecular-weight ones, are the rea- sons why GC methods cannot be applied satisfactorily to the quantification of endogenous a ! cohols . In this paper two methods are described for the quantitative determination of endogeaous ethanol and endogenous higher-molecular-wei  t cohols . taking advantage of the high specificity and sensitivity of mass fra mento- graphy ( MF ). Serum and urine samp ! es were collected from norrnal individuals , from hos- pttal patients without obvious metabolic defects and from diabetic patients who had abstained from drinking alcoholic beverages for 3 days before sample collection . Serum was obtained from venous blood by centrifugation for 10 min at 1600 g . Urine The GC-MF determinations were performed on a combination of a lMode1 2700 gas chromatograph and a CH 5 mass spectrometer ( Varian-MAT , Bremen . Ethanol was determined by direct injection of a serum or urine sample . To 0 . 5 ml of serum or urine 2  1 of internal standard ( 50 J . LI of diethyl ether in 100 ml of distilled water ) were added . The mixuxe was thoro ly shaken and I {} 1 The water from the directly injected serum or urine sample was by-passed between the outlet of the GC column and the interface ";
        ///String text = "Expired air samples have been analyzed from three groups of human subjects (normal, liver dysfunction, lung cancer) and the baboon (Papio anubus). Of the several hundred compounds present, three compounds were of particular interest due to their structural relationship to the isoprenoid-type intermediates in the sterol pathway. These compounds were 1-methyl-4-(1-methyl-ethenyl)-cyclohexene, 6-methyl-5-hepten-2-one, and 6,10-dimethyl-5,9-undecadien-2-one. Hydroxyacetone was also found in all samples screened. The relationship of these compounds to the non-sterol pathway of mevalonate metabolism is discussed.";
        // String text = "This is chemical 1,1,2-Trichlorotrifluoroethane (CFC-113, Freon-113) . This is chemical 1,1-Biphenyl, 2,2-diethyl- . This is chemical 1,1-dimethylethyl)thio] acetic acid . This is chemical 1,2,4-Trimethylbenzene . This is chemical 1,2,4-Trimethylbenzene . This is chemical 1,2,4-Trimethylbenzene . This is chemical 1,2,4-Trimethylbenzene . This is chemical 1,2-Benzenedicarboxylic acid . This is chemical 1,3,5,7-Cyclooctatetraene . This is chemical 1,3,5-Trimethylbenzene (mesitylene) . This is chemical 1,3-Cyclopentadiene, 1-methyl . This is chemical 1,3-Pentadiene . This is chemical 1,4-Dichlorobenzene, (para-Dichlorobenzene) . This is chemical 1,4-Dimethyl cyclohexane . This is chemical 1,4-Pentadiene . This is chemical 1,5,9-Trimethyl-1,5 9-cyclododecatriene . This is chemical 1,5-Dimethylcyclopentene . This is chemical 1-Chloro-2-methylbutane . This is chemical 1-Heptanol, 2-propyl- . This is chemical 1H-Indene, 2,3-dihydro-4-methyl . This is chemical 1-Methyl-3-isopropylbenzene . This is chemical 1-Methyl-4-(1-methylethyl) benzene . This is chemical 1-Methyl-4-(1-methylethyl) benzene . This is chemical 1-Methyl-4-(1-methylethyl) benzene . This is chemical 1-methyl-5-(1-methylethenyl)cyclohexene . This is chemical 1-(Methylthio)-propane . This is chemical 1-Octanol, 2-butyl- . This is chemical 1-Octen-3-ol . This is chemical 1-Octene . This is chemical 1-Propene, 1-(methylthio)-, (E)- . This is chemical 2,2,4,6,6-Pentamethylheptane . This is chemical 2,2,4,6,6-Pentamethylheptane . This is chemical 2,2,4,6,6-pentamethyl heptane . This is chemical 2,2,4-Trimethyl-1,3-pentanediol diisobutyrate . This is chemical 2,2,4-Trimethyl-1,3-pentanediol diisobutyrate . This is chemical 2,2-Dimethylbutane . This is chemical 2,2-Dimethyldecane . This is chemical 2,3,3-Trimethylpentane . This is chemical 2,3,3-Trimethylpentane . This is chemical 2,3,3-Trimethylpentane . This is chemical 2,3,4-Trimethylhexane . This is chemical 2,3,4-Trimethylhexane . This is chemical 2,3,4-Trimethylpentane . This is chemical 2,3,5-Trimethylhexane . This is chemical 2,3-Dimethyl-2-butanol . This is chemical 2,3-Dimethylhexane . This is chemical 2,3-Hexanedione . This is chemical 2,4-Dimethyl-3-pentanone . This is chemical 2,4-Dimethylheptane . This is chemical 2,4-Dimethylheptane . This is chemical 2,4-Dimethylheptane . This is chemical 2,4-Dimethylheptane . This is chemical 2,4-Dimethylheptane . This is chemical 2,4-Hexadiene, 2,5-dimethyl- . This is chemical 2,5-Dimethylfuran . This is chemical 2,6,11-Trimethyldodecane . This is chemical 2,6,11-Trimethyldodecane . This is chemical 2,6,6-Trimethyl octane . This is chemical 2,6-bis(1,1-dimethylethyl)-2,5-cyclohexadiene-1,4-dione . This is chemical 2,6-bis(1,1-dimethylethyl)-2,5-cyclohexadiene-1,4-dione . This is chemical 2,6-Dimethylheptane . This is chemical 2,6-Di-tert-butyl-4-methylphenol (Butylated hydroxytoluene) . This is chemical 2-amino-5-isopropyl-8-methyl-1-azulenecarbonitrile . This is chemical 2-amino-5-isopropyl-8-methyl-1-azulenecarbonitrile . This is chemical 2-amino-5-isopropyl-8-methyl-1-azulenecarbonitrile . This is chemical 2-Butanone . This is chemical 2-Butyl-1-octanol . This is chemical 2-Ethyl-1-hexanol . This is chemical 2-Ethyl-1-hexanol . This is chemical 2-Ethyl-4-methyl-1-pentanol . This is chemical 2-Ethyltoluene . This is chemical 2-Hexene (E) . This is chemical 2-Hexyl-1-octanol . This is chemical 2-Methyl-1-propene (Isobutene, Isobutylene) . This is chemical 2-Methyl-2-butene . This is chemical 2-Methylheptane . This is chemical 2-Methylhexane . This is chemical 2-Methylhexane . This is chemical 2-Methylnonane . This is chemical 2-Methyl octane . This is chemical 2-Methylpentane (Methylpentane) . This is chemical 2-Methylpentane (Methylpentane) . This is chemical 2-Methylpentane (Methylpentane) . This is chemical 2-Methylpentane (Methylpentane) . This is chemical 2-Methylpentane (Methylpentane) . This is chemical 2-Pentanol (sec-amyl alcohol) . This is chemical 2-Propyl-1-pentanol . This is chemical 3,3-Dimethylhexane . This is chemical 3,3-Dimethylpentane . This is chemical 3,3-Dimethylpentane . This is chemical 3,5-Dimethyloctane . This is chemical 3-Butyn-2-ol (a-Methylpropargyl alcohol) . This is chemical 3-Butyn-2-ol (a-Methylpropargyl alcohol) . This is chemical 3-Cyclohexene-1-methanol, .alpha.,.alpha.4-trimethyl- . This is chemical 3-Ethyl-3-methyl-2-pentanone . This is chemical 3-Ethylhexane . This is chemical 3-Ethylhexane . This is chemical 3-Ethylpentane . This is chemical 3-Heptanone . This is chemical 3-Hexanone, 2-methyl- . This is chemical 3-Methylhexane . This is chemical 3-Methylhexane . This is chemical 3-Methyl nonadecane . This is chemical 3-Methylnonane . This is chemical 3-Methyloctane . This is chemical 3-Methylpentane . This is chemical 3-Methylpentane . This is chemical 3-Methyl pyridine . This is chemical 3-Methyltridecane . This is chemical 3-Methyl undecane . This is chemical 4,7-Dimethyl-undecane . This is chemical 4,7-Dimethyl-undecane . This is chemical 4-Methyldecane . This is chemical 4-Methyldecane . This is chemical 4-Methyl dodecane . This is chemical 4-Methyloctane . This is chemical 4-Methyloctane . This is chemical 4-Methyloctane . This is chemical 4-Methyloctane . This is chemical 4-Methyloctane . This is chemical 4-Methyloctane . This is chemical 4-Methyloctane . This is chemical 4-Methyl pyridine . This is chemical 4-Penten-2-ol . This is chemical 5-(2-methylpropyl)nonane . This is chemical 5,5-Dimethyl-1,3-hexadiene . This is chemical 5H-Dibenz[b,f]azepine, 10,11-dihydro- . This is chemical 5-Methyldecane . This is chemical 5-Methylpentadecane . This is chemical 5-Methyl tridecane . This is chemical 6-Aminoundecane . This is chemical 6-Methyl-2-heptanone . This is chemical 6-Methyl pentadecane . This is chemical 7-Methylhexadecane . This is chemical 7-Methyltridecane . This is chemical Acetic acid . This is chemical Acetic acid . This is chemical Acetic acid . This is chemical Acetic acid . This is chemical Acetic acid . This is chemical Acetoin (3-Hydroxy-2-butanone) . This is chemical Acetoin (3-Hydroxy-2-butanone) . This is chemical Acetophenone (phenyl methyl ketone) . This is chemical alpha-Methylstyrene (1-methylethenyl-Benzene) . This is chemical alpha-Pinene . This is chemical Benzaldehyde . This is chemical Benzene, 1,1'-(1-butenylidene)bis- . This is chemical Benzene, 1,1'-(1-butenylidene)bis- . This is chemical Benzene, 1,2,3,4-tetramethyl- . This is chemical Benzene, 1,2,3,5-tetramethyl- . This is chemical Benzene, 1,2,4,5-tetramethyl- . This is chemical Benzene, 1,4-dichloro . This is chemical Benzene, 1-ethyl-3,5-dimethyl- . This is chemical Benzene, cyclobutyl- . This is chemical Benzoic acid, 4-ethoxy-, ethyl ester . This is chemical Benzoic acid, 4-ethoxy-, ethyl ester . This is chemical Beta-caryophyllene . This is chemical Bicyclo[4.2.0]octa-1,3,5-triene . This is chemical Butanal, 3-methyl- . This is chemical Butane (n-Butane) . This is chemical Butyl acetate (n-Butyl acetate) . This is chemical Butyric acid . This is chemical Butyric acid (Butanoic acid) . This is chemical Camphene . This is chemical Camphor . This is chemical Carbon monoxide . This is chemical Carbonyl sulfide . This is chemical cis 1,2-Dimethylcyclopropane . This is chemical Cyclohexane . This is chemical Cyclohexane, 1,3-dimethyl-, trans- . This is chemical Cyclohexane, 1,4-dimethyl . This is chemical Cyclohexane, 1-ethyl-4-methyl-, trans- . This is chemical Cyclohexanone . This is chemical Cyclohexene, 1-methyl-4-(1-methylethenyl)-, (S)- . This is chemical Cyclohexene, 1-methyl-5-(1-methylethenyl)-, (r)- . This is chemical Cyclopentene . This is chemical Cyclopropane, 1-methyl-2-pentyl- . This is chemical Cyclotetrasiloxane, octamethyl- . This is chemical Decanal . This is chemical Decane . This is chemical Decane . This is chemical Decane, 2,3,4-trimethyl- . This is chemical Decane, 4-methyl . This is chemical Diethyl phthalate . This is chemical Dimethyl ether . This is chemical Dimethyl sulfide . This is chemical Dimethyl sulfide . This is chemical Dimethyl trisulfide . This is chemical Diphenyl ether (Benzene, 1,1'-oxybis-) . This is chemical Diphenyl ether (Benzene, 1,1'-oxybis-) . This is chemical d-Limonene ((+)-Limonene) . This is chemical d-Limonene ((+)-Limonene) . This is chemical d-Limonene ((+)-Limonene) . This is chemical DL-Limonene . This is chemical DL-Limonene . This is chemical DL-Limonene . This is chemical DL-Limonene . This is chemical DL-Limonene . This is chemical Ethane . This is chemical Ethylbenzene . This is chemical Ethyleneimine . This is chemical Ethylidenecyclopropane . This is chemical Ethyl mercaptan (Ethanethiol) . This is chemical Furan, 2-pentyl- . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptanal . This is chemical Heptane, 2,3-dimethyl- . This is chemical Heptane, 2,4-dimethyl- . This is chemical Heptane, 2-bromo- . This is chemical Heptane, 3-ethyl-2methyl . This is chemical Heptane, 3-methyl- . This is chemical Heptane, 4-methyl- . This is chemical Heptane (n-Heptane) . This is chemical Heptene (n-Heptene) . This is chemical Hexanal . This is chemical Hexanal . This is chemical Hexanal . This is chemical Hexane (n-Hexane) . This is chemical Hexane (n-Hexane) . This is chemical Hexene (1-Hexene) . This is chemical Hexene (1-Hexene) . This is chemical Isobutane (2-methylpropane) . This is chemical Isobutane (2-methylpropane) . This is chemical Isobutyric acid . This is chemical Isocumene (n-Propylbenzene) . This is chemical Isocumene (n-Propylbenzene) . This is chemical Isopentane (2-methylbutane) . This is chemical Isopentane (2-methylbutane) . This is chemical Isopentane (2-methylbutane) . This is chemical Isoprene (2-Methyl-1,3-butadiene) . This is chemical Isopropyl alcohol (Isopropanol, 2-Propanol) . This is chemical Isopropyl alcohol (Isopropanol, 2-Propanol) . This is chemical Isopropyl alcohol (Isopropanol, 2-Propanol) . This is chemical Isopropyl alcohol (Isopropanol, 2-Propanol) . This is chemical Isopropyl alcohol (Isopropanol, 2-Propanol) . This is chemical Isoquinoline, 1,2,3,4-tetrahydro- . This is chemical Isovaleric acid . This is chemical Isovaleric acid (Butanoic acid, 3-methyl-) . This is chemical Methylcyclopentane . This is chemical Methylcyclopentane . This is chemical Methyl hydrazine . This is chemical Methyl mercaptan (Methanethiol) . This is chemical Methyl nicotinate . This is chemical Methyl p-anisate . This is chemical Methyl phenylacetate . This is chemical Methyl tert-butyl ether (MTBE) . This is chemical m-Xylene . This is chemical m-Xylene . This is chemical m-Xylene . This is chemical N-(2-dimethyl)-1-propanamine . This is chemical Naphthalene, 1-methyl- . This is chemical Nicotinic acid (Niacine) . This is chemical Nitric oxide . This is chemical Nitric oxide . This is chemical Nitric oxide . This is chemical Nonanal . This is chemical Nonanal . This is chemical Nonanal . This is chemical Nonanal . This is chemical Nonane . This is chemical Nonane . This is chemical Nonane, 1-iodo- . This is chemical Nonane, 4-ethyl-5-methyl- . This is chemical n-Propyl alcohol (1-Propanol, n-Propanol) . This is chemical Octanal . This is chemical Octanal . This is chemical Octane, 2,6-dimethyl- . This is chemical o-Phenylanisole . This is chemical Oxalic acid . This is chemical o-Xylene (ortho-Xylene, 2-Xylene) . This is chemical o-Xylene (ortho-Xylene, 2-Xylene) . This is chemical o-Xylene (ortho-Xylene, 2-Xylene) . This is chemical p-Cresol . This is chemical Pentadecane . This is chemical Pentanal (n-Pentanal) . This is chemical Pentane, 2-methyl- . This is chemical Pentane (n-Pentane) . This is chemical Phenol, 2,4-bis(1,1-dimethylethyl)- . This is chemical Phenyl acetic acid . This is chemical Propane, 2-methoxy-2-methyl- . This is chemical Propanedial (Malonaldehyde) . This is chemical Propanoic acid, 2-methyl-, 1-(1,1-dimethylethyl)-2-methyl-1,3-propanediyl ester . This is chemical Propionic acid . This is chemical Propylbenzene (Isocumene) . This is chemical p-Xylene (Para xylene) . This is chemical p-Xylene (Para xylene) . This is chemical p-Xylene (Para xylene) . This is chemical p-Xylene (Para xylene) . This is chemical p-Xylene (Para xylene) . This is chemical Salicylic Acid . This is chemical Styrene . This is chemical Styrene . This is chemical Styrene, alpha-methyl-, dimer . This is chemical Terpineol (4-trimethyl-3-cyclohexene-1-methanol) . This is chemical Tetrachloroethylene (Perchloroethylene, Tetrachloroethene) . This is chemical Tetradecane . This is chemical Tetradecane, 5-methyl- . This is chemical Toluene (Methylbenzene) . This is chemical Trans-anti-1-methyl-decahydronaphthalene (α-Methyldecalin) . This is chemical Trichloroethylene . This is chemical Trichlorofluoromethane . This is chemical Tridecane . This is chemical Tridecane . This is chemical Tridecane, 6-methyl- . This is chemical Undecane . This is chemical Undecane . This is chemical Undecane . This is chemical Undecane, 2-methyl- . This is chemical Undecane, 3,7-dimethyl- . This is chemical Undecane, 3,7-dimethyl- . This is chemical Undecane, 5,7-dimethyl- . This is chemical Urea . This is chemical Urea, tetramethyl- . This is chemical Valeric acid (Pentanoic acid) . This is chemical α Isomethyl ionone . This is chemical β-Pinene . This is chemical  ./n";

        CooccurrenceMatcher ow = new CooccurrenceMatcher();

      /*
      List<ResolvedNamedEntity> entities = ow.process(text);
      for (ResolvedNamedEntity ne : entities) {

         System.out.println(ne.getSurface());
         System.out.println(ne.getStart()+":"+ne.getEnd());
         System.out.println(ne.getType());
         System.out.println(ne.getNamedEntity().getConfidence());
         ChemicalStructure inchi = ne.getFirstChemicalStructure(FormatType.INCHI);
         if (inchi != null) {              System.out.println(inchi.getValue());         }

          System.out.println();
      }
      */



        //
        // Load properties. (home for GATE).
        //
        Properties pro = new Properties();
        try {
            pro.load(Files.newInputStream(new File(logger.getClass().getClassLoader().getResource("project.properties").toURI()).toPath()));
        } catch (URISyntaxException | IOException e1) {
            e1.printStackTrace();
        }
        //
        // Load GATE.
        //
        String gateHome = pro.getProperty("GATE_HOME");
        if (!Gate.isInitialised()) {
            logger.info("Initializing GATE...");

            // Set GATE HOME directory.
            Gate.setGateHome(new File(gateHome));
            // Set GATE HOME directory.
            Gate.setPluginsHome(new File(gateHome, "plugins"));
            logger.info(Gate.getPluginsHome());
            // Initialise GATE.
            Gate.init();
            logger.info("Initializing GATE... Done.");
        }

        //
        // Create gate document from whatizit xml string.
      /*/
      gate.Document sourceDocument = null;
      try {
         sourceDocument = (gate.Document) gate.Factory.createResource("gate.corpora.DocumentImpl", gate.Utils.featureMap(
               gate.Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, text, gate.Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME,
               "text/xml"));
      } catch (ResourceInstantiationException e1) {
         e1.printStackTrace();
      }
      //Utils.saveGateXml(sourceDocument, new File("temp1.xml"), false);
       */

        //
        // Create gate document from input text.
        //
        String fileName = "2100047.xml";
        // fileName = "/home/sadnana/work/cri/development/projects/foodweb/Nathalie/EcolAnn/OUTPUT/Bertucci.xml";
        gate.Document targetDocument = null;
        try {
            targetDocument = Factory.newDocument(new File(fileName).toURI().toURL());

            //targetDocument = Factory.newDocument(text);
        } catch (ResourceInstantiationException e1) {
            e1.printStackTrace();
        }

        //
        // Transfer annotations from whatizit output to original document.
        //
        //List annotationTypes = Arrays.asList(new String[]{"Oscar4_CM","Organism"});
        List<String> annotationTypes = Arrays.asList("Oscar4_CM","Negation");
        //List annotationTypes = Arrays.asList(new String[]{"Modifier"});
        assert targetDocument != null;
        ow.annotate(targetDocument,annotationTypes,"A_only",3);

        Utils.saveGateXml(targetDocument, new File("temp3.xml"), false);


        System.out.println("Finished.");
    }
}