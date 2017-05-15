# -*- coding: utf-8 -*-
"""
Calculate & store n-gram document frequencies.

https://github.com/boudinfl/pke#document-frequency-counts
"""

from pke import compute_document_frequency
from string import punctuation

# path to the collection of documents
input_dir = '/home/bac003/sw/submissions/data/submissions/'

# path to the DF counts dictionary, saved as a gzip tab separated values
output_file = '/home/bac003/sw/submissions/data/docFreq.gz'

# compute df counts and store stem -> weight values
compute_document_frequency(input_dir=input_dir,
                           output_file=output_file,
                           format="raw",            # input files format
                           use_lemmas=False,    # do not use Stanford lemmas
                           stemmer="porter",            # use porter stemmer
                           stoplist=list(punctuation),            # stoplist
                           delimiter='\t',            # tab separated output
                           extension='txt',          # input files extension
                           n=5)              # compute n-grams up to 5-grams