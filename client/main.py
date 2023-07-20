import subprocess
import sys
import datetime

input = sys.stdin.readline

command = "/home/researcher/AndroidStudioProjects/HealthLogger/client/log-client.sh"
proc = subprocess.Popen([command], stdout=subprocess.PIPE)

for line in proc.stdout:
    if line.decode("utf-8")[0] == '-': continue
    line = line.decode("utf-8")
    splitted = line.split(',')
    timestamp = datetime.datetime.fromtimestamp(int(splitted[0]) / 1000)
    heartrate = int(splitted[1])
    sys.stdout.write(f"{timestamp}, {heartrate}\n")

proc.kill()
proc.wait()
