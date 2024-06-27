<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Profile Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="pctp" uri="http://example.org/shapechange/profileConstraintTransformer/profileSchema"/>
  <ns prefix="pctbs" uri="http://example.org/shapechange/profileConstraintTransformer/baseSchema"/>
  <pattern>
    <rule context="pctbs:B_FeatureType2">
      <assert test="not(pctbs:att2/*)">att2_prohibited: att2 is prohibited.</assert>
      <assert test="not(pctbs:att3/*)">att3_prohibited: att3 is prohibited.</assert>
    </rule>
    <rule context="pctbs:B_FeatureType3">
      <assert test="not(.)">B_FeatureType3_prohibited: B_FeatureType3 is prohibited. Use (one of) the following type(s) instead: P_FeatureType3.</assert>
    </rule>
    <rule context="pctbs:B_FeatureTypeX">
      <assert test="not(pctbs:attX/*)">attX_prohibited: attX is prohibited.</assert>
    </rule>
    <rule context="pctp:P_FeatureType3">
      <assert test="not(pctbs:att2/*)">att2_prohibited: att2 is prohibited.</assert>
      <assert test="not(pctbs:att3/*)">att3_prohibited: att3 is prohibited.</assert>
      <assert test="not(pctbs:att4/*)">att4_prohibited: att4 is prohibited.</assert>
    </rule>
    <rule context="pctp:P_FeatureTypeX">
      <assert test="not(pctbs:attX/*)">attX_prohibited: attX is prohibited.</assert>
    </rule>
  </pattern>
</schema>
