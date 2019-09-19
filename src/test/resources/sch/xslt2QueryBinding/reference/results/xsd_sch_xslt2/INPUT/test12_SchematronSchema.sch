<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 12 - KindOf TypeOf'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex12" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s12"/>
  <pattern>
    <rule context="ex12:TS12_FT">
      <assert test="every $x in ex12:att1/* satisfies $x[local-name()='TS12_DT5' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s12']">ts12_ft_constraint1: inv: att1-&gt;forAll(x|x.oclIsTypeOf(TS12_DT5))</assert>
      <assert test="every $x in ex12:att2/* satisfies $x[(local-name()='TS12_DT3' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s12') or (local-name()='TS12_DT4' and namespace-uri()='http://example.org/shapechange/sch/xslt2QueryBinding/s12')]">ts12_ft_constraint2: inv: att2-&gt;forAll(x|x.oclIsKindOf(TS12_DT2)</assert>
    </rule>
  </pattern>
</schema>
