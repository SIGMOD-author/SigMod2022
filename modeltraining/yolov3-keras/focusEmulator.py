import argparse
import re
import os

argparser = argparse.ArgumentParser(
    description='Focus emulator')

argparser.add_argument(
    '-s',
    '--search',
    help='object name to search')

argparser.add_argument(
    '-t',
    '--threshold',
    help='object threshold to search')

def _main_(args):
    query = args.search
    threshold = args.threshold
    threshold = float(threshold)
    root = '20streamsOutput'
    counter = 0
    for x in os.listdir(root):
        flag = -1
        index_dir = os.path.join(root, x)
        if not os.path.isdir(index_dir):
            continue
        index_path = os.path.join(index_dir, 'top3Index.txt')
        f = open(index_path, 'r')
        lines = f.readlines()
        for line in lines:
            s = line.split()
            rr = re.findall("[-+]?[.]?[\d]+(?:,\d\d\d)*[\.]?\d*(?:[eE][-+]?\d+)?", str(s))
            for index in [0,2,4]:
                if query == rr[index] and float(rr[index + 1]) > threshold:
                    counter = counter + 1
                    print(x)
                    flag = 1
                    break
            if flag == 1:
                break
        f.close()
    print(counter)

if __name__ == '__main__':
    args = argparser.parse_args()
    _main_(args)
