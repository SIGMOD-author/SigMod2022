import cv2
import numpy as np

cap = cv2.VideoCapture("./data/test.mkv")
filename = "./data/test/all/"
count = 1
while(cap.isOpened()):
    ret, frame = cap.read()
    # select at 6 fps
    if ret:
        if count % 5 == 0:
            cv2.imwrite(filename + str(count) + ".jpg", frame)
            print(count)
        count = count + 1
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
    else:
        break
cap.release()
cv2.destroyAllWindows()

