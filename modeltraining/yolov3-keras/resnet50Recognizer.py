import argparse
import os
import numpy as np
import sys
from keras.models import Model, load_model
from tensorflow.keras.applications.resnet50 import ResNet50
from tensorflow.keras.preprocessing import image
from tensorflow.keras.applications.resnet50 import preprocess_input, decode_predictions
import struct
import scipy.io as sio
from keras.preprocessing.image import ImageDataGenerator
# Set up the GPU environment
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
np.set_printoptions(threshold=sys.maxsize)
os.environ["CUDA_DEVICE_ORDER"]="PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"]="0"
config = tf.ConfigProto()
config.log_device_placement = False
sess = tf.Session(config=config)
set_session(sess)  # set this TensorFlow session as the default session for Keras


argparser = argparse.ArgumentParser(
    description='Use resnet50 to extract features')

argparser.add_argument(
    '-i',
    '--input',
    help='path to input files')

def _main_(args):
    input_path_dir = args.input

    # load the model
    # here is the model without softmax layer
    model = ResNet50(weights='imagenet');
    model.summary();
    
    # initialize image data generator
    dataGen = ImageDataGenerator(preprocessing_function=preprocess_input)

    # load image from input path dir
    testSet = dataGen.flow_from_directory(input_path_dir, batch_size=256, target_size=(224,224), shuffle=False, class_mode='sparse')

    labels = testSet.labels

    # make prediction on testSet
    res = model.predict(testSet, verbose=1)
    
    print(res.shape)

    preRes = decode_predictions(res, top=3)

    f = open(os.path.join(input_path_dir, "top3Index.txt"), 'w')

    for i in range(1, len(preRes)):
        print(preRes[i], labels[i], file=f)
    f.close()
    

if __name__ == '__main__':
    args = argparser.parse_args()
    _main_(args)

