<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 1 - Multiple Tests'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex1" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s1"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex1:TS1_FeatureType3">
      <assert test="every $x in for $VAR1 in (current()/ex1:rFT3toFT2/*, (for $BYREFVAR in current()/ex1:rFT3toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return (for $VAR2 in ($VAR1/ex1:rFT2toFT1/*, (for $BYREFVAR in $VAR1/ex1:rFT2toFT1/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR2/ex1:att3/*/ex1:dtAtt) satisfies $x = 'test'">ts1_ft3_constraint1: self.rFT3toFT2.rFT2toFT1.att3.dtAtt = 'test'</assert>
      <assert test="every $ft2 in for $VAR1 in (current()/ex1:rFT3toFT2/*, (for $BYREFVAR in current()/ex1:rFT3toFT2/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 satisfies some $ft1 in for $VAR1 in ($ft2/ex1:rFT2toFT1/*, (for $BYREFVAR in $ft2/ex1:rFT2toFT1/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 satisfies $ft1/ex1:att1 = 5">ts1_ft3_constraint2: self.rFT3toFT2-&gt;forAll(ft2|ft2.rFT2toFT1-&gt;exists(ft1|ft1.att1 = 5))</assert>
    </rule>
    <rule context="ex1:TS1_LetTest">
      <let name="A" value="current()/ex1:att1"/>
      <let name="B" value="current()/ex1:att2"/>
      <let name="C" value="current()/ex1:att3/*/ex1:attx"/>
      <let name="D" value="//*[local-name()='TS1_LetTest' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s1']"/>
      <assert test="not($A) or 0 &lt; $A and $A &lt; count($B)">ts1_lettest_constraint1: att1 must be greater 0 and less than the cardinality of att2.</assert>
      <assert test="not($A) or (some $z in $B satisfies $z = $A) and count($B) = count(distinct-values($B))">ts1_lettest_constraint2: att2 contains att1 and is unique.</assert>
      <assert test="not($A) or not($C) or $A != $C">ts1_lettest_constraint3: att1 must be different from associated attx.</assert>
      <assert test="some $x in $D satisfies 0 &lt;= $x/ex1:att3/*/ex1:attx and $x/ex1:att3/*/ex1:attx &lt; 2 * $A">ts1_lettest_constraint4: There need to exist TS1_LetTest objects, which numerically relate to the current object.</assert>
    </rule>
    <rule context="ex1:TS1_M">
      <let name="A" value="//*[local-name()='TS1_FTM' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s1']"/>
      <assert test="some $x in for $VAR1 in ($A/ex1:rFTMtoM/*, (for $BYREFVAR in $A/ex1:rFTMtoM/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 satisfies generate-id($x) = generate-id(current())">ts1_m_constraint1: TS1_M must be referenced by a TS1_FTM.</assert>
    </rule>
  </pattern>
</schema>
