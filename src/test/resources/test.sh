#!/bin/sh
java -Dfile.encoding=UTF-8 -jar ShapeChange-${project.version}.jar -c http://shapechange.net/resources/test/testXMI.xml
