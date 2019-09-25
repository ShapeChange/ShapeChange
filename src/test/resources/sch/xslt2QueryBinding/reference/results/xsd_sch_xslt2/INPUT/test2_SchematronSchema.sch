<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 2 - AllInstances'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex2" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s2"/>
  <pattern>
    <rule context="ex2:TS2_FTX">
      <let name="A" value="//*[(local-name()='TS2_FT1' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s2') or (local-name()='TS2_FT3' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s2') or (local-name()='TS2_FT4' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s2')]"/>
      <let name="B" value="//*[local-name()='TS2_FT3' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s2']"/>
      <assert test="current()/ex2:ftCount = count($A)">ts2_ftx_constraint1: inv: ftCount = TS2_FT1.allInstances()-&gt;size()</assert>
      <assert test="count($B/ex2:att) = 2">ts2_ftx_constraint2: inv: TS2_FT2.allInstances().att-&gt;size() = 2</assert>
    </rule>
  </pattern>
</schema>
