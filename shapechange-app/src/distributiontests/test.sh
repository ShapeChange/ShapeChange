#!/bin/sh
java -Dfile.encoding=UTF-8 -jar ShapeChange-${project.version}.jar -c test/scxml/testSCXML.xml
java -Dfile.encoding=UTF-8 -jar ShapeChange-${project.version}.jar -c test/xmi/testXMI.xml