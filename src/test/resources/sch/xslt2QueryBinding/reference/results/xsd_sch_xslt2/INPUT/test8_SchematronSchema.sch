<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 8 - Empty and NotEmpty'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex8" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s8"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <pattern>
    <rule context="ex8:TS8_FT1">
      <assert test="ex8:att1">ts8_ft1_constraint1: att1-&gt;notEmpty()</assert>
      <assert test="not(ex8:att2)">ts8_ft1_constraint2: att2-&gt;isEmpty()</assert>
      <assert test="ex8:rFT1toFT2a/* | //*[concat('#',@gml:id)=current()/ex8:rFT1toFT2a/@xlink:href]">ts8_ft1_constraint3: rFT1toFT2a-&gt;notEmpty()</assert>
      <assert test="not(ex8:rFT1toFT2b/* | //*[concat('#',@gml:id)=current()/ex8:rFT1toFT2b/@xlink:href])">ts8_ft1_constraint4: rFT1toFT2b-&gt;isEmpty()</assert>
    </rule>
  </pattern>
</schema>
