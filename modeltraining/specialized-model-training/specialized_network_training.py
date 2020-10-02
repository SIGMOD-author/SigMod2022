'''Trains a convnet on the tiny imagenet dataset

0.4 val acc after 20 epochs with 100 classes
'''

# System
import numpy as np
import time
import os

np.random.seed(1337)  # for reproducibility

# Configure using which GPU
os.environ["CUDA_DEVICE_ORDER"]="PCI_BUS_ID";
os.environ["CUDA_VISIBLE_DEVICES"]="1";

# Keras
import keras
from keras import backend as K
from keras.layers.core import Dense, Dropout, Activation, Flatten
from keras.layers.convolutional import Conv2D, MaxPooling2D, ZeroPadding2D, AveragePooling2D
from keras.layers.normalization import BatchNormalization
from keras.utils import np_utils
from keras.applications import *
from keras.optimizers import SGD

# Configure Tensorflow backend
import tensorflow as tf
from keras.backend.tensorflow_backend import set_session
config = tf.ConfigProto()
config.gpu_options.allow_growth = True  # dynamically grow the memory used on the GPU
config.log_device_placement = False  # to log device placement (on which device the operation ran)
sess = tf.Session(config=config)
set_session(sess)  # set this TensorFlow session as the default session for Keras

# Custom
from load_images import load_images
from plotter import Plotter

# Params
# loss_functions = ['categorical_crossentropy','squared_hinge','hinge']
loss_functions = ['categorical_crossentropy']
num_classes = 5
num_img_per_class = 170
num_val_imgs = 100
batch_size = 10
nb_epoch = 30


def reset_weights(model):
    session = K.get_session()
    for layer in model.layers:
        if isinstance(layer, keras.engine.network.Network):
            reset_weights(layer)
            continue
        for v in layer.__dict__.values():
            if hasattr(v, 'initializer'):
                v.initializer.run(session=session)


for loss_function in loss_functions:
    # for num_classes in num_classes_arr: # num classes loop

    base_model = keras.applications.resnet.ResNet50(input_shape=(128, 128, 3), include_top=False, pooling='avg')
    #base_model = keras.applications.mobilenet_v2.MobileNetV2(input_shape=(128, 128, 3), include_top=False, pooling='avg')
    #base_model = keras.applications.inception_v3.InceptionV3(input_shape=(128, 128, 3), include_top=False, pooling='avg')
    x = base_model.output
    preds = Dense(num_classes, activation='softmax')(x)
    model = keras.Model(inputs=base_model.input, outputs=preds)
    for layer in model.layers[30:-30]:
        layer.trainable = False
    model.layers[-1].trainable = True
    print(model.layers[2].__dict__)

    print('===========================')
    print('Testing: ' + loss_function + ' with ' + str(num_classes) + ' classes')
    print('===========================')

    opt = SGD(lr=0.001)

    model.compile(loss=loss_function,
                  optimizer=opt,
                  metrics=['accuracy'])

    model.summary()

    # Load images
    path = './combination3'
    X_train, y_train, X_test, y_test = load_images(path, num_classes, num_img_per_class, num_val_imgs)

    print('X_train shape:', X_train.shape)
    print(X_train.shape[0], 'train samples')
    print(X_test.shape[0], 'test samples')

    num_samples = len(X_train)

    # input image dimensions
    img_rows, img_cols, num_channels = X_train.shape[1], X_train.shape[2], X_train.shape[3]

    X_train = X_train.astype('float32')
    X_test = X_test.astype('float32')
    X_train /= 255
    X_test /= 255

    # convert class vectors to binary class matrices
    Y_train = np_utils.to_categorical(y_train, num_classes)
    Y_test = np_utils.to_categorical(y_test, num_classes)

    fpath = 'loss-' + loss_function + '-' + str(num_classes)

    # datagen = ImageDataGenerator(
    #     featurewise_center=True,  # set input mean to 0 over the dataset
    #     samplewise_center=False,  # set each sample mean to 0
    #     featurewise_std_normalization=True,  # divide inputs by std of the dataset
    #     samplewise_std_normalization=False,  # divide each input by its std
    #     zca_whitening=False,  # apply ZCA whitening
    #     rotation_range=10.,  # randomly rotate images in the range (degrees, 0 to 180)
    #     width_shift_range=0.1,  # randomly shift images horizontally (fraction of total width)
    #     height_shift_range=0.1,  # randomly shift images vertically (fraction of total height)
    #     horizontal_flip=True,  # randomly flip images
    #     vertical_flip=False)  # randomly flip images
    # datagen.fit(X_train)
    # chpt = keras.callbacks.ModelCheckpoint('weights.{epoch:02d}-{val_loss:.2f}.h5', monitor='val_loss', verbose=0,
    #                                        save_best_only=False, save_weights_only=True, mode='auto', period=1)
    # model.fit_generator(datagen.flow(X_train, Y_train, batch_size=batch_size, shuffle=True,
    # save_to_dir='./datagen/', save_prefix='datagen-',save_format='png'), # To save the images created by the generator
    # samples_per_epoch=num_samples, nb_epoch=nb_epoch,
    # verbose=1, validation_data=(X_test,Y_test),
    # callbacks=[Plotter(show_regressions=False, save_to_filepath=fpath, show_plot_window=False)])




    #chpt = keras.callbacks.ModelCheckpoint('weights.{epoch:02d}-{val_loss:.2f}.h5', monitor='val_loss', verbose=0,
    #                                       save_best_only=False, save_weights_only=True, mode='auto', period=1)
    model.fit(X_train, Y_train, batch_size=batch_size, nb_epoch=nb_epoch,
              verbose=1, validation_data=(X_test, Y_test),
              callbacks=[Plotter(show_regressions=False, save_to_filepath=fpath, show_plot_window=False)])#, chpt])
    
    start = time.time()
    score = model.evaluate(X_test[0:20,:], Y_test[0:20,:], verbose=1)
    print('First exec time: ' + str(time.time()-start))
    start = time.time()
    score = model.evaluate(X_test[0:20,:], Y_test[0:20,:], verbose=1)
    print('Second exec time: ' + str(time.time()-start))

    print('Test score:', score[0])
    print('Test accuracy:', score[1])
    #
    # pathWeights = 'model' + loss_function + '{0:2d}'.format(time.time_ns()) + '.h5'
    # model.save_weights(pathWeights)
