<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 10 - ForAll'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex10" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s10"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <pattern>
    <rule context="ex10:TS10_FT1">
      <assert test="every $x in ex10:att satisfies $x = 'test'">ts10_ft1_constraint1: att-&gt;forAll(x|x='test')</assert>
      <assert test="every $x in for $VAR1 in (current()/ex10:rFT1toFT2/* | //*[concat('#',@gml:id)=current()/ex10:rFT1toFT2/@xlink:href]) return $VAR1 satisfies $x/ex10:att = 5">ts10_ft1_constraint2: rFT1toFT2-&gt;forAll(x|x.att=5)</assert>
      <assert test="every $x in for $VAR1 in (current()/ex10:rFT1toFT2/* | //*[concat('#',@gml:id)=current()/ex10:rFT1toFT2/@xlink:href]) return (for $VAR2 in ($VAR1/ex10:rFT2toFT3/* | //*[concat('#',@gml:id)=$VAR1/ex10:rFT2toFT3/@xlink:href]) return $VAR2) satisfies $x/ex10:att = true()">ts10_ft1_constraint3: rFT1toFT2.rFT2toFT3-&gt;forAll(x|x.att = true)</assert>
      <assert test="every $x in for $VAR1 in (current()/ex10:rFT1toFT2/* | //*[concat('#',@gml:id)=current()/ex10:rFT1toFT2/@xlink:href]) return $VAR1 satisfies every $y in for $VAR1 in ($x/ex10:rFT2toFT3/* | //*[concat('#',@gml:id)=$x/ex10:rFT2toFT3/@xlink:href]) return $VAR1 satisfies $y/ex10:att = true()">ts10_ft1_constraint4: rFT1toFT2-&gt;forAll(x|x.rFT2toFT3-&gt;forAll(y|y.att = true))</assert>
    </rule>
  </pattern>
</schema>
