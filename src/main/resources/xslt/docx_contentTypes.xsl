<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:xs="http://www.w3.org/2001/XMLSchema"
 xmlns:ooxmlct="http://schemas.openxmlformats.org/package/2006/content-types"
 xmlns="http://schemas.openxmlformats.org/package/2006/content-types"
 exclude-result-prefixes="xs ooxmlct" version="2.0">

 <xsl:output method="xml" encoding="UTF-8" indent="yes" standalone="yes"/>

 <xsl:template match="@* | node()">
  <xsl:copy>
   <xsl:apply-templates select="@* | node()"/>
  </xsl:copy>
 </xsl:template>

 <!--If <Types> has a <Default> child, but not one for Extension JPG, add new <Default> behind the last <Default> child -->
 <xsl:template
  match="/ooxmlct:Types[ooxmlct:Default and not(ooxmlct:Default[@Extension = 'jpg'])]/ooxmlct:Default[position() = last()]">
  <!-- Copy last <Default> child first. -->
  <xsl:copy>
   <xsl:apply-templates select="@* | node()"/>
  </xsl:copy>
  <!-- Then add new <Default> child -->
  <Default Extension="jpg" ContentType="image/jpeg"/>
 </xsl:template>

 <!--If <Types> does not have a <Default> child, create one -->
 <xsl:template match="/ooxmlct:Types[not(ooxmlct:Default)]">
  <xsl:copy>
   <Default Extension="jpg" ContentType="image/jpeg"/>
   <xsl:apply-templates select="@* | node()"/>
  </xsl:copy>
 </xsl:template>

</xsl:stylesheet>
