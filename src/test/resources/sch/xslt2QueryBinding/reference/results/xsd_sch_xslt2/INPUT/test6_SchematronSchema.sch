<?xml version="1.0" encoding="UTF-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <title>Schematron constraints for schema 'Test Schema 6 - Comparison'</title>
  <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron"/>
  <ns prefix="ex6" uri="http://example.org/shapechange/sch/xslt2QueryBinding/s6"/>
  <pattern>
    <rule context="ex6:TS6_FT">
      <let name="A" value="current()/ex6:att1"/>
      <let name="B" value="current()/ex6:att2"/>
      <let name="C" value="current()/ex6:att3"/>
      <let name="D" value="current()/ex6:att4"/>
      <let name="E" value="current()/ex6:att5"/>
      <let name="F" value="current()/ex6:att6"/>
      <assert test="$A = 1">ts6_ft_constraint1: att1 = 1</assert>
      <assert test="$B != 1">ts6_ft_constraint2: att2 &lt;&gt; 1</assert>
      <assert test="$C &gt; 1">ts6_ft_constraint3: att3 &gt; 1</assert>
      <assert test="$D &gt;= 1">ts6_ft_constraint4: att4 &gt;= 1</assert>
      <assert test="$E &lt; 1">ts6_ft_constraint5: att5 &lt; 1</assert>
      <assert test="$F &lt;= 1">ts6_ft_constraint6: att6 &lt;= 1</assert>
    </rule>
  </pattern>
</schema>
