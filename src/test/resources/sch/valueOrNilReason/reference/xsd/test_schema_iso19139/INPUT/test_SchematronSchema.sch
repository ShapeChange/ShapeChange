<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex" uri="http://example.org/shapechange/sch/valueOrNilReason"/>
  <ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
  <ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <pattern>
    <rule context="ex:DataType">
      <assert test="not(ex:dtAtt) or (count(ex:dtAtt[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:dtAtt/*)) or (count(ex:dtAtt[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:dtAtt/*) eq count(ex:dtAtt))">If there are elements for property dtAtt, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
    </rule>
    <rule context="ex:DataType/ex:dtAtt">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property dtAtt, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType1">
      <assert test="not(ex:attribute1) or (count(ex:attribute1[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute1/*)) or (count(ex:attribute1[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute1/*) eq count(ex:attribute1))">If there are elements for property attribute1, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute2) or (count(ex:attribute2[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute2/*)) or (count(ex:attribute2[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute2/*) eq count(ex:attribute2))">If there are elements for property attribute2, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute3) or (count(ex:attribute3[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute3/text())) or (count(ex:attribute3[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute3/text()) eq count(ex:attribute3))">If there are elements for property attribute3, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute4) or (count(ex:attribute4[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute4/text())) or (count(ex:attribute4[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute4/text()) eq count(ex:attribute4))">If there are elements for property attribute4, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute5) or (count(ex:attribute5[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute5/text())) or (count(ex:attribute5[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute5/text()) eq count(ex:attribute5))">If there are elements for property attribute5, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute6) or (count(ex:attribute6[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute6/text())) or (count(ex:attribute6[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute6/text()) eq count(ex:attribute6))">If there are elements for property attribute6, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:rFT1toFT2) or (count(ex:rFT1toFT2[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:rFT1toFT2/(* | @xlink:href))) or (count(ex:rFT1toFT2[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:rFT1toFT2/(* | @xlink:href)) eq count(ex:rFT1toFT2))">If there are elements for property rFT1toFT2, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:rFT1toObjectType) or (count(ex:rFT1toObjectType[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:rFT1toObjectType/(* | @xlink:href))) or (count(ex:rFT1toObjectType[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:rFT1toObjectType/(* | @xlink:href)) eq count(ex:rFT1toObjectType))">If there are elements for property rFT1toObjectType, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
    </rule>
    <rule context="ex:FeatureType3">
      <assert test="not(ex:attribute1) or (count(ex:attribute1[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute1/*)) or (count(ex:attribute1[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute1/*) eq count(ex:attribute1))">If there are elements for property attribute1, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute2) or (count(ex:attribute2[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute2/*)) or (count(ex:attribute2[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute2/*) eq count(ex:attribute2))">If there are elements for property attribute2, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute3) or (count(ex:attribute3[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute3/text())) or (count(ex:attribute3[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute3/text()) eq count(ex:attribute3))">If there are elements for property attribute3, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute4) or (count(ex:attribute4[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute4/text())) or (count(ex:attribute4[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute4/text()) eq count(ex:attribute4))">If there are elements for property attribute4, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute5) or (count(ex:attribute5[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute5/text())) or (count(ex:attribute5[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute5/text()) eq count(ex:attribute5))">If there are elements for property attribute5, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:attribute6) or (count(ex:attribute6[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:attribute6/text())) or (count(ex:attribute6[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:attribute6/text()) eq count(ex:attribute6))">If there are elements for property attribute6, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:rFT1toFT2) or (count(ex:rFT1toFT2[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:rFT1toFT2/(* | @xlink:href))) or (count(ex:rFT1toFT2[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:rFT1toFT2/(* | @xlink:href)) eq count(ex:rFT1toFT2))">If there are elements for property rFT1toFT2, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:rFT1toObjectType) or (count(ex:rFT1toObjectType[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:rFT1toObjectType/(* | @xlink:href))) or (count(ex:rFT1toObjectType[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:rFT1toObjectType/(* | @xlink:href)) eq count(ex:rFT1toObjectType))">If there are elements for property rFT1toObjectType, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:attribute1">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property attribute1, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:attribute1">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property attribute1, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:attribute2">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property attribute2, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:attribute2">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property attribute2, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:attribute3">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute3, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:attribute3">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute3, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:attribute4">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute4, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:attribute4">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute4, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:attribute5">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute5, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:attribute5">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute5, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:attribute6">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute6, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:attribute6">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property attribute6, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:rFT1toFT2">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property rFT1toFT2, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:rFT1toFT2">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property rFT1toFT2, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType1/ex:rFT1toObjectType">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property rFT1toObjectType, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType3/ex:rFT1toObjectType">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property rFT1toObjectType, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType2">
      <assert test="not(ex:propCharacterString) or (count(ex:propCharacterString[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:propCharacterString/*)) or (count(ex:propCharacterString[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:propCharacterString/*) eq count(ex:propCharacterString))">If there are elements for property propCharacterString, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:propDataType) or (count(ex:propDataType[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:propDataType/*)) or (count(ex:propDataType[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:propDataType/*) eq count(ex:propDataType))">If there are elements for property propDataType, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:propEnumeration) or (count(ex:propEnumeration[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:propEnumeration/*)) or (count(ex:propEnumeration[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:propEnumeration/*) eq count(ex:propEnumeration))">If there are elements for property propEnumeration, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:propInteger) or (count(ex:propInteger[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:propInteger/*)) or (count(ex:propInteger[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:propInteger/*) eq count(ex:propInteger))">If there are elements for property propInteger, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:propUnion) or (count(ex:propUnion[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:propUnion/*)) or (count(ex:propUnion[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:propUnion/*) eq count(ex:propUnion))">If there are elements for property propUnion, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:rFT2toFT1) or (count(ex:rFT2toFT1[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:rFT2toFT1/(* | @xlink:href))) or (count(ex:rFT2toFT1[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:rFT2toFT1/(* | @xlink:href)) eq count(ex:rFT2toFT1))">If there are elements for property rFT2toFT1, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
    </rule>
    <rule context="ex:FeatureType2/ex:propCharacterString">
      <assert test="not(@*:nilReason) or @*:nilReason = ('xxx', 'yyy')">If a nil reason is given for property propCharacterString, then it needs to be one of: 'xxx', 'yyy'</assert>
    </rule>
    <rule context="ex:FeatureType2/ex:propDataType">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property propDataType, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType2/ex:propEnumeration">
      <assert test="not(@*:nilReason) or @*:nilReason = ('bar', 'foo')">If a nil reason is given for property propEnumeration, then it needs to be one of: 'bar', 'foo'</assert>
    </rule>
    <rule context="ex:FeatureType2/ex:propInteger">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property propInteger, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType2/ex:propUnion">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property propUnion, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:FeatureType2/ex:rFT2toFT1">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property rFT2toFT1, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:Union">
      <assert test="not(ex:optionA) or (count(ex:optionA[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:optionA/*)) or (count(ex:optionA[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:optionA/*) eq count(ex:optionA))">If there are elements for property optionA, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
      <assert test="not(ex:optionB) or (count(ex:optionB[@xsi:nil='true' and @*:nilReason]) eq 1 and not(ex:optionB/*)) or (count(ex:optionB[@xsi:nil='true' or @*:nilReason]) eq 0 and count(ex:optionB/*) eq count(ex:optionB))">If there are elements for property optionB, then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values</assert>
    </rule>
    <rule context="ex:Union/ex:optionA">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property optionA, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
    <rule context="ex:Union/ex:optionB">
      <assert test="not(@*:nilReason) or @*:nilReason = ('a', 'aa', 'bbb', 'unknown')">If a nil reason is given for property optionB, then it needs to be one of: 'a', 'aa', 'bbb', 'unknown'</assert>
    </rule>
  </pattern>
</schema>
