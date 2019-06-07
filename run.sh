#!/usr/bin/env bash
cd bin
#java CodeGenerator ../testFiles/HelloWorld.jmm
java CodeGenerator ../testFiles/$1
cd ../jasmin
#java -jar jasmin.jar Factorial.j
java -jar jasmin.jar $1.j
#javap -c Factorial.class
javap -c $1
cd ..

# cd bin
# java jmm $1
# java -jar ../jasmin.jar $1.j
# java $1
# cd ..