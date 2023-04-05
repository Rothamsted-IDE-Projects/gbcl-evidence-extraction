package uk.ac.rothamsted.ide.gbcl;

import gate.*;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Objects;

public class Pipeline implements Serializable {
    private static final Logger logger = LogManager.getLogger(Pipeline.class);

    private SerialAnalyserController annieController;

    public Pipeline(String gateHome) throws GateException {
        if (!Gate.isInitialised()) {
            logger.info("Initializing GATE...");

            // GATE HOME directory.
            Gate.setGateHome(new File(gateHome));
            logger.info("GATE Home: " + Gate.getGateHome());
            // GATE plugins directory.
            Gate.setPluginsHome(new File(gateHome, "plugins"));
            logger.info("GATE Plugins: " + Gate.getPluginsHome());
            Gate.init();
            logger.info("Initializing GATE...Done.");
        }
    }

    public void init(boolean runDocumentResetter,
                     boolean runTokenizer,
                     boolean runANNIEGazetteer,
                     boolean runSentenceSplitter,
                     boolean runPosTagger,
                     boolean runANNIENETranducer,
                     boolean runCooccurrenceExtractor
    ) throws GateException, URISyntaxException, IOException {

        logger.info("Pipeline started....");

        // Load ANNIE plugin.
        File pluginsHome = Gate.getPluginsHome();
        Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "ANNIE").toURI().toURL());

        // Create a serial analyser controller to run ANNIE with.
        annieController = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController",
                Factory.newFeatureMap(), Factory.newFeatureMap(), "ANNIE_" + Gate.genSym());


        if (runDocumentResetter) {
            logger.info("Adding Document Reset PR adding...");
            FeatureMap prParams = Factory.newFeatureMap();
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", prParams);
            annieController.add(pr);
            logger.info("done.");
        }


        if (runTokenizer) {
            logger.info("Adding ANNIE English Tokenizer...");
            FeatureMap tokParams = Factory.newFeatureMap();
            ProcessingResource tokPr = (ProcessingResource) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser", tokParams);
            annieController.add(tokPr);
            logger.info("done");
        }

        if (runANNIEGazetteer) {
            logger.info("Adding ANNIE Gazetteer...");
            FeatureMap neAnnGazParams = Factory.newFeatureMap();
            ProcessingResource annieGazetteerPr = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", neAnnGazParams);
            annieController.add(annieGazetteerPr);
            logger.info("done.");
        }


        if (runSentenceSplitter) {
            logger.info("Adding ANNIE Sentence Splitter...");
            FeatureMap senParams = Factory.newFeatureMap();
            ProcessingResource senPr = (ProcessingResource) Factory.createResource("gate.creole.splitter.SentenceSplitter", senParams);
            annieController.add(senPr);
            logger.info("done.");
        }

        if (runPosTagger) {
            logger.info("Adding ANNIE POS Tagger...");
            FeatureMap posParams = Factory.newFeatureMap();
            ProcessingResource posPr = (ProcessingResource) Factory.createResource("gate.creole.POSTagger", posParams);
            annieController.add(posPr);
            logger.info("done.");
        }

        if (runANNIENETranducer) {
            logger.info("Adding ANNIE NE Transducer...");
            // Add gazetteer.
            FeatureMap neTransParams = Factory.newFeatureMap();
            ProcessingResource annieNETransducerPr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", neTransParams);
            annieController.add(annieNETransducerPr);
            logger.info("done.");
        }
        /*
        {
            logger.info("Adding NumberTagger...");
            Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "JAPE_Plus").toURI().toURL());
            FeatureMap numTagParams = Factory.newFeatureMap();
            Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "Tagger_Numbers").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.numbers.NumbersTagger", numTagParams);
            annieController.add(pr);
            logger.info("done.");
        }
         */
        {
            logger.info("Adding Custom gazetteer for crop loss evidence...");
            FeatureMap owParams = Factory.newFeatureMap();
            owParams.put("caseSensitive", false);
            owParams.put("encoding", "UTF-8");
            owParams.put("gazetteerFeatureSeparator", "@");
            owParams.put("listsURL", this.getClass().getClassLoader().getResource("gate/gazetteers/crop-loss-evidence-types.def").toURI().toURL());
            owParams.put("longestMatchOnly", true);
            ProcessingResource owPr = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", owParams);
            annieController.add(owPr);
            logger.info("done.");
        }
        {
            logger.info("Adding LookupTransducer...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/LookupTransducer.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("done.");
        }
        {
            logger.info("Adding CropNameUnifier transducer...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", Objects.requireNonNull(this.getClass().getClassLoader().getResource("gate/japerules/CropNameUnifier.jape")).toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("done.");
        }

        {
            logger.info("Adding PestNameUnifier transducer...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", Objects.requireNonNull(this.getClass().getClassLoader().getResource("gate/japerules/PestNameUnifier.jape")).toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("done.");
        }

        {
            logger.info("Adding NumberExtractor transducer...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", Objects.requireNonNull(this.getClass().getClassLoader().getResource("gate/japerules/NumberExtractor.jape")).toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("done.");
        }

        if (runCooccurrenceExtractor) {
            logger.info("Adding Cooccurrence transducer...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", Objects.requireNonNull(this.getClass().getClassLoader().getResource("gate/japerules/CooccurrenceMatcher.jape")).toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("done.");
        }


        /*
        {
            logger.info("YieldImpactDeterminer transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", Objects.requireNonNull(this.getClass().getClassLoader().getResource("gate/japerules/YieldImpactDeterminer.jape")).toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("YieldImpactDeterminer transducer added");
        }

        if (runCooccurrenceExtractor) {
            logger.info("Cooccurrence transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", Objects.requireNonNull(this.getClass().getClassLoader().getResource("gate/japerules/CooccurrenceMatcher.jape")).toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("Cooccurrence transducer added");
        }
         */


        /*
        {   //This works
            logger.info("OrganismTagger transducer adding...");
            Plugin plugin = new Plugin.Directory(new File(pluginsHome, "OrganismTagger").toURI().toURL());
            Gate.getCreoleRegister().registerPlugin(plugin);
            //Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "OrganismTagger").toURI().toURL());
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/OrganismTagger.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("OrganismTagger transducer added");
        }

        {
            logger.info("OrganismAggregator transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/OrganismAggregator.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("OrganismAggregator transducer added");
        }

    /*
        {
            // this works
            logger.info("organism tagger adding...");
            Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "JAPE_Plus").toURI().toURL());
            //Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "OrganismTagger_").toURI().toURL());
            FeatureMap params = Factory.newFeatureMap();
            // params.put("grammarURL",
            // this.getClass().getClassLoader().getResource("gate/japerules/WhatizitOscar3a5Main.jape"));
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/OrganismTagger.jape").toURI().toURL());
            // ///// params.put("grammarURL", new
            // File("resources/gate/japerules/Oscar4.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("organism tagger added");
        }
   */
        /*
        {
            logger.info("Number tagger adding...");
            Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "JAPE_Plus").toURI().toURL());
            FeatureMap numTagParams = Factory.newFeatureMap();
            Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "Tagger_Numbers").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.numbers.NumbersTagger", numTagParams);
            annieController.add(pr);
            logger.info("Number tagger added");
        }
        {
            logger.info("NumberFinalizer transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/NumberFinalizer.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("NumberFinalizer transducer added");
        }
        {
            logger.info("RelativeQuantifierExtraction transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/RelativeQuantifierExtraction.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("RelativeQuantifierExtraction transducer added");
        }
        {
            logger.info("Crop loss gazetteer adding...");
            FeatureMap owParams = Factory.newFeatureMap();
            owParams.put("caseSensitive", false);
            owParams.put("encoding", "UTF-8");
            owParams.put("gazetteerFeatureSeparator", "@");
            owParams.put("listsURL", this.getClass().getClassLoader().getResource("gate/gazetteers/croploss-evidence-gazetteer-list.def").toURI().toURL());
            owParams.put("longestMatchOnly", true);
            ProcessingResource owPr = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", owParams);
            annieController.add(owPr);
            logger.info("Crop loss gazetteer added");
        }
        {
            logger.info("Lookup transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/LookupTransducer.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("Lookup transducer added");
        }
        {
            logger.info("CostExtraction transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/CostExtraction.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("CostExtraction transducer added");
        }


        {
            logger.info("YieldUnitExtractor transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/YieldUnitExtractor.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("YieldUnitExtractor transducer added");
        }


        /*
        {
            logger.info("Ecology gazetteer adding...");
            FeatureMap owParams = Factory.newFeatureMap();
            owParams.put("caseSensitive", false);
            owParams.put("encoding", "UTF-8");
            owParams.put("gazetteerFeatureSeparator", "@");
            owParams.put("listsURL", this.getClass().getClassLoader().getResource("gate/gazetteers/ecology.def").toURI().toURL());
            owParams.put("longestMatchOnly", true);
            ProcessingResource owPr = (ProcessingResource) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer", owParams);
            annieController.add(owPr);
            logger.info("Ecology gazetteer added");
        }

        // A Biomedical Named Entity Recognizer : tagger for genes, proteins
        {
            logger.info("Abner tagger adding...");
            Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, "Tagger_Abner").toURI().toURL());
            ProcessingResource abnerPr = (ProcessingResource) Factory.createResource("gate.abner.AbnerTagger", Factory.newFeatureMap());
            annieController.add(abnerPr);
            logger.info("Abner tagger added");
        }


        {
            logger.info("Lookup transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/LookupTransducer.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("Lookup transducer added");
        }

        {
            logger.info("Modifier transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/modifier.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("Modifier transducer added");
        }

        {
            logger.info("object transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/object.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("object transducer added");
        }

        {
            logger.info("Abstract transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/titleabstract.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("Abstract transducer added");
        }

        {
            logger.info("combine transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/combine.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("combine transducer added");
        }

        if (runCooccurrenceExtractor) {
            logger.info("cooccurrence transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/CooccurrenceMatcher.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("cooccurrence transducer added");
        }
        */

        /*
        if (runCooccurrenceExtractor) {
            logger.info("Co-occurrence transducer adding...");
            FeatureMap params = Factory.newFeatureMap();
            params.put("grammarURL", this.getClass().getClassLoader().getResource("gate/japerules/CooccurrenceMatcher.jape").toURI().toURL());
            ProcessingResource pr = (ProcessingResource) Factory.createResource("gate.creole.ANNIETransducer", params);
            annieController.add(pr);
            logger.info("Co-occurrence transducer added");
        }

         */
    }

    public void setCorpus(Corpus corpus) {
        annieController.setCorpus(corpus);
    }

    public void execute(gate.Document doc) throws GateException {
        logger.info("Pipeline running...");
        gate.Corpus gateCorpus = Factory.newCorpus("corpus" + doc.getName());
        gateCorpus.add(doc);
        this.setCorpus(gateCorpus);
        annieController.execute();

        Factory.deleteResource(gateCorpus);

        logger.info("Pipeline complete");
    }

}