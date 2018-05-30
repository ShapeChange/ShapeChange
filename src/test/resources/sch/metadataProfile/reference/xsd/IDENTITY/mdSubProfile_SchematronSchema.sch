<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'SubProfile'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="mdsp" uri="http://example.org/shapechange/metadataprofile/subprofile"/>
  <ns prefix="gmd" uri="http://www.isotc211.org/2005/gmd"/>
  <pattern>
    <rule context="mdsp:MDSP_DataType">
      <assert test="not(gmd:hoursOfService/*)">hoursOfService_prohibited: The use of hoursOfService is prohibited.</assert>
    </rule>
  </pattern>
</schema>
