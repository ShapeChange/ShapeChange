<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 8 - Empty and NotEmpty'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex8" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s8"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex8:TS8_FT1">
      <assert test="current()/ex8:att1">ts8_ft1_constraint1: att1-&gt;notEmpty()</assert>
      <assert test="not(current()/ex8:att2)">ts8_ft1_constraint2: att2-&gt;isEmpty()</assert>
      <assert test="for $VAR1 in (current()/ex8:rFT1toFT2a/*, (for $BYREFVAR in current()/ex8:rFT1toFT2a/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1">ts8_ft1_constraint3: rFT1toFT2a-&gt;notEmpty()</assert>
      <assert test="not(for $VAR1 in (current()/ex8:rFT1toFT2b/*, (for $BYREFVAR in current()/ex8:rFT1toFT2b/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1)">ts8_ft1_constraint4: rFT1toFT2b-&gt;isEmpty()</assert>
    </rule>
  </pattern>
</schema>
