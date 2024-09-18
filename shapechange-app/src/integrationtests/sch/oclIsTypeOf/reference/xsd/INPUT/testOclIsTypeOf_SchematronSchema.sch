<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/oclIsTypeOf"/>
  <ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
  <pattern>
    <rule context="ex:FeatureType">
      <assert test="not(ex:att1/*/ex:valueOrReason[not(@xsi:nil='true')]) or ex:att1/*/ex:valueOrReason[not(@xsi:nil='true')][local-name()='SubMeasure' and namespace-uri()='http://example.org/shapechange/sch/oclIsTypeOf']">att1_value_oclIstypeOf_SubMeasure:  att1.valueOrReason.value must be of type SubMeasure. </assert>
      <assert test="not(ex:att2/*/ex:valuesOrReason[not(@xsi:nil='true')]) or not((ex:att2/*/ex:valuesOrReason[not(@xsi:nil='true')])[not(local-name()='SubMeasure' and namespace-uri()='http://example.org/shapechange/sch/oclIsTypeOf')])">att2_values_oclIsTypeOf_SubMeasure:  att2.valuesOrReason.values must be of type SubMeasure. </assert>
    </rule>
  </pattern>
</schema>
