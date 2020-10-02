import argparse
import os
import numpy as np
from keras.layers import GlobalAveragePooling2D, Dense, Conv2D, Input, BatchNormalization, LeakyReLU, ZeroPadding2D, UpSampling2D, Activation
from keras.layers.merge import add, concatenate
from keras.models import Model, load_model
import struct
import cv2
import sys
import scipy.io as sio
from keras.regularizers import l2
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
    description='Use darknet53 to extract features')

argparser.add_argument(
    '-i',
    '--input',
    help='path to input files')

def _main_(args):
    input_path_dir = args.input

    # load the model
    # here is the model without softmax layer
    darknet53 = load_model('models/darknet53.h5')
    
    # initialize image data generator
    dataGen = ImageDataGenerator()

    # load image from input path dir
    testSet = dataGen.flow_from_directory(input_path_dir, batch_size=256, shuffle=False, class_mode='sparse')

    labels = testSet.labels

    # make prediction on testSet
    res = darknet53.predict_generator(testSet, verbose=1)
    
    # save a mat file
    sio.savemat(os.path.join(input_path_dir, 'features.mat'), {'features': res, 'labels': labels})

if __name__ == '__main__':
    args = argparser.parse_args()
    _main_(args)

