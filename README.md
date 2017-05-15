# submissions

## Introduction

Automatic text summarization e.g. for submissions to parliamentary/senate committees.

These [submissions](http://www.aph.gov.au/Parliamentary_Business/Committees/House/Employment_Education_and_Training/Innovationandcreativity/Submissions) are suggested as suitable example data. Save this page in a file for the project to parse, then it can download the PDFs and extract text from them. Open the link, set `Page size` to 200 (so that all the submissions are displayed) then save the HTML to `data/submissions/` with: right-click > Save As > HTML Only.

The Maui training data under `data/train` was copied from Maui's `src/test/resources/data/automatic_tagging/train`.

This project requires a small local modification of Maui and a local build of it, as decribed in the following section.

## Maui

The [Kea paper](http://www.cs.waikato.ac.nz/~ml/publications/2005/chap_Witten-et-al_Windows.pdf) is also applicable to Maui, which is an update to Kea. This suggest that we need about 20 training examples with manually assigned key phrases. Maui resources: [code](https://github.com/zelandiya/maui); [wiki](https://code.google.com/archive/p/maui-indexer); [Google group](https://groups.google.com/forum/#!forum/kea-and-maui-support). This [item](https://groups.google.com/forum/#!topic/kea-and-maui-support/ybNFEsFvV1k) suggests downloading the DBpedia categories dataset to assign topics from wikipedia.

I changed maui `MauiTopicExtractor.topicsPerDocument` from package-private (no modifier) to `public` to avoid having to create code in the same package to be able to set it. Build and install under `~/.m2/repository` with:

    mvn -Dmaven.test.skip=true install

## Build and Run

    sbt one-jar
    java -jar target/scala-2.12/submissions_2.12-0.1-one-jar.jar --download --extract --train --summarize

to:
- parse the HTML file (see instructions in the Introduction), 
- download the linked PDF (as *.pdf files), 
- extract text from them (as *.txt files), 
- train the Kea model; then 
- use it to extract key phrases (as *.sum text files).

These files are all created under `data/submissions`.

## PKE

The [pke](https://github.com/boudinfl/pke) project provides Python 2 implementations of a range of key phrase extractor algorithms, including Kea. Install and run with:

    sudo apt-get install python-numpy python-scipy python-nltk python-networkx python-sklearn
    pip install git+https://github.com/boudinfl/pke.git
    python src/main/py/calcDocFreq.py
    python src/main/py/keyPhraseExtraction.py
    
to:
- save n-gram document frequencies built from our *.txt files to `data/docGreq.gz`
- save key phrases extracted with Kea and WINGNUS (using the above docFreqs but models trained using the SemEval-2010 dataset) to *.kea and *.wingus respectively.

## Named Entity Recognition (NER)
Convert text files to JSON format input suitable for [dataFusion-ner](https://github.com/NICTA/dataFusion/tree/master/dataFusion-ner) and perform NER:

    node txtToDocIn.js data/submissions/*.txt > source.json
    cd ../dataFusion
    source ./setenv.sh
    cd ../submisions
    java -Xmx7G -jar ../dataFusion/dataFusion-ner/target/scala-2.12/datafusion-ner_2.12-0.1-SNAPSHOT-one-jar.jar --cliNer < source.json > ner.json

Issue: we have one file for which at least one NER implementation appears to be taking forever:

    cat ner-server.log
    In progress for more than a minute: (path, minutes) = List((data/submissions/sub-043-part-1.txt,12.8754))

Do some experiments on just this file with each NER imple to establish which can't handle it.
Then cut down the input to try to figure out what it is that it can't handle.

Due to a dataFusion-ner bug (now fixed in no-graph-loop branch), we have "scrore" in our JSON instead or "score".

How should NER voting work?
- more than 1 impl with overlap in [offStr, offEnd]
- take shortest one with at least 3 tokens 
- only take PERSON and ORGANISATION
- use score? OpenNLP's is a probability 0.0801 - 0.999936 (1/5 are above 0.9692, 4/5 above 0.72160),
  but MITIE's is a score 0.037 - 6.99 (1/5 are above 1.08818, 4/5 above 0.41420)
- if only one impl has score in the top 1/5 accept it
- if only one impl is in the bottom 1/5 exclude it
