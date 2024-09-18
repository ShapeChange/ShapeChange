<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 20 - Unique'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex20" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s20"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex20:TS20_FT1">
      <let name="A" value="current()/ex20:att1/*"/>
      <let name="B" value="current()/ex20:att7"/>
      <let name="C" value="for $VAR1 in (current()/ex20:rFT1toFT4/*, (for $BYREFVAR in current()/ex20:rFT1toFT4/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1"/>
      <let name="D" value="current()/ex20:att2"/>
      <let name="E" value="current()/ex20:att3"/>
      <let name="F" value="current()/ex20:att4/*"/>
      <let name="G" value="for $VAR1 in (current()/ex20:rFT1toFT2/*, (for $BYREFVAR in current()/ex20:rFT1toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1"/>
      <let name="H" value="for $VAR1 in (current()/ex20:rFT1toType/*, (for $BYREFVAR in current()/ex20:rFT1toType/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1"/>
      <let name="I" value="current()/ex20:att5"/>
      <let name="J" value="for $VAR1 in (current()/ex20:rFT1toFT3/*, (for $BYREFVAR in current()/ex20:rFT1toFT3/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1"/>
      <let name="K" value="current()/ex20:att6/text()"/>
      <assert test="for $COUNT1 in count($A), $COUNT2 in sum(for $ISUVAR1 in $A, $ISUVAR2 in $A return (if(empty($ISUVAR1) and empty($ISUVAR2)) then 0 else if ((empty($ISUVAR1) and not(empty($ISUVAR2))) or (not(empty($ISUVAR1)) and empty($ISUVAR2))) then 1 else if (generate-id($ISUVAR1) = generate-id($ISUVAR2)) then 0 else if (deep-equal($ISUVAR1,$ISUVAR2)) then 0 else 1)) return $COUNT1 * ($COUNT1 - 1) = $COUNT2">ts20_ft1_constraint1: att1-&gt;isUnique(x|x)</assert>
      <assert test="count($B) = count(distinct-values($B))">ts20_ft1_constraint10: att7-&gt;isUnique(x|x)</assert>
      <assert test="count($C) = count(distinct-values(for $x in $C return (if (empty($x/ex20:att1/*/ex20:dtAtt1)) then 'SC_EMPTY_ISU_BODY' else $x/ex20:att1/*/ex20:dtAtt1)))">ts20_ft1_constraint11: rFT1toFT4-&gt;isUnique(x|x.att1.dtAtt1)</assert>
      <assert test="count($C) = count(distinct-values(for $x in $C return (if (empty($x/ex20:att2/text())) then 'SC_EMPTY_ISU_BODY' else $x/ex20:att2/text())))">ts20_ft1_constraint12: rFT1toFT4-&gt;isUnique(x|x.att2)</assert>
      <assert test="count($C) = count(distinct-values(for $x in $C return (if (empty($x/ex20:att3)) then 'SC_EMPTY_ISU_BODY' else $x/ex20:att3)))">ts20_ft1_constraint13: rFT1toFT4-&gt;isUnique(x|x.att3)</assert>
      <assert test="count($C) = count(distinct-values(for $x in $C return (if (empty($x/ex20:att4)) then 'SC_EMPTY_ISU_BODY' else $x/ex20:att4)))">ts20_ft1_constraint14: rFT1toFT4-&gt;isUnique(x|x.att4)</assert>
      <assert test="count($C) = count(distinct-values(for $x in $C return (if (empty(for $VAR1 in ($x/ex20:rFT4toFT5/*, (for $BYREFVAR in $x/ex20:rFT4toFT5/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1)) then 'SC_EMPTY_ISU_BODY' else for $VAR1 in ($x/ex20:rFT4toFT5/*, (for $BYREFVAR in $x/ex20:rFT4toFT5/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1/@*:id)))">ts20_ft1_constraint15: rFT1toFT4-&gt;isUnique(x|x.rFT4toFT5)</assert>
      <assert test="count($D) = count(distinct-values($D))">ts20_ft1_constraint2: att2-&gt;isUnique(x|x)</assert>
      <assert test="count($E) = count(distinct-values($E))">ts20_ft1_constraint3: att3-&gt;isUnique(x|x)</assert>
      <assert test="for $COUNT1 in count($F), $COUNT2 in sum(for $ISUVAR1 in $F, $ISUVAR2 in $F return (if(empty($ISUVAR1) and empty($ISUVAR2)) then 0 else if ((empty($ISUVAR1) and not(empty($ISUVAR2))) or (not(empty($ISUVAR1)) and empty($ISUVAR2))) then 1 else if (generate-id($ISUVAR1) = generate-id($ISUVAR2)) then 0 else if (deep-equal($ISUVAR1,$ISUVAR2)) then 0 else 1)) return $COUNT1 * ($COUNT1 - 1) = $COUNT2">ts20_ft1_constraint4: att4-&gt;isUnique(x|x)</assert>
      <assert test="count($G) = count(distinct-values($G/@*:id))">ts20_ft1_constraint5: rFT1toFT2-&gt;isUnique(x|x)</assert>
      <assert test="count($H) = count(distinct-values($H/@*:id))">ts20_ft1_constraint6: rFT1toType-&gt;isUnique(x|x)</assert>
      <assert test="count($I) = count(distinct-values($I/xs:boolean(.)))">ts20_ft1_constraint7: att5-&gt;isUnique(x|x)</assert>
      <assert test="count($J) = count(distinct-values(for $x in $J return (if (empty($x/ex20:att1)) then 'SC_EMPTY_ISU_BODY' else $x/ex20:att1)))">ts20_ft1_constraint8: rFT1toFT3-&gt;isUnique(x|x.att1)</assert>
      <assert test="count($K) = count(distinct-values($K))">ts20_ft1_constraint9: att6-&gt;isUnique(x|x)</assert>
    </rule>
  </pattern>
</schema>
