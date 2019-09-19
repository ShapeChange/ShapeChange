<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 7 - Concatenate'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex7" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s7"/>
  <pattern>
    <rule context="ex7:TS7_FT">
      <assert test="concat(ex7:att1, ex7:att2) = ex7:att3">ts7_ft_constraint1: att1.concat(att2) = att3</assert>
    </rule>
  </pattern>
</schema>
