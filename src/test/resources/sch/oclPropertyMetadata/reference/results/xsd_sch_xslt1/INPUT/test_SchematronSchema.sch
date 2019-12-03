<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/oclConstraintOnProperties"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="ext" uri="http://example.org/shapechange/sch/oclConstraintOnProperties/externalSchema"/>
  <pattern>
    <rule context="ex:FeatureType">
      <let name="A" value="ex:att1"/>
      <let name="B" value="ex:att2"/>
      <assert test="not(//*[@gml:id=$A/@metadata][ex:prop != 'test'])">att1_metadata1: att1 metadata shall have prop='test'</assert>
      <assert test="not(//*[@gml:id=$B/@metadata][ext:att != 2])">att2_metadata1: att2 metadata shall have att=2</assert>
      <assert test="not(//*[@gml:id=$A/@metadata][ex:prop != 'test'])">constraint_on_att1: Constraint directly defined on property att1.</assert>
    </rule>
  </pattern>
</schema>
