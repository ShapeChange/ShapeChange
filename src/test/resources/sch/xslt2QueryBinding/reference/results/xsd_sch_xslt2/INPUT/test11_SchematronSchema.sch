<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 11 - If Then Else'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex11" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s11"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex11:TS11_FT1">
      <assert test="if (every $x in current()/ex11:att1 satisfies $x = true()) then current()/ex11:att2 &gt; 0 else current()/ex11:att2 &lt; 0">ts11_ft1_constraint1: inv: if att1-&gt;forAll(x|x = true) then att2 &gt; 0 else att2 &lt; 0 endif</assert>
      <assert test="string-length(concat(current()/ex11:att3, if (current()/ex11:att1 = true()) then 'x' else 'xxx')) &gt; 2">ts11_ft1_constraint2: inv: att3.concat(if att1 = true then 'x' else 'xxx' endif).size() &gt; 2</assert>
      <assert test="if (current()/ex11:att1 = true()) then for $VAR1 in (current()/ex11:rFT1toFT2/*, (for $BYREFVAR in current()/ex11:rFT1toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return (for $VAR2 in ($VAR1/ex11:rFT2toFT3/*, (for $BYREFVAR in $VAR1/ex11:rFT2toFT3/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR2) else for $VAR1 in (current()/ex11:rFT1toFT2/*, (for $BYREFVAR in current()/ex11:rFT1toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return (for $VAR2 in ($VAR1/ex11:rFT2toFT4/*, (for $BYREFVAR in $VAR1/ex11:rFT2toFT4/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR2)">ts11_ft1_constraint3: inv: if att1 = true then rFT1toFT2.rFT2toFT3-&gt;notEmpty() else rFT1toFT2.rFT2toFT4-&gt;notEmpty() endif</assert>
      <assert test="every $x in for $VAR1 in (current()/ex11:rFT1toFT2/*, (for $BYREFVAR in current()/ex11:rFT1toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 satisfies if ($x/ex11:att &gt; 0) then for $VAR1 in ($x/ex11:rFT2toFT3/*, (for $BYREFVAR in $x/ex11:rFT2toFT3/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 else for $VAR1 in ($x/ex11:rFT2toFT4/*, (for $BYREFVAR in $x/ex11:rFT2toFT4/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1">ts11_ft2_constraint4: inv: rFT1toFT2-&gt;forAll(x|if x.att &gt; 0 then x.rFT2toFT3-&gt;notEmpty() else x.rFT2toFT4-&gt;notEmpty() endif)</assert>
    </rule>
  </pattern>
</schema>
