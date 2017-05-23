# submissions

## Introduction

Automatic text summarization e.g. for submissions to parliamentary/senate committees.

These [submissions](http://www.aph.gov.au/Parliamentary_Business/Committees/House/Employment_Education_and_Training/Innovationandcreativity/Submissions) are suggested as suitable example data. This page was saved as `data/submissions/Submissions – Parliament of Australia.html` by: setting `Page size` to 200 (so that all the submissions are displayed) then saving the HTML with: `right-click` > `Save As` > `HTML Only`.

Text is extracted from PDF documents using [PDFBox](https://pdfbox.apache.org/), which appears to handle multi-column text (whereas an experiment with [iText](http://itextpdf.com/) did not).

Two different key phrase extraction packages are used Maui and pke. 

## Maui

The Maui training data under `data/train` was copied from Maui's `src/test/resources/data/automatic_tagging/train`. This consists of only 4 biology papers and should be (but has not yet been) replaced with training data for the actual task at hand.

The [Kea paper](http://www.cs.waikato.ac.nz/~ml/publications/2005/chap_Witten-et-al_Windows.pdf) is also applicable to Maui. This suggest that we need about 20 training examples with manually assigned key phrases. Maui resources: [code](https://github.com/zelandiya/maui); [wiki](https://code.google.com/archive/p/maui-indexer); [Google group](https://groups.google.com/forum/#!forum/kea-and-maui-support). Maui's unit tests provide helpful usage examples. This  [topic](https://groups.google.com/forum/#!topic/kea-and-maui-support/ybNFEsFvV1k) in the Google Group suggests downloading the DBpedia categories dataset to assign topics from wikipedia.

This project requires a small local modification of Maui and a local build of it, as described here:

Change Maui's `MauiTopicExtractor.topicsPerDocument` from package-private (no modifier) to `public` to avoid having to create code in the same package to be able to set it. Build and install under `~/.m2/repository` with:

    mvn -Dmaven.test.skip=true install

## PKE

[pke](https://github.com/boudinfl/pke) provides Python 2 implementations of a range of key phrase extractor algorithms, including Kea. Install with:

    sudo apt-get install python-numpy python-scipy python-nltk python-networkx python-sklearn
    pip install git+https://github.com/boudinfl/pke.git

The pke key phrases use stemmed words (endings are munged so as not to distinguish between them). They are still comprehensible but this may not be acceptable for some applications. By default pke uses included models trained on the SemEval-2010 automatic key phrase extraction dataset (of scientific papers). This will be more comprehensive than the Maui example training dataset and so may produce better results, however the penalty of using a model trained on scientific papers with submissions data (both for Maui and pke) is unknown.

## Build and Run

    sbt one-jar
    java -jar target/scala-2.12/submissions_2.12-0.1-one-jar.jar --download --extract --train --summarize

to:
- parse the HTML file (see instructions in the Introduction), 
- download the linked PDF (as *.pdf files), 
- extract text from them (as *.txt files), 
- train the Maui model; then 
- use it to extract key phrases (as *.sum text files).

These files are all created under `data/submissions`.

Run pke on the above *.txt files with:

    python src/main/py/calcDocFreq.py
    python src/main/py/keyPhraseExtraction.py
    
to:
- save n-gram document frequencies built from our *.txt files to `data/docFreq.gz`
- save key phrases extracted with the Kea and WINGNUS algorithms (using the above docFreqs) to `data/submissions/*.kea` and `data/submissions/*.wingus` respectively.

## Named Entity Recognition (NER)
Build the dataFusion project:

    cd ~/sw
    git clone git@github.com:NICTA/dataFusion.git
    cd dataFusion
    source ./setenv.sh  # set env for MITIE
    sbt one-jar         # build
    cd ../submissions
    
Convert text files to JSON suitable as input to [dataFusion-ner](https://github.com/NICTA/dataFusion/tree/master/dataFusion-ner) and perform NER:

    node txtToDocIn.js data/submissions/*.txt > source.json
    java -Xmx7G -jar ../dataFusion/dataFusion-ner/target/scala-2.12/datafusion-ner_2.12-0.1-SNAPSHOT-one-jar.jar --cliNer < source.json > ner.json

Inspection of the NER output may suggest some text pre-processing that could improve the results.

Cleanup NER results, calculate a network of NER proximity (similar to dataFusion project - outputting results to `node.json` and `edge.json`), run a graph-service on this data, serve the UI with a python web server:

    # cleanup NER results
    java -jar target/scala-2.12/submissions_2.12-0.1-one-jar.jar --nerFilter < ner.json > nerFiltered.json
    # calculate a network of NER proximity, outputting results to `node.json` and `edge.json`
    node nersToNodeEdge.js < nerFiltered.json
    # run a JSON web service on data from `node.json` and `edge.json`
    java -jar ../dataFusion/dataFusion-graph/target/scala-2.12/datafusion-graph_2.12-0.1-SNAPSHOT-one-jar.jar &
    # run a web server for the UI
    ( cd ui; python3 -m http.server ) &

Access the UI at: http://localhost:8000/

## Term vector Visualization
See:
- https://github.com/NICTA/termvect-viz
- https://github.com/NICTA/termvect-viz/blob/master/clusteval/3NewsGroups.pdf

Build the termvect-viz project:

    cd ~/sw
    git clone git@github.com:NICTA/termvect-viz.git
    cd termvect-viz/termvect
    # build Java code
    mvn
    
    # build C++ code:
    cd ../bh_tsne
    g++ quadtree.cpp tsne.cpp -o bh_tsne -O3 -lblas
    
    
Run it on our *.txt files with parameters suggested by the 3NewsGroup.pdf report (linked above):

    cd ~/sw/submissions
    java -jar ../termvect-viz/termvect/target/termvect-0.1-SNAPSHOT.one-jar.jar --docDir data/submissions --docSuffix '.txt' --indexDir data/index --minDocFreq 2 --maxDocFreq 100 --excludeTerm '^[0-9.,-]+$' --tfCalc 3 --idfCalc 0 --terms terms --corpus corpus --paths paths
    # see sw/termvect/src/main/scripts/run.sh
    ../termvect-viz/bh_tsne/bh_tsne 2.5 6.0 500 < terms > bh.out
    sed -e '1,2d' -e 's/ $//' -e 's/ /\t/' bh.out > bh.matrix
    ( echo -e "x\ty\tpath"; awk -F/ '{print $3}' paths | paste bh.matrix - ) > ui/bh.matrix.withPath
    
Visualize this data in UI at: http://localhost:8000/ using `Graph type: T-SNE`.
    
