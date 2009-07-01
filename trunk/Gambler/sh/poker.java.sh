#! /bin/bash

cd ..
killall rmid 2>/dev/null
killall rmiregistry 2>/dev/null
sleep 2
`cat sh/rmid-path` -J-classpath -Jbin:../bayes/bin:ext/jdom.jar &
`cat sh/rmiregistry-path` -J-classpath -Jbin:../bayes/bin:ext/jdom.jar
