# -*- coding: utf-8 -*-
"""
Created on Thu Nov 18 20:59:32 2021

@author: Schrapel
"""


#Imports modules
import socket
import threading
import keyboard
import shutil
import time
from os import listdir, path, mkdir, rename, getcwd
from os.path import isfile, join
from PIL import Image
from sklearn.externals import joblib
import cv2
#import csv
from sklearn import svm
from sklearn.preprocessing import normalize

import numpy as np
#import pandas as pd


def calcHistogram(image):

    '''
    resolution = 480, 640
    resizedImage = cv2.resize(image, resolution)
    '''
    # get data of color channels
    
    bins = 64
    histo_b = cv2.calcHist([image],[0],None,[bins],[0,255])
    histo_g = cv2.calcHist([image],[1],None,[bins],[0,255])
    histo_r = cv2.calcHist([image],[2],None,[bins],[0,255])
    
    # reshape data
    histo_out = []
    for i in range(bins):
        histo_out.append(histo_b[i][0])
    for i in range(bins):
        histo_out.append(histo_g[i][0])
    for i in range(bins):
        histo_out.append(histo_r[i][0])
    hist = np.zeros([1,len(histo_out)])
    hist[0]=histo_out
    return hist

####TODO
# def writeCSV(data,filename):
    # df = pd.DataFrame(data) 
    # df.to_csv(filename) 
    # # with open(filename, "w", newline="") as f:
    # #     writer = csv.writer(f)
    # #     writer.writerows(data)

# def readCSV(filename):
    # sequence = []
    # with open(filename, 'r') as csvFile:
        # reader = csv.reader(csvFile, 'excel')
        # for row in reader:
            # sequence.append(row)
    # return sequence

# to receive commands
class commandReceiverThread(threading.Thread):
    def __init__(self, inPort):
        threading.Thread.__init__(self)
        self.inPort = inPort
        self.ip = socket.gethostbyname(socket.gethostname()) 
        self.rsocket= socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.clientaddress=""
        self.rsocket.bind(('', inPort))
        self.rsocket.listen(99999999)

        self.command=""
        self.running = True
        self.datareceived=False
        
        # received command for SVM
        
        # Example received string
        # Testing:
        # "test:modelname"
        # "test:modelname:class" - not implemented
        
        # Add to Dataset:
        # "data:cat"
        
        # Train SVM
        # "train:modelname:linear,100" 
        # "train:cats,dogs:modelname:rbf,0.001"
        
    def run(self):
        print("Starting command Server @"+self.ip +":"+str(self.inPort)+"\n")
        while self.running:
            try:
                (clientsocket, self.clientaddress) = self.rsocket.accept()
                print("Received image identifier")
                datain = 1
                while datain:
                    datain = clientsocket.recv(999999999) #Gets incoming data
                    self.command = self.commandRecognizer(datain.decode('utf-8'))
                    if(len(self.command)>0):
                        break              
                self.datareceived=True
            except :
                print("identifier Socked closed!"+"\n")
        self.rsocket.close()
    
    def stop(self):
        self.running=False
        self.rsocket.close()
 

    def commandRecognizer(self,command):
        cnt=0
        cmd=[]
        while command.find(':')!=-1:
            cmd.append(command[0:command.find(':')])
            command=command[command.find(':')+1:]
        cmd.append(command[:-1])
        return cmd
    
    
    
    
# to receive images
class imageReceiverThread(threading.Thread):
    def __init__(self, inPort,tmp_path = "temp.jpg"):
        threading.Thread.__init__(self)
        self.inPort = inPort
        self.ip = socket.gethostbyname(socket.gethostname()) 
        self.rsocket= socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.clientaddress=""
        self.rsocket.bind(('', inPort))
        self.rsocket.listen(99999999)
        self.datareceived=False
        self.running = True
        self.busy = False
        self.tmp_path = tmp_path
        
    def run(self):
        print("Starting image Server @"+self.ip +":"+str(self.inPort)+"\n")
        while self.running:
            try:
                (clientsocket, self.clientaddress) = self.rsocket.accept()
                print("Received image request")
                #Opens File
                self.busy = True
                f = open(self.tmp_path, 'wb')
                datain = 1
                
                #Receives Image
                while datain:
                    datain = clientsocket.recv(999999999) #Gets incoming data
                    f.write(datain) #Writes data to file
                f.close()
                self.busy = False
                self.datareceived=True;
            except :
                print("image Socked closed!"+"\n")
        self.rsocket.close()
    
    def stop(self):
        self.running=False
        self.rsocket.close()
 

 
# handle commands
class commandHandlerThread(threading.Thread):
    def __init__(self, datasetPath,modelPath,commandReceiver,imageReceiver):
        threading.Thread.__init__(self)
        self.datasetPath = path.join(getcwd(),datasetPath)
        self.modelPath = path.join(getcwd(),modelPath)
        self.commandReceiver = commandReceiver
        self.imageReceiver = imageReceiver
        self.running = True
        self.busy = False
        self.hasSendData = False
        self.sendData=""
        self.client = ""
        self.imagetype=".jpg"
        self.command = []
        self.datatype=".csv"
        
        if not(path.exists(self.datasetPath)):
            mkdir(self.datasetPath)
        if not(path.exists(self.modelPath)):
            mkdir(self.modelPath)
        
        
    def run(self):
        print("Starting command listener\n")
        while self.running:
            if self.commandReceiver.datareceived:
                self.command = self.commandReceiver.command
                self.client = self.commandReceiver.clientaddress

            # received command
            if len(self.command)>0:
                
                # add to dataset
                if self.command[0] == "data":
                    if self.imageReceiver.datareceived:
                        self.busy = True
                        fpath = path.join(self.datasetPath, self.command[1])
                        
                        if not(path.exists(fpath)):
                            mkdir(fpath)
                        fcnt=0
                        filemv = path.join(self.datasetPath,path.join(self.command[1],self.command[1]+"-"+str(fcnt)+self.imagetype))
                        while path.exists(filemv):
                            fcnt+=1
                            filemv = path.join(self.datasetPath,path.join(self.command[1],self.command[1]+"-"+str(fcnt)+self.imagetype))
                        while self.imageReceiver.busy:
                            self.busy = True
                        rename(self.imageReceiver.tmp_path,filemv)
                        #shutil.move(self.command[1]+"-"+str(fcnt)+self.imagetype, filemv)
                        print("Added image to dataset: " + self.command[1]+"-"+str(fcnt)+self.imagetype )
                        self.imageReceiver.datareceived= False
                        self.commandReceiver.datareceived=False
                        self.command =[]
                        self.busy = False
                        
                # test image
                elif self.command[0] == "test":
                    if self.imageReceiver.datareceived:
                        print("Testing image...")
                        self.busy = True
                        while self.imageReceiver.busy:
                            self.busy = True

                        hist = calcHistogram(cv2.imread(self.imageReceiver.tmp_path))
                        hist = normalize(hist, norm='max', axis=1)
                        # hist=calcHistogram(im)
                        # hist=[int(a) for a in hist]

                        modelSavePath=path.join(self.modelPath,self.command[1]+".pkl")
                        if not(path.exists(modelSavePath)):
                            print("Test impossible: Unknown model!")
                            print(modelSavePath)
                            print("Please train a model first!")

                        else:
                            clf = joblib.load(modelSavePath)
                            prediction = clf.predict(hist)
                            print("Result: ")
                            print(prediction)
                            folders = [f for f in listdir(self.datasetPath) if not isfile(join(self.datasetPath, f))]
                            self.sendData=""+str(prediction)+":"+folders[prediction[0]]
                            self.hasSendData = True
                            
                        self.imageReceiver.datareceived=False
                        self.commandReceiver.datareceived=False
                        self.busy = False
                        
                # train SVM 
                elif self.command[0] == "train":
                    print("This server will be not reachable until the process is finished...")
                    print("Watch the console output to see the server status!")
                    self.busy = True
                    modelSavePath=path.join(self.modelPath,self.command[1]+".pkl")
                    
                    try:
                        params=self.command[1:]
                        clf=[]
                        print(params)
                        if path.exists(modelSavePath):
                            print("Model exists!")
                            clf = joblib.load(modelSavePath)
                            print("Model loaded and will be retrained!")
                            # if params!=[]:
                            #     print("SVM already exists: Parameters will be ignored!")
                        else:
                            #TODO: Add more parameters here and in Android!
                            clf = svm.SVC(kernel=params[1], C=int(params[2]), class_weight="balanced")
                    except:
                        print("Wrong usage! Please read the code!")
                        
                    print("Calculating and loading dataset...")
                    dataset=self.datasetLoader(self.datasetPath,self.imagetype,self.datatype)
                    print("Training SVM")
                    clf.fit(dataset[0],dataset[1])
                    joblib.dump(clf, modelSavePath)
                    
                    print("SVM Trained and stored!")
                    self.imageReceiver.datareceived= False
                    self.commandReceiver.datareceived=False
                    self.command =[]
                    self.busy = False
                    
        
    def datasetLoader(self,datapath,imagetype,datatype):
        datasetX=[]
        datasetY=[]
        folders = [f for f in listdir(datapath) if not isfile(join(datapath, f))]
        nclasses=len(folders)
        cntclass=0
        for folder in folders:
            classpath=join(datapath,folder)
            files = [f for f in listdir(classpath) if isfile(join(classpath, f))]
            for file in files:
                filepath=join(classpath,file)
                name,ending = path.splitext(file)
                # image has not been converted before
                hist=[]
                hist = calcHistogram(cv2.imread(filepath))
                hist = normalize(hist, norm='max', axis=1)
                
                # TODO: leave out files that do not belong to dataset
                # TODO: save feature vectors to prevent double calculations    
                        
                if type(hist) == np.ndarray:
                    if type(datasetX) == list:
                        datasetX = hist
                    else:
                        datasetX=np.append(datasetX,hist,axis=0)
                    datasetY.append(cntclass)
                    
            cntclass+=1
        
        dataset=[np.array(datasetX),np.array(datasetY)]
        print(dataset[0].shape)
        return dataset
        
    def stop(self):
        self.running=False


# to send commands
class commandTransmitterThread(threading.Thread):
    def __init__(self, outPort,commandHandler):
        threading.Thread.__init__(self)
        self.outPort = outPort
        self.commandHandler=commandHandler
        self.ip = ""
        self.rsocket= socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.receiveraddress=""
        self.running = True
        
        
    def run(self):
        print("Starting command transmitter at port: "+str(self.outPort)+"\n")
        while self.running:
            if self.commandHandler.hasSendData:
                self.rsocket= socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                #TODO: Send more messages like SVM training finished etc.
                try:
                    data = self.commandHandler.sendData
                    print(str(self.commandHandler.client[0]))
                    print(data)
                    self.rsocket.connect((str(self.commandHandler.client[0]),self.outPort));
                    self.rsocket.send(data.encode())
                    self.commandHandler.hasSendData = False
                except Exception as e:
                    print(e)
                    time.sleep(0.01)
                    
                self.rsocket.close()
                    
        print("client closed!")
    
    def stop(self):
        self.running=False
        self.rsocket.close()
        


startport = 1417
datasetPath = "Dataset"
modelPath = "Model"

print("Dataset will be stored in subdirectory: "+datasetPath)
print("Models will be stored in subdirectory: "+modelPath+"\n")
print("In the Android App:")
print("Enter the IP: "+socket.gethostbyname(socket.gethostname()))
print("Enter the port: "+str(startport)+"\n")
print("Starting Threads and Servers...")


# For receiving commands
cmdThread = commandReceiverThread(startport)

# For receiving images
imgThread = imageReceiverThread(startport+1)

# For handling actions on incoming commands
cmdHandleThread = commandHandlerThread(datasetPath,modelPath,cmdThread,imgThread)

# For sending data back
cliThread = commandTransmitterThread(startport+2,cmdHandleThread)


cmdThread.start()
imgThread.start()
cmdHandleThread.start()
cliThread.start()
print("\nSetup complete!")
print("Press Q to stop server")
while True:  # making a loop
    #if receiver.datareceived:
        
    
    try:  # used try so that if user pressed other than the given key error will not be shown
        if keyboard.is_pressed('q'):  # if key 'q' is pressed 
            break  # finishing the loop
        if keyboard.is_pressed('Q'):  # if key 'q' is pressed 
            break  # finishing the loop
    except:
        break  # if user pressed a key other than the given key the loop will break

print("\nStopping Threads and Servers...\n")
cmdThread.stop()
imgThread.stop()
cmdHandleThread.stop()
cliThread.stop()
time.sleep(0.5)
print("\nGood Bye!\n")