<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 15 - Matches'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex15" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s15"/>
  <pattern>
    <rule context="ex15:TS15_FT">
      <let name="A" value="current()/ex15:att"/>
      <assert test="matches($A, '^(foo|bar)$')">ts15_ft_constraint1: att.matches('^(foo|bar)$')</assert>
    </rule>
  </pattern>
</schema>
