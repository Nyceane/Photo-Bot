#!/usr/bin/env python
"""
Test challenge-response, assumes a NIST PUB 198 A.2
20 bytes test vector in Slot 2 (variable input)
"""

import sys
import yubico

#expected = \
#    b'\x71\x0c\xbf\x2e\x12\x84\x90\x65\x0e\xa9' + \
#    b'\x2a\xfa\x34\x21\x10\x17\x9e\xd5\x6b\xf5'

expected = \
    b'\xe8\xfd\x17\x6c\x83\xad\xeb\x77\x6f\x75' + \
    b'\x94\x89\xe3\x6b\xac\xaf\x66\x4b\x1e\xb2'

# turn on YubiKey debug if -v is given as an argument
debug = False
if len(sys.argv) > 1:
    debug = (sys.argv[1] == '-v')

# Look for and initialize the YubiKey
try:
    YK = yubico.find_yubikey(debug=debug)
    print("Version : %s " % YK.version())
    print("Serial  : %i" % YK.serial())
    print("")

    # Do challenge-response
    secret = b'Sample #2'.ljust(64, b'\0')
    print("Sending challenge : %s\n" % repr(secret))

    response = YK.challenge_response(secret, slot=2)
except yubico.yubico_exception.YubicoError as inst:
    print("ERROR: %s" % inst.reason)
    sys.exit(1)

print("Response :\n%s\n" % yubico.yubico_util.hexdump(response))

# Workaround for http://bugs.python.org/issue24596
del YK

# Check if the response matched the expected one
if response == expected:
    print("OK! Response matches the NIST PUB 198 A.2 expected response.")
    sys.exit(0)
else:
    print("ERROR! Response does NOT match the NIST PUB 198 A.2 expected response.")
    sys.exit(1)