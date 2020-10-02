# Sys
import os
import numpy as np
from PIL import Image

# Custom
from val_load import get_annotations_map


def load_images(path, num_classes, num_imgs, num_val):
    # Load images

    print('Loading ' + str(num_classes) + ' classes')

    X_train = np.zeros([num_classes * num_imgs, 128, 128, 3], dtype='uint8')
    y_train = np.zeros([num_classes * num_imgs], dtype='uint8')

    trainPath = path + '/train'

    print('loading training images...')

    i = 0
    j = 0
    annotations = {}
    for sChild in os.listdir(trainPath):
        if sChild == '.DS_Store':
            continue
        sChildPath = os.path.join(trainPath, sChild)
        annotations[sChild] = j
        flag = 0
        for c in os.listdir(sChildPath):
            if flag >= num_imgs:
                break
            img = Image.open(os.path.join(sChildPath, c))
            img = img.resize((128, 128))
            X = np.array(img)
            if len(np.shape(X)) == 2:
                X_train[i, :, :, 0] = X
                X_train[i, :, :, 1] = X
                X_train[i, :, :, 2] = X
            else:
                X_train[i]=X
            y_train[i] = j
            i += 1
            flag += 1
        j += 1
        if j >= num_classes:
            break

    print('finished loading training images')

    X_test = np.zeros([num_classes * num_val, 128, 128, 3], dtype='uint8')
    y_test = np.zeros([num_classes * num_val], dtype='uint8')

    testPath = path + '/val'

    print('loading test images...')

    i = 0
    j = 0
    annotations = {}
    for sChild in os.listdir(testPath):
        if sChild == '.DS_Store':
            continue
        sChildPath = os.path.join(trainPath, sChild)
        annotations[sChild] = j
        flag = 0
        for c in os.listdir(sChildPath):
            if flag >= num_val:
                break
            img = Image.open(os.path.join(sChildPath, c))
            img = img.resize((128, 128))
            X = np.array(img)
            if len(np.shape(X)) == 2:
                X_test[i, :, :, 0] = X
                X_test[i, :, :, 1] = X
                X_test[i, :, :, 2] = X
            else:
                X_test[i]=X
            y_test[i] = j
            i += 1
            flag += 1
        j += 1
        if j >= num_classes:
            break

    print('finished loading test images')
    #val_annotations_map = get_annotations_map()

    #X_test = np.zeros([num_val, 128, 128, 3], dtype='uint8')
    #y_test = np.zeros([num_val], dtype='uint8')

    #print('loading test images...')

    #i = 0
    #testPath = path + '/val/images'
    #for sChild in os.listdir(testPath):
    #    if i >= num_val:
    #        break
    #    if val_annotations_map[sChild] in annotations.keys():
    #        sChildPath = os.path.join(testPath, sChild)
    #        img = Image.open(sChildPath)
    #        img = img.resize((128,128))
    #        X = np.array(img)
    #        if len(np.shape(X)) == 2:
    #            X_test[i, :, :, 0] = X
    #            X_test[i, :, :, 1] = X
    #            X_test[i, :, :, 2] = X
    #        else:
    #            X_test[i] = X
    #        y_test[i] = annotations[val_annotations_map[sChild]]
    #        i += 1
    #    else:
    #        pass
    #print('finished loading {} test images '.format(i))

    return X_train, y_train, X_test, y_test


if __name__ == "__main__":
    import matplotlib.pyplot as plt

    path = './tiny-imagenet-200'
    X_train, y_train, X_test, y_test = load_images(path, 3, 10, 20)

    fig1 = plt.figure()
    fig1.suptitle('Train data')
    ax1 = fig1.add_subplot(221)
    ax1.axis("off")
    ax1.imshow(X_train[0])
    print(y_train[0])
    ax2 = fig1.add_subplot(222)
    ax2.axis("off")
    ax2.imshow(X_train[9])
    print(y_train[9])
    ax3 = fig1.add_subplot(223)
    ax3.axis("off")
    ax3.imshow(X_train[15])
    print(y_train[15])
    ax4 = fig1.add_subplot(224)
    ax4.axis("off")
    ax4.imshow(X_train[29])
    print(y_train[29])

    plt.show()

    fig2 = plt.figure()
    fig2.suptitle('Test data')
    ax1 = fig2.add_subplot(221)
    ax1.axis("off")
    ax1.imshow(X_test[0])
    print(y_test[0])
    ax2 = fig2.add_subplot(222)
    ax2.axis("off")
    ax2.imshow(X_test[9])
    print(y_test[9])
    ax3 = fig2.add_subplot(223)
    ax3.axis("off")
    ax3.imshow(X_test[15])
    print(y_test[15])
    ax4 = fig2.add_subplot(224)
    ax4.axis("off")
    ax4.imshow(X_test[19])
    print(y_test[19])

    plt.show()
