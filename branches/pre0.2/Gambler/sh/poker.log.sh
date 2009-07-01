#! /bin/bash

cd ..
`cat sh/java-path` -classpath bin:../bayes/bin poker.server.log.LogServerImpl localhost $*
