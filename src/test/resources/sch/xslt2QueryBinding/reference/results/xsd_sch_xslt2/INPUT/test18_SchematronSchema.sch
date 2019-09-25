<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 18 - Size'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex18" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s18"/>
  <pattern>
    <rule context="ex18:TS18_FT">
      <assert test="count(current()/ex18:att1) &gt; 3">ts18_ft_constraint1: att1-&gt;size() &gt; 3</assert>
      <assert test="string-length(current()/ex18:att2) &gt; 5">ts18_ft_constraint2: att2.size() &gt; 5</assert>
    </rule>
  </pattern>
</schema>
