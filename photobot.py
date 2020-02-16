""" Get serial of connected YubiKey. """
import sys
import yubico
import socket # for socket 
import threading
import sys  
from ev3dev2.sound import Sound
from ev3dev2.led import Leds
import logging

#from ev3dev2.motor import OUTPUT_A, OUTPUT_D, MoveTank, SpeedPercent, MediumMotor

expected = \
    b'\xe8\xfd\x17\x6c\x83\xad\xeb\x77\x6f\x75' + \
    b'\x94\x89\xe3\x6b\xac\xaf\x66\x4b\x1e\xb2'

# turn on YubiKey debug if -v is given as an argument
debug = False
if len(sys.argv) > 1:
    debug = (sys.argv[1] == '-v')

BUFFER_SIZE = 1024
# set logger to display on both EV3 Brick and console
logging.basicConfig(level=logging.INFO, stream=sys.stdout, format='%(message)s')
logging.getLogger().addHandler(logging.StreamHandler(sys.stderr))
logger = logging.getLogger(__name__)

#drive = MoveTank(OUTPUT_A, OUTPUT_D)

# next create a socket object 
s = socket.socket()          
print ("Socket successfully created")
  
# reserve a port on your computer in our 
# case it is 12345 but it can be anything 
port = 12345
  
# Next bind to the port 
# we have not typed any ip in the ip field 
# instead we have inputted an empty string 
# this makes the server listen to requests  
# coming from other computers on the network 
s.bind(('', port))         
print ("socket binded to " + str(port))
  
# put the socket into listening mode 
s.listen(5)      
print ("socket is listening")       

# move right side
def _robotic_right():    
    global isRoboticActive
    if isRoboticActive == False:
        isRoboticActive = True

        isRoboticActive = False

def _robotic_left():    
    global isRoboticActive
    if isRoboticActive == False:
        isRoboticActive = True

        isRoboticActive = False

def _robotic_forward():    
    global isRoboticActive
    if isRoboticActive == False:
        isRoboticActive = True

        isRoboticActive = False

def _robotic_backward():    
    global isRoboticActive
    if isRoboticActive == False:
        isRoboticActive = True

        isRoboticActive = False

# a forever loop until we interrupt it or  
# an error occurs 
while True: 
  
   # Establish connection with client. 
   c, addr = s.accept()      
   #print('Got connection from', addr )

   # send a thank you message to the client.  
   #c.send(b'Thank you for connecting') 
   data = c.recv(BUFFER_SIZE)
   serialnumber = ""
   if 'yubico' in data.decode("utf-8"):
       # Look for and initialize the YubiKey
        try:
            YK = yubico.find_yubikey(debug=debug)
            print("Version : %s " % YK.version())
            print("Serial  : %i" % YK.serial())
            print("")
            serialnumber = str(YK.serial())
            # Do challenge-response
            if '12236151' in serialnumber or '12236386' in serialnumber:
                secret = b'Sample #2'.ljust(64, b'\0')
                print("Sending challenge : %s\n" % repr(secret))
                response = YK.challenge_response(secret, slot=2)
        except yubico.yubico_exception.YubicoError as inst:
            print("ERROR: %s" % inst.reason)
            c.send(b'YubiKey is required to use this app') 


        # Check if the response matched the expected one
        if '12236151' in serialnumber or '12236386' in serialnumber:
            c.send(b'Your YubiKey does not have right to access this machine') 
        elif response == expected:
            print("Response :\n%s\n" % yubico.yubico_util.hexdump(response))

            # Workaround for http://bugs.python.org/issue24596
            del YK
            print("OK! Response matches the NIST PUB 198 A.2 expected response.")
            c.send(b'You are using the right YubiKey!') 
        else:
            print("Response :\n%s\n" % yubico.yubico_util.hexdump(response))

            # Workaround for http://bugs.python.org/issue24596
            del YK
            print(b'"ERROR! Response does NOT match the NIST PUB 198 A.2 expected response."')
            c.send(b'You are using the wrong YubiKey, please use the right') 
   if not data: 
      break
   print("received data:", data)
   #conn.send(data)   
   # Close the connection with the client 
   c.close() 