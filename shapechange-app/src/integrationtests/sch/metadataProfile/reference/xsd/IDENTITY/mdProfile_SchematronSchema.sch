<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'MetadataProfile'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="mdp" uri="http://example.org/shapechange/metadataprofile"/>
  <ns prefix="gmd" uri="http://www.isotc211.org/2005/gmd"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <pattern>
    <rule context="mdp:MDP_Contact">
      <assert test="not(gmd:hoursOfService/*)">hoursOfService_prohibited: The use of hoursOfService is prohibited.</assert>
    </rule>
    <rule context="mdp:MDP_Metadata">
      <let name="A" value="count(gmd:locale/* | //*[concat('#',@id)=current()/gmd:locale/@xlink:href])"/>
      <assert test="mdp:attA/* = 'TEST'">attA_value: attA must have value 'TEST'</assert>
      <assert test="count(mdp:attB/*) &lt;= 5">attB_multiplicity:  attB multiplicity is 1..5 (the size of the set of locale values must be smaller than or equal to 5). </assert>
      <assert test="gmd:fileIdentifier/*">fileIdentifier_multiplicity:  fileIdentifier is mandatory. </assert>
      <assert test="$A &gt;= 0 and $A &lt;= 1">locale_multiplicity:  locale multiplicity is 0..1 (the size of the set of locale values must be 1). </assert>
      <assert test="not(gmd:metadataStandardName/*) or gmd:metadataStandardName/* = 'ISO 19115'">metadataStandardName_value:  metadataStandardName has fixed value 'ISO 19115' </assert>
      <assert test="not(gmd:parentIdentifier/*)">parentIdentifier_prohibited:  parentIdentifier is prohibited. </assert>
    </rule>
  </pattern>
</schema>
