#! /bin/bash

cd ..
`cat sh/java-path` -classpath bin:../bayes/bin poker.server.base.PokerBaseServerImpl localhost 2>&1 | tee logs/base_log
