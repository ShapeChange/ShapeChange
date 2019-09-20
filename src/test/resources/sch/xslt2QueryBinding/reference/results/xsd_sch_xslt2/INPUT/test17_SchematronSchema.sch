<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
 <title>Schematron constraints for schema 'Test Schema 17 - Select'</title>
 <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
 <ns prefix="ex17" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s17"/>
 <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
 <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
 <pattern>
  <rule context="ex17:TS17_FT1">
   <assert test="count((current()/ex17:att)[for $x in . return $x &lt; 0]) &lt;= 1">ts17_ft1_constraint1: att-&gt;select(x|x &lt; 0-&gt;size() &lt;= 1</assert>
   <assert test="count((for $VAR1 in (current()/ex17:rFT1toFT2/* | //*[concat('#',@gml:id)=current()/ex17:rFT1toFT2/@xlink:href]) return $VAR1)[for $x in . return some $y in for $VAR1 in ($x/ex17:rFT2toFT3/* | //*[concat('#',@gml:id)=$x/ex17:rFT2toFT3/@xlink:href]) return $VAR1 satisfies $y/ex17:att = true()]) = 1">ts17_ft1_constraint2: rFT1toFT2-&gt;select(x|x.rFT2toFT3-&gt;exists(y|y.att = true))-&gt;size() = 1</assert>
  </rule>
 </pattern>
</schema>
