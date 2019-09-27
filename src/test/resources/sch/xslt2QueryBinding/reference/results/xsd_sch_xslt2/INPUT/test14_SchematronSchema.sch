<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 14 - Logic'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex14" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s14"/>
  <pattern>
    <rule context="ex14:TS14_FT">
      <let name="A" value="current()/ex14:att1"/>
      <let name="B" value="current()/ex14:att2"/>
      <let name="C" value="current()/ex14:att3"/>
      <let name="D" value="current()/ex14:att4"/>
      <let name="E" value="current()/ex14:att5"/>
      <let name="F" value="current()/ex14:att6"/>
      <let name="G" value="current()/ex14:att7"/>
      <let name="H" value="current()/ex14:att8"/>
      <let name="I" value="current()/ex14:att9"/>
      <assert test="$A = true() and $B = true()">ts14_ft_constraint1: (att1 = true) and (att2 = true)</assert>
      <assert test="$C = true() or $D = true()">ts14_ft_constraint2: (att3 = true) or (att4 = true)</assert>
      <assert test="($E = true()) != ($F = true())">ts14_ft_constraint3: att5 = true xor att6 = true</assert>
      <assert test="$G != true() or $H = true()">ts14_ft_constraint4: (att7 = true) implies (att8 = true)</assert>
      <assert test="$I != true()">ts14_ft_constraint5: not(att9 = true)</assert>
    </rule>
  </pattern>
</schema>
