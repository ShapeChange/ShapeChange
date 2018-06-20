<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/iteratorWithByReferenceProperty"/>
  <ns prefix="gml" uri="http://www.opengis.net/gml/3.2"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <pattern>
    <rule context="ex:FT1">
      <let name="A" value="ex:ft2/* | //*[concat('#',@gml:id)=current()/ex:ft2/@xlink:href]"/>
      <assert test="not((ex:attDT1/*)[count(ex:attDT2/*/ex:value) != 1])">test_forAll_with_datatypes: Each value of attDT1 shall have exactly one attDT2.value.</assert>
      <assert test="not((ex:attDT1/*)[count(ex:attDT2/*/ex:attDT3/*/ex:value) != 1])">test_forAll_with_datatypes_long_property_path_in_iterator: Each value of attDT1 shall have exactly one attDT2.attDT3.value.</assert>
      <assert test="not(($A/ex:ft3/* | //*[concat('#',@gml:id)=$A/ex:ft3/@xlink:href])[not(local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty')])">test_forAll_with_featureTypes: Each value of ft2.ft3 shall be of kind FT3Sub.</assert>
      <assert test="not((ex:ft2/* | //*[concat('#',@gml:id)=current()/ex:ft2/@xlink:href])[count((ex:ft3/* | //*[concat('#',@gml:id)=ex:ft3/@xlink:href])[local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty']) &lt; 1 or count((ex:ft3/* | //*[concat('#',@gml:id)=ex:ft3/@xlink:href])[local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty']) &gt; 2])">test_forAll_with_featureTypes_contextDependent_multiplicity_1to2: For each value of ft2, the number of values of ft3 of type FT3Sub shall be 1 or 2.</assert>
      <assert test="not(($A/ex:ft3/* | //*[concat('#',@gml:id)=$A/ex:ft3/@xlink:href])[count(ex:ft4/* | //*[concat('#',@gml:id)=ex:ft4/@xlink:href]) != 1])">test_forAll_with_featureTypes_multiplicity: Multiplicity of values of ft4 in context of ft2 &gt; ft3 shall be 1.</assert>
      <assert test="not((ex:ft2/* | //*[concat('#',@gml:id)=current()/ex:ft2/@xlink:href])[count((ex:ft3/* | //*[concat('#',@gml:id)=ex:ft3/@xlink:href])[local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty']) != 1])">test_forAll_with_featureTypes_multiplicityOfSpecificValueType: Multiplicity of values of ft3 with type FT3Sub in context ft2 shall be 1.</assert>
      <assert test="not((ex:ft2/* | //*[concat('#',@gml:id)=current()/ex:ft2/@xlink:href])[ex:ft3/* | //*[concat('#',@gml:id)=ex:ft3/@xlink:href] and (ex:ft3/* | //*[concat('#',@gml:id)=ex:ft3/@xlink:href])[not(local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty')]])">test_forAll_with_featureTypes_notEmpty_and_oclIsKindOf: If an ft2 has an ft3 then it must have an ft3 with value of type FT3Sub.</assert>
      <assert test="count(($A/ex:ft3/* | //*[concat('#',@gml:id)=$A/ex:ft3/@xlink:href])[local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty']) = 1">test_select_with_featureTypes_multiplicityOfSpecificValueType: Multiplicity of values of ft2.ft3 with type FT3Sub shall be 1.</assert>
      <assert test="count((ex:ft2/* | //*[concat('#',@gml:id)=current()/ex:ft2/@xlink:href])[(ex:ft3/* | //*[concat('#',@gml:id)=ex:ft3/@xlink:href])[local-name()='FT3Sub' and namespace-uri()='http://example.org/shapechange/sch/iteratorWithByReferenceProperty']]) = 1">test_select_with_featureTypes_multiplicityOfSpecificValueType_byReferenceInIterator: Multiplicity of values of ft2 that have ft3 with type FT3Sub shall be 1.</assert>
    </rule>
  </pattern>
</schema>
