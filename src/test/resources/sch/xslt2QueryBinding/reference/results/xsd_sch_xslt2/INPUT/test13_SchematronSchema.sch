<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 13 - Let'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex13" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s13"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex13:TS13_FT1">
      <let name="A" value="//*[local-name()='TS13_FT2' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s13']"/>
      <let name="B" value="current()/ex13:att1"/>
      <let name="C" value="current()/ex13:att2"/>
      <assert test="count(for $VAR1 in (current()/ex13:rFT1toFT2/*, (for $BYREFVAR in current()/ex13:rFT1toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1) = count($A)">ts13_ft1_constraint1: let ft2instances = TS13_FT2.allInstances() in rFT1toFT2-&gt;size() = ft2instances-&gt;size()</assert>
      <assert test="not($B) or 0 &lt; $B and $B &lt; count($C)">ts13_ft1_constraint2: att1, if not empty, must be greater 0 and less than the cardinality of att2.</assert>
    </rule>
  </pattern>
</schema>
