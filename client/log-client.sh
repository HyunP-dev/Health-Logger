#!/bin/bash

# adb shell logcat --pid=`adb shell ps | grep kr.ac.hallym | awk '{print $2}'` | awk '/HeartrateListener/{print $NF}'
adb logcat -v raw HeartrateListener:D *:S