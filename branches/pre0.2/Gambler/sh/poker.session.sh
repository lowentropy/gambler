#! /bin/bash

cd ..
`cat sh/java-path` -classpath bin:../bayes/bin:ext/jdom.jar poker.server.session.SessionServerImpl localhost 2>&1 | tee logs/session_log
