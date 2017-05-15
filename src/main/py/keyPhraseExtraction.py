# -*- coding: utf-8 -*-
"""
Key phrase extraction.
Using model trained on SemEval-2010 data set.

https://github.com/boudinfl/pke#minimal-example
"""

import pke
import os

def KeyPhraseExtract(df_counts, extractor, ofile):
    # load the content of the document and perform French stemming (instead of
    # Porter stemmer)
    extractor.read_document(format='raw')
    
    # keyphrase candidate selection
    extractor.candidate_selection()
    
    # candidate weighting
    # WINGNUS & Kea use trained models (supervised learning)
    # extractor.candidate_weighting() # runs feature_extraction() and classify_candidates()
    extractor.feature_extraction(df=df_counts)
    
    # SupervisedLoadFile unpickles the model each time here, to unpickle once for mutiple docs we'd have to change code 
    extractor.classify_candidates()
    
    # the other 4 methods are unsupervised
    #  extractor.candidate_weighting(df=df_counts)
    
    # N-best selection, keyphrases contains the 10 highest scored candidates as
    # (keyphrase, score) tuples
    keyphrases = extractor.get_n_best(n=10, redundancy_removal=True)
    txt = '\n'.join([x[0] + '\t' + str(x[1]) for x in keyphrases]) + '\n'
    
    of = open(ofile, 'w')
    of.write(txt)
    of.close()
    
def KeyPhraseExtractWingus(df_counts, ifile):
    KeyPhraseExtract(df_counts, pke.WINGNUS(input_file=ifile), ifile.replace('.txt', '.wingus'))

def KeyPhraseExtractKea(df_counts, ifile):
    KeyPhraseExtract(df_counts, pke.Kea(input_file=ifile), ifile.replace('.txt', '.kea'))

df_counts = pke.load_document_frequency_file(input_file='/home/bac003/sw/submissions/data/docFreq.gz') 
   
dataDir='/home/bac003/sw/submissions/data/submissions'
for i in [ os.path.join(dataDir, f) for f in os.listdir(dataDir) if f.endswith('.txt') ]:
    print i + '...' 
    KeyPhraseExtractWingus(df_counts, i)
    KeyPhraseExtractKea(df_counts, i)

print 'done'

    