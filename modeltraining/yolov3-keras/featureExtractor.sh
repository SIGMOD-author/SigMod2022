#!/bin/sh
for f in 20streamsOutput/*
do
    echo $f
    python3 resnet50Extractor.py -i $f
done
python3 resnet50Extractor.py -i 20streamsOutput/
