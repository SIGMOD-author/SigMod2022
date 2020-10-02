#!/bin/sh
for f in 20streamsOutput/*
do
    echo $f
    python3 resnet50Recognizer.py -i $f
done
