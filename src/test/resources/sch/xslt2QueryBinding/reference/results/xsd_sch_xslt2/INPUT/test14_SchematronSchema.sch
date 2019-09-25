<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
 <title>Schematron constraints for schema 'Test Schema 14 - Logic'</title>
 <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
 <ns prefix="ex14" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s14"/>
 <pattern>
  <rule context="ex14:TS14_FT">
   <assert test="current()/ex14:att1 = true() and current()/ex14:att2 = true()">ts14_ft_constraint1: (att1 = true) and (att2 = true)</assert>
   <assert test="current()/ex14:att3 = true() or current()/ex14:att4 = true()">ts14_ft_constraint2: (att3 = true) or (att4 = true)</assert>
   <assert test="(current()/ex14:att5 = true()) != (current()/ex14:att6 = true())">ts14_ft_constraint3: att5 = true xor att6 = true</assert>
   <assert test="current()/ex14:att7 != true() or current()/ex14:att8 = true()">ts14_ft_constraint4: (att7 = true) implies (att8 = true)</assert>
   <assert test="current()/ex14:att9 != true()">ts14_ft_constraint5: not(att9 = true)</assert>
  </rule>
 </pattern>
</schema>
