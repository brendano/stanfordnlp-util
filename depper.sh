#!/bin/zsh
h=$(dirname $0)
java -cp $h/out/production/Stanford-mystuff:$h/../stanford-corenlp-2012-01-08/stanford-corenlp-2012-01-08.jar:$h/lib/json-simple-1.1.1.jar Depper

