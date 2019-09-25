<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 16 - PropertyMetadata'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex16" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s16"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex16:TS16_FT1">
      <assert test="(for $METAREFVAR in current()/ex16:att/@metadata return key('idKey',substring-after($METAREFVAR,'#')))/ex16:attMeta = 10">ts16_ft1_constraint1: att.propertyMetadata().attMeta = 10</assert>
      <assert test="not(for $METAREFVAR in current()/ex16:rFT1toFT2/@metadata return key('idKey',substring-after($METAREFVAR,'#'))) or (for $METAREFVAR in current()/ex16:rFT1toFT2/@metadata return key('idKey',substring-after($METAREFVAR,'#')))/ex16:attMeta = 20">ts16_ft1_constraint2: rFT1toFT2.propertyMetadata()-&gt;notEmpty() implies rFT1toFT2.propertyMetadata().attMeta = 20</assert>
      <assert test="every $x in for $VAR1 in (current()/ex16:rFT1toFT2/*, (for $BYREFVAR in current()/ex16:rFT1toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 satisfies (for $METAREFVAR in $x/ex16:rFT2toFT3/@metadata return key('idKey',substring-after($METAREFVAR,'#')))/ex16:attMeta = 30">ts16_ft1_constraint3: rFT1toFT2-&gt;forAll(x|x.rFT2toFT3.propertyMetadata().attMeta = 30)</assert>
    </rule>
  </pattern>
</schema>
