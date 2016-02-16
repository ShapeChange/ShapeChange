#!/bin/sh
java -jar ShapeChange-${project.version}.jar -Dfile.encoding=UTF-8 -c http://shapechange.net/resources/test/testXMI.xml
