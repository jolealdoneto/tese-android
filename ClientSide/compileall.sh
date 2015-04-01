#!/bin/bash
ant clean
ant debug
cd /home/neto/prj/ufmg/tese/testes/soot/AndroidInstrument/bin
rm -Rf sootOutput
java -cp "/home/neto/prj/ufmg/tese/testes/soot/TestingSoot/lib/*:../../*:."  AndroidInstrument -pp -android-jars /home/neto/adt-bundle-linux-x86_64-20140702/sdk/platforms/android-20 -process-dir /home/neto/prj/android/ClientSide/bin/ClientMain-debug.apk -allow-phantom-refs -w
alert jarsigner
jarsigner -verbose -keystore ~/.android/debug.keystore sootOutput/ClientMain-debug.apk androiddebugkey -sigalg MD5withRSA -digestalg SHA1
~/adt-bundle-linux-x86_64-20140702/sdk/platform-tools/adb install -r sootOutput/ClientMain-debug.apk
alert ok
