# Simple Python Backend for Android Apps

An example connection between an Android phone and a Python server including an example SVM classification.

I used my code to help students with their project work.

The example dataset, the image analysis as well as the classification methods have to be adapted according to your desired application.

You can easily create new classes by adding a new folder and images in the Dataset folder on the server.

You can train and test an SVM remotely on a specific dataset.

You can remotely add images to the dataset.

You can remotely analyze images.

# HowTo:

Install Android app

Start SpectroServer.py

Press q or Q to stop server

The terminal will display you different messages.

HowTo use:
1. Start Server on your PC in WiFi network (server and phone must be on the same WiFi network)
2. Start Application on your Phone
3. Type the IP and port noted down on the python terminal into the Android App
4. Have fun and test your own ML models and data!


Server Ports:
- 1417: On this port you can send commands to the server
- 1418: This port is desired for data (image) transmissions
- 1419: This is the port to which the server responds with classification results



Environmental Settings: Tested on Windows 10 with Python 3.6

Required Python Modules:

- socket
- threading
- keyboard
- shutil
- time
- os
- PIL
- scikit-learn (lower than version 0.23! [Tested with scikit-learn 0.22.1])
- cv2
- numpy (Tested with numpy 1.18.1)
