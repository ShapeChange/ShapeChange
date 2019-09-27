<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 19 - Substring'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex19" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s19"/>
  <pattern>
    <rule context="ex19:TS19_FT">
      <let name="A" value="current()/ex19:att1"/>
      <let name="B" value="current()/ex19:att2"/>
      <assert test="substring($A, 1, 3 - 1 + 1) = 'foo'">ts19_ft_constraint1: att1.substring(1,3) = 'foo'</assert>
      <assert test="every $x in $B satisfies substring($x, string-length($x) - 3, string-length($x) - (string-length($x) - 3) + 1) = 'test'">ts19_ft_constraint2: att2-&gt;forAll(x|x.substring(x.size()-3,x.size()) = 'test')</assert>
    </rule>
  </pattern>
</schema>
