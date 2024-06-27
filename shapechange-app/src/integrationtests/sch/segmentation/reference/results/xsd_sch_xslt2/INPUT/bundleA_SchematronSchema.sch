<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for a part of schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/segmentation"/>
  <pattern>
    <rule context="ex:FT_BundleA">
      <let name="A" value="current()/ex:prop"/>
      <assert test="$A">prop not empty bundleA</assert>
    </rule>
    <rule context="ex:FT_LeafA2">
      <let name="A" value="current()/ex:prop"/>
      <assert test="$A">prop not empty leafA2</assert>
    </rule>
  </pattern>
</schema>
