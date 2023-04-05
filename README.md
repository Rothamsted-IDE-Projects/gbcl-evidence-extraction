# gbcl-evidence-extraction  
This repository contains a text-mining pipeline using the [GATE](https://gate.ac.uk) 
NLP tool as part of the project titled **A pipeline for extracting evidence of crop 
loss from scientific reports**. The aim of the pipeline is to extract information from preselected documents in the context of Global Burden of Crop Loss (GBCL) that describes crop loss from insect infiltration. The pipeline achieves this goal by mining sentences from the documents which are chosen based on the annotations submitted by the user. The results are saved in a single spreadsheet in TSV format.

## Limitations
- Pipeline has difficulty to extract evidence if source text contain single and double quote. 

## Main components
- GATE NLP installation **v8.X only**
- `Main.java` contains command line options for I/O
- `Pipeline.java` contains GATE plugins, gazetteers, JAPE rules
- `CooccerenceMatcher.java` depends on the `CooccerenceMatcher.jape`
rule for selecting annotations simultaneously occurring in sentences 
of length 1/2/3
- `AnnotationExporter.java` takes array of annotations from the 
`main(...)` method to export annotated results as a TSV file
- `gazetteers` directory contains proprietary lists of pests 
(&copy;[CABI](https://www.cabi.org/)) including other lists
- `japerules` directory contains all necessary jape rules for 
producing the final annotations

## Settings

1. [Download](https://gate.ac.uk/download/) and install GATE
2. Set path to `GATE_HOME` in `src/main/resources/project.properties`
3. Open GATE GUI and `Load ANNIE System with defaults`. 
*File->Ready Made Applications->ANNIE*


> Before using any plugin, create `plugins` as
`[gate-installation-directory]/plugins` and load

> Note: Use [nimbletext](https://nimbletext.com/Live/1498428636/) to format gazetteer with `@id=`
## How to run

### Using the `Main` method
- in Windows, set options `pipe` and arguments as follows:

`--pipe -i H:\[CORPUS_DIR] -x H:\[ANNOTATED_XML_DIR]`
- in Ubuntu

`--pipe -i [CORPUS_DIR] -x [ANNOTATED_XML_DIR]`

### Using the exec-maven-plugin
1. Set the argument values directly inside the plugin inside `<configuration>`

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <configuration>
        <mainClass>uk.ac.rothamsted.ide.gbcl.Main</mainClass>
        <arguments>
            <argument>--pipe</argument>
            <argument>-i H:\[CORPUS_DIR]</argument>
            <argument>-x H:\[ANNOTATED_XML_DIR]</argument>            
        </arguments>
    </configuration>
</plugin>
```
or

2. Use `mvn exec:java` from terminal

`mvn clean compile package`

For Windows
```shell
mvn exec:java -D'exec.mainClass'='uk.ac.rothamsted.ide.gbcl.Main' \
    -D'exec.args'="--pipe -i H:\[CORPUS_DIR] -x H:\[ANNOTATED_XML_DIR]"
```

For Linux
```shell
 mvn exec:java -Dexec.mainClass=uk.ac.rothamsted.ide.gbcl.Main \
     -Dexec.args="--pipe -i [CORPUS_DIR] -x [ANNOTATED_XML_DIR]"
```