<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/oclPathEncoding"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <pattern>
    <rule context="ex:FeatureType1">
      <let name="A" value="//*[concat('#',@gml:id)=current()/ex:ft1Toft2ByReference/@xlink:href]"/>
      <let name="B" value="//*[concat('#',@gml:id)=$A/ex:ft2Toft3ByReference/@xlink:href]"/>
      <let name="C" value="ex:ft1Toft2InlineOrByReference/* | //*[concat('#',@gml:id)=current()/ex:ft1Toft2InlineOrByReference/@xlink:href]"/>
      <let name="D" value="ex:ft1Toft2Inline/*"/>
      <assert test="$B">byRef_byRef_withExplicitLets: test</assert>
      <assert test="//*[concat('#',@gml:id)=$A/ex:ft2Toft3ByReference/@xlink:href]/ex:attribute">byReference_byReference: byReference_byReference</assert>
      <assert test="//*[concat('#',@gml:id)=current()/ex:ft1Toft2ByReference/@xlink:href]/ex:ft2Toft3Inline/*/ex:attribute">byReference_inline: byReference_inline</assert>
      <assert test="($A/ex:ft2Toft3InlineOrByReference/* | //*[concat('#',@gml:id)=$A/ex:ft2Toft3InlineOrByReference/@xlink:href])/ex:attribute">byReference_inlineOrByReference: byReference_inlineOrByReference</assert>
      <assert test="//*[concat('#',@gml:id)=$C/ex:ft2Toft3ByReference/@xlink:href]/ex:attribute">inlineOrByReference_byReference: inlineOrByReference_byReference</assert>
      <assert test="(ex:ft1Toft2InlineOrByReference/* | //*[concat('#',@gml:id)=current()/ex:ft1Toft2InlineOrByReference/@xlink:href])/ex:ft2Toft3Inline/*/ex:attribute">inlineOrByReference_inline: inlineOrByReference_inline</assert>
      <assert test="($C/ex:ft2Toft3InlineOrByReference/* | //*[concat('#',@gml:id)=$C/ex:ft2Toft3InlineOrByReference/@xlink:href])/ex:attribute">inlineOrByReference_inlineOrByReference: inlineOrByReference_inlineOrByReference</assert>
      <assert test="//*[concat('#',@gml:id)=$D/ex:ft2Toft3ByReference/@xlink:href]/ex:attribute">inline_byReference: inline_byReference</assert>
      <assert test="ex:ft1Toft2Inline/*/ex:ft2Toft3Inline/*/ex:attribute">inline_inline: inline_inline</assert>
      <assert test="($D/ex:ft2Toft3InlineOrByReference/* | //*[concat('#',@gml:id)=$D/ex:ft2Toft3InlineOrByReference/@xlink:href])/ex:attribute">inline_inlineOrByReference: inline_inlineOrByReference</assert>
    </rule>
  </pattern>
</schema>
