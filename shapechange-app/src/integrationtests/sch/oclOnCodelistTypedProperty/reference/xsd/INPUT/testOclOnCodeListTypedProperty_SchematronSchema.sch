<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/codelistCheck"/>
  <ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
  <pattern>
    <rule context="ex:FeatureType">
      <assert test="not(ex:attribute1iso/*/@codeListValue) or ex:attribute1iso/*/@codeListValue = 'code1'">attribute1iso_value: attribute1iso must have value CodeListIso19139::code1</assert>
      <assert test="not(ex:attribute2iso/*/@codeListValue[. and . != 'code2'])">attribute2iso_value: attribute2iso must have value CodeListIso19139::code2</assert>
      <assert test="not(ex:attribute3Gml32/text()) or ex:attribute3Gml32/text() = 'codeA'">attribute3Gml32_value: attribute3Gml32 must have value CodeListGml32::codeA</assert>
      <assert test="not(ex:attribute4Gml32/text()[. and . != 'codeB' and . != 'codeC'])">attribute4Gml32_value: attribute4Gml32 must have value CodeListGml32::codeB or SubCodeListGml32::codeC. </assert>
      <assert test="not(ex:attribute5Gml33[not(@xsi:nil='true')]/concat(@codeSpace,'#',text())) or ex:attribute5Gml33[not(@xsi:nil='true')]/concat(@codeSpace,'#',text()) = 'file:/C:/TMP/CodeListGml33.xml#codeX'">attribute5Gml33_value: attribute5Gml33 must have value CodeListGml33::codeX</assert>
      <assert test="not(ex:attribute6Gml33[not(@xsi:nil='true')]/concat(@codeSpace,'#',text())[. and . != 'file:/C:/TMP/CodeListGml33.xml#codeY'])">attribute6Gml33_value: attribute6Gml33 must have value CodeListGml33::codeY</assert>
      <assert test="not(ex:dt/*/ex:att1iso/*/@codeListValue) or ex:dt/*/ex:att1iso/*/@codeListValue = 'code1'">dtatt1iso_value: dt.att1iso must have value CodeListIso19139::code1</assert>
      <assert test="not((ex:dt/*/ex:att2iso/*/@codeListValue)[. and . != 'code2'])">dtatt2iso_value: dt.att2iso must have value CodeListIso19139::code2</assert>
      <assert test="not(ex:dt/*/ex:att3Gml32/text()) or ex:dt/*/ex:att3Gml32/text() = 'codeA'">dtatt3Gml32_value: dt.att3Gml32 must have value CodeListGml32::codeA</assert>
      <assert test="not((ex:dt/*/ex:att4Gml32/text())[. and . != 'codeB'])">dtatt4Gml32_value: dt.att4Gml32 must have value CodeListGml32::codeB</assert>
      <assert test="not(ex:dt/*/ex:att5Gml33[not(@xsi:nil='true')]/concat(@codeSpace,'#',text())) or ex:dt/*/ex:att5Gml33[not(@xsi:nil='true')]/concat(@codeSpace,'#',text()) = 'file:/C:/TMP/CodeListGml33.xml#codeX'">dtatt5Gml33_value: dt.att5Gml33 must have value CodeListGml33::codeX</assert>
      <assert test="not((ex:dt/*/ex:att6Gml33[not(@xsi:nil='true')]/concat(@codeSpace,'#',text()))[. and . != 'file:/C:/TMP/CodeListGml33.xml#codeY'])">dtatt6Gml33_value: dt.att6Gml33 must have value CodeListGml33::codeY</assert>
    </rule>
  </pattern>
</schema>
