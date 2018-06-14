<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Test Schema1'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex1" uri="http://example.org/shapechange/sch/codelist2/codeListRestriction/1"/>
  <ns prefix="iso" uri="http://example.org/shapechange/sch/codelist2/isoBaseSchema"/>
  <pattern>
    <rule context="ex1:FeatureTypeS1_2">
      <assert test="iso:att1/*[local-name()='CodeListS1' and namespace-uri()='http://example.org/shapechange/sch/codelist2/codeListRestriction/1']">att1_is_CodeListS1:  The type of att1 is restricted to CodeListS1. </assert>
      <assert test="not(iso:att2/*) or not(iso:att2/*[not(local-name()='CodeListS2' and namespace-uri()='http://example.org/shapechange/sch/codelist2/codeListRestriction/1')])">att2_is_CodeListS2:  The type of att2 is restricted to CodeListS2. </assert>
    </rule>
    <rule context="ex1:FeatureTypeS1_3">
      <assert test="iso:att1/*[local-name()='CodeListS1' and namespace-uri()='http://example.org/shapechange/sch/codelist2/codeListRestriction/1']">att1_is_CodeListS1:  The type of att1 is restricted to CodeListS1. </assert>
      <assert test="not(iso:att2/*) or not(iso:att2/*[not(local-name()='CodeListS2' and namespace-uri()='http://example.org/shapechange/sch/codelist2/codeListRestriction/1')])">att2_is_CodeListS2:  The type of att2 is restricted to CodeListS2. </assert>
    </rule>
  </pattern>
</schema>
