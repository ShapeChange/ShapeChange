<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 5 - Cast'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex5" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s5"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex5:TS5_FT">
      <let name="A" value="current()/ex5:att1/*"/>
      <let name="B" value="$A[local-name()='TS5_Codelist' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s5']"/>
      <let name="C" value="for $VAR1 in (current()/ex5:att2/*, (for $BYREFVAR in current()/ex5:att2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1"/>
      <assert test="concat($B/@codeList,'/',$B/@codeListValue) = 'http://example.org/codelist/XYZ'">ts5_ft_constraint1: att1.oclAsType(TS5_Codelist) = TS5_Codelist::XYZ</assert>
      <assert test="count($C[local-name()='TS5_DT3' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s5']) = count($C)">ts5_ft_constraint2: att2.oclAsType(TS5_DT2)-&gt;size() = att2-&gt;size()</assert>
    </rule>
  </pattern>
</schema>
