<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 4 - Attribute'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex4" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s4"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <xsl:key match="*[@*:id]" name="idKey" use="@*:id"/>
  <pattern>
    <rule context="ex4:TS4_FT1">
      <let name="A" value="current()/ex4:ft1Att"/>
      <let name="B" value="current()/ex4:rFT1toFT2Inline/*/ex4:rFT2toFT3Inline/*/ex4:rFT3toFT4Inline/*"/>
      <let name="C" value="for $VAR1 in for $BYREFVAR in current()/ex4:rFT1toFT2ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return $VAR1/ex4:rFT2toFT3Inline/*"/>
      <let name="D" value="for $VAR1 in for $BYREFVAR in current()/ex4:rFT1toFT2ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return (for $VAR2 in for $BYREFVAR in $VAR1/ex4:rFT2toFT3ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return $VAR2/ex4:rFT3toFT4Inline/*)"/>
      <let name="E" value="for $VAR1 in for $BYREFVAR in current()/ex4:rFT1toFT2ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return (for $VAR2 in for $BYREFVAR in $VAR1/ex4:rFT2toFT3ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return (for $VAR3 in for $BYREFVAR in $VAR2/ex4:rFT3toFT4ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return $VAR3))"/>
      <let name="F" value="for $VAR1 in for $BYREFVAR in current()/ex4:rFT1toFT2ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return (for $VAR2 in for $BYREFVAR in $VAR1/ex4:rFT2toFT3ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return (for $VAR3 in ($VAR2/ex4:rFT3toFT4InlineOrByReference/*, (for $BYREFVAR in $VAR2/ex4:rFT3toFT4InlineOrByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR3))"/>
      <let name="G" value="for $VAR1 in (current()/ex4:rFT1toFT2InlineOrByReference/*, (for $BYREFVAR in current()/ex4:rFT1toFT2InlineOrByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1"/>
      <let name="H" value="for $VAR1 in (current()/ex4:rFT1toFT2InlineOrByReference/*, (for $BYREFVAR in current()/ex4:rFT1toFT2InlineOrByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return (for $VAR2 in ($VAR1/ex4:rFT2toFT3InlineOrByReference/*, (for $BYREFVAR in $VAR1/ex4:rFT2toFT3InlineOrByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return (for $VAR3 in ($VAR2/ex4:rFT3toFT4InlineOrByReference/*, (for $BYREFVAR in $VAR2/ex4:rFT3toFT4InlineOrByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR3))"/>
      <let name="I" value="//*[local-name()='TS4_FT3' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s4']"/>
      <let name="J" value="current()/ex4:ft1AttFt4Check"/>
      <let name="K" value="current()/ex4:ft1AttDt1/*/ex4:dt1Att"/>
      <let name="L" value="current()/ex4:ft1AttDt1/*/ex4:dt1AttDt2/*/ex4:dt2Att"/>
      <let name="M" value="current()/ex4:rFT1toFT2Inline/*"/>
      <let name="N" value="for $VAR1 in for $BYREFVAR in current()/ex4:rFT1toFT2ByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')) return $VAR1"/>
      <assert test="$A = 1">ts4_ft1_constraint1: inv: ft1Att = 1</assert>
      <assert test="count($B) = 1">ts4_ft1_constraint10: inv: rFT1toFT2Inline.rFT2toFT3Inline.rFT3toFT4Inline-&gt;size() = 1</assert>
      <assert test="count($C) = 2">ts4_ft1_constraint11: inv: rFT1toFT2ByReference.rFT2toFT3Inline-&gt;size() = 2</assert>
      <assert test="count($D) = 2">ts4_ft1_constraint12: inv: rFT1toFT2ByReference.rFT2toFT3ByReference.rFT3toFT4Inline-&gt;size() = 2</assert>
      <assert test="count($E) = 1">ts4_ft1_constraint13: inv: rFT1toFT2ByReference.rFT2toFT3ByReference.rFT3toFT4ByReference-&gt;size() = 1</assert>
      <assert test="count($F) = 2">ts4_ft1_constraint14: inv: rFT1toFT2ByReference.rFT2toFT3ByReference.rFT3toFT4InlineOrByReference-&gt;size() = 2</assert>
      <assert test="count($G) = 2">ts4_ft1_constraint15: inv: rFT1toFT2InlineOrByReference-&gt;size() = 2</assert>
      <assert test="count($H) = 2">ts4_ft1_constraint16: inv: rFT1toFT2InlineOrByReference.rFT2toFT3InlineOrByReference.rFT3toFT4InlineOrByReference-&gt;size() = 2</assert>
      <assert test="some $x in for $VAR1 in ($I/ex4:rFT3toFT4InlineOrByReference/*, (for $BYREFVAR in $I/ex4:rFT3toFT4InlineOrByReference/@xlink:href return key('idKey',substring-after($BYREFVAR,'#')))) return $VAR1 satisfies $x/ex4:ft4Att = $J">ts4_ft1_constraint17: inv: TS4_FT3.allInstances().rFT3toFT4InlineOrByReference-&gt;exists(x|x.ft4Att = self.ft1AttFt4Check)</assert>
      <assert test="$K = 2.5">ts4_ft1_constraint2: inv: ft1AttDt1.dt1Att = 2.5</assert>
      <assert test="$L = 'test'">ts4_ft1_constraint3: inv: ft1AttDt1.dt1AttDt2.dt2Att = 'test'</assert>
      <assert test="every $x in $M satisfies $x/ex4:ft2AttClGml32/concat(@codeSpace,'/',text()) = 'http://example.org/codelist/code'">ts4_ft1_constraint4: inv: rFT1toFT2Inline-&gt;forAll(x|x.ft2AttClGml32 = TS4_CL_GML32::code)</assert>
      <assert test="every $x in $M satisfies $x/ex4:ft2AttClIso19139/*/concat(@codeList,'/',@codeListValue) = 'http://example.org/codelist/code'">ts4_ft1_constraint5: inv: rFT1toFT2Inline-&gt;forAll(x|x.ft2AttClIso19139 = TS4_CL_ISO19139::code)</assert>
      <assert test="every $x in $N satisfies $x/ex4:ft2AttClGml32/concat(@codeSpace,'/',text()) = 'http://example.org/codelist/code'">ts4_ft1_constraint6: inv: rFT1toFT2ByReference-&gt;forAll(x|x.ft2AttClGml32 = TS4_CL_GML32::code)</assert>
      <assert test="every $x in $N satisfies $x/ex4:ft2AttClIso19139/*/concat(@codeList,'/',@codeListValue) = 'http://example.org/codelist/code'">ts4_ft1_constraint7: inv: rFT1toFT2ByReference-&gt;forAll(x|x.ft2AttClIso19139 = TS4_CL_ISO19139::code)</assert>
      <assert test="every $x in $G satisfies $x/ex4:ft2AttClGml32/concat(@codeSpace,'/',text()) = 'http://example.org/codelist/code'">ts4_ft1_constraint8: inv: rFT1toFT2InlineOrByReference-&gt;forAll(x|x.ft2AttClGml32 = TS4_CL_GML32::code)</assert>
      <assert test="every $x in $G satisfies $x/ex4:ft2AttClIso19139/*/concat(@codeList,'/',@codeListValue) = 'http://example.org/codelist/code'">ts4_ft1_constraint9: inv: rFT1toFT2InlineOrByReference-&gt;forAll(x|x.ft2AttClIso19139 = TS4_CL_ISO19139::code)</assert>
    </rule>
  </pattern>
</schema>
