import socket
import cv2
import numpy
import json
import tensorflow as tf
import os
import sys
import time
from darkflow.cli import cliHandler

_MSG_BYTE_ARRAY_HEADER = b'\x05ur\x00\x00\x02[B\xac\xf3\x17\xf8\x06\x08T\xe0\x02\x00\x00xp\x06h'

def classify(fileName, decimg):
    cliHandler(sys.argv)

    openFile = open(fileName)
    try:
        data = json.load(openFile)
    except:
        openFile = open("./sample_img/out/none.json")
        data = json.load(openFile)
    retStr = ''
    for i in range(len(data)):
        if(data[i]["confidence"] >= 0.6):
            retStr += data[i]["label"] + " "
            topLeft_x = data[i]["topleft"]["x"]
            topLeft_y = data[i]["topleft"]["y"]
            bottomRight_x = data[i]["bottomright"]["x"]
            bottomRight_y = data[i]["bottomright"]["y"]
            cv2.rectangle(decimg, (topLeft_x, topLeft_y), (bottomRight_x, bottomRight_y), (0, 0, 255), 7)

    return retStr, decimg

#socket 수신 버퍼를 읽어서 반환하는 함수
def recvall(sock, count):
    buf = b''
    while count:
        newbuf = sock.recv(count)
        if not newbuf: return None
        buf += newbuf
        count -= len(newbuf)
    return buf


#수신에 사용될 내 ip와 내 port번호
TCP_IP = '192.168.0.17'
TCP_PORT = 50000

#TCP소켓 열고 수신 대기
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind((TCP_IP, TCP_PORT))
s.listen(True)
i = 0
while True:
    print("listening")
    conn, addr = s.accept()
    print("Accept")

    #String형의 이미지를 수신받아서 이미지로 변환 하고 화면에 출력
    # java  serialization magic number
    conn.recv(1024)
    conn.recv(2)

    length = conn.recv(6)
#    print(int(length))

    # magic number
    conn.recv(len(_MSG_BYTE_ARRAY_HEADER))

    # stringData = conn.recv(3000)
    stringData = recvall(conn, int(length))
#    print('hello', len(_MSG_BYTE_ARRAY_HEADER))
#    print(len(stringData))

    # stringData = stringData[1:]
    # print(stringData)

    data = numpy.fromstring(stringData, numpy.uint8)
    #
    decimg = cv2.imdecode(data, 1)
    cv2.imwrite('./sample_img/0.png', decimg)

    i = i + 1

    if stringData:
        fileName = "./sample_img/out/0.json"
        #cv2.imshow("a", decimg);
        sendStr, decimg = classify(fileName, decimg)
        #cv2.imshow("a", decimg)

        fileName = "./sample_img/outimg/0.png"
        cv2.imwrite(fileName, decimg)
        sendImg = open(fileName,"rb")

        sendStr = sendStr + "\r\n"
        conn.send(sendStr.encode('utf-8'))
        t = os.path.getsize(fileName)
        retDataSize = str(t) + "\r\n"
        a = conn.send(retDataSize.encode('utf-8'))
        print(a)
        time.sleep(1)
        chk = 0
        while(True):
#        conn.send(stringData)
            data = sendImg.readline()
            if(data == b''):
                break
            a = conn.send(data)

        print("send!")
        os.remove("./sample_img/0.png");
        os.remove("./sample_img/out/0.json");

        # if(conn.send(img) < 0):
        #     print("error!")
        # img
