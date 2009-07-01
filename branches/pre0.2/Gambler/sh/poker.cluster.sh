#! /bin/bash

cd ..
`cat sh/java-path` -classpath bin:../bayes/bin poker.server.cluster.ClusterServerImpl localhost --tunnel=n --base_port=5901 --base_screen=1 --max_screens=10 --max_apps=20
