<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 3 - Arithmetic'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex3" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s3"/>
  <pattern>
    <rule context="ex3:TS3_FT1">
      <let name="A" value="current()/ex3:att1"/>
      <let name="B" value="current()/ex3:att2"/>
      <let name="C" value="current()/ex3:att3"/>
      <assert test="$A + $B = 8">ts3_ft1_constraint1: att1 + att2 = 8</assert>
      <assert test="$B - $A = 2">ts3_ft1_constraint2: att2 - att1 = 2</assert>
      <assert test="$A * $B = $C">ts3_ft1_constraint3: att1 * att2 = att3</assert>
      <assert test="$C div 2 = 7.5">ts3_ft1_constraint4: inv: att3 / 2 = 7.5</assert>
    </rule>
  </pattern>
</schema>
