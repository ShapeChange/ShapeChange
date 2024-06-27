<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/oclConstraintOnProperties"/>
  <pattern>
    <rule context="ex:FeatureType">
      <assert test="ex:att1 &gt; 40">att1_constraint: The value of att1 shall be greater than 40.</assert>
      <assert test="ex:att2/*/ex:dtAtt = 'Test'">att2_constraint: att2.dtAtt shall be equal to 'Test'</assert>
    </rule>
    <rule context="ex:SubFT1">
      <assert test="ex:att1 &gt; 40">att1_constraint: The value of att1 shall be greater than 40.</assert>
      <assert test="ex:att2/*/ex:dtAtt = 'Test'">att2_constraint: att2.dtAtt shall be equal to 'Test'</assert>
    </rule>
    <rule context="ex:SubSubFT2">
      <assert test="ex:att1 &gt; 40">att1_constraint: The value of att1 shall be greater than 40.</assert>
      <assert test="ex:att2/*/ex:dtAtt = 'Test'">att2_constraint: att2.dtAtt shall be equal to 'Test'</assert>
    </rule>
    <rule context="ex:SubFT2">
      <assert test="ex:att1 &gt; 40">att1_constraint: The value of att1 shall be greater than 40.</assert>
      <assert test="ex:att2/*/ex:dtAtt = 'Test'">att2_constraint: att2.dtAtt shall be equal to 'Test'</assert>
    </rule>
  </pattern>
</schema>
