<?xml version="1.0" encoding="UTF-8"?>
<!-- (c) 2013 interactive instruments GmbH -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://www.opengis.net/gml/3.2" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink">
	<xsl:output method="xml"/>
	<xsl:param name="baseuri"/>
	<xsl:param name="level"/>
	<xsl:param name="language"/>
	<xsl:template match="/">
    	<xsl:variable name="id" select="/*/atom:id"/>
    	
            <xsl:if test="$level='0'">    
<Dictionary gml:id="Vocabulary" xsi:schemaLocation="http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd">
  <xsl:if test="/atom:feed/atom:subtitle">
  <description>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:subtitle"/>
            </xsl:call-template>
  </description>
  </xsl:if>
  <identifier codeSpace="urn:ietf:rfc:2616"><xsl:value-of select="$id"/></identifier>
  <xsl:if test="/atom:feed/atom:title">
  <name>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:title"/>
            </xsl:call-template>
  </name>
  </xsl:if>
  <remarks>Typ: Register. 
<xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/version')]">GeoInfoDok-Versionen: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/version')]"><xsl:value-of select="@label"/><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if>
<xsl:if test="/*/atom:published"><xsl:variable name="date" select="/*/atom:published"/>Veröffentlicht: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="/*/atom:updated"><xsl:variable name="date" select="/*/atom:updated"/>Aktualisiert: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/profile')]">Modellarten: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/profile')]"><xsl:value-of select="@label"/><xsl:variable name="term" select="@term"/><xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]">(mandatoryToCapture)</xsl:if><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if></remarks>
  <xsl:for-each select="/atom:feed/atom:entry">
  <xsl:sort select="atom:id"/>
  <dictionaryEntry xlink:href="{atom:id}">
  	<xsl:attribute name="xlink:title">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:title"/>
            </xsl:call-template>
            <xsl:if test="atom:summary">: 
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:summary"/>
            </xsl:call-template>
            </xsl:if>
  	</xsl:attribute>
  </dictionaryEntry>
  </xsl:for-each>
 </Dictionary>
            </xsl:if>
            
            <xsl:if test="$level='1'">
<Dictionary gml:id="{/atom:feed/atom:title}" xsi:schemaLocation="http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd">
  <xsl:if test="/atom:feed/atom:subtitle">
  <description>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:subtitle"/>
            </xsl:call-template>
  </description>
  </xsl:if>
  <identifier codeSpace="urn:ietf:rfc:2616"><xsl:value-of select="$id"/></identifier>
  <xsl:if test="/atom:feed/atom:title">
  <name>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:title"/>
            </xsl:call-template>
  </name>
  </xsl:if>
  <remarks><xsl:choose>
<xsl:when test="/atom:feed/atom:category[@scheme=concat($baseuri,'/type')]">Typ: <xsl:value-of select="/atom:feed/atom:category[@scheme=concat($baseuri,'/type')]/@label"/>.
</xsl:when>
<xsl:otherwise>Typ: Codeliste / Enumeration.
</xsl:otherwise>
</xsl:choose>
<xsl:if test="/atom:feed/atom:category[@scheme=concat($baseuri,'/status')]">Status: <xsl:value-of select="/atom:feed/atom:category[@scheme=concat($baseuri,'/status')]/@label"/>.
</xsl:if>
<xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/version')]">GeoInfoDok-Versionen: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/version')]"><xsl:value-of select="@label"/><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if>
<xsl:if test="/*/atom:published"><xsl:variable name="date" select="/*/atom:published"/>Veröffentlicht: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="/*/atom:updated"><xsl:variable name="date" select="/*/atom:updated"/>Aktualisiert: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:choose>
<xsl:when test="/*/atom:category[@scheme=concat($baseuri,'/profile')]">Modellarten: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/profile')]"><xsl:value-of select="@label"/><xsl:variable name="term" select="@term"/><xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]">(mandatoryToCapture)</xsl:if><xsl:text> </xsl:text></xsl:for-each>.
</xsl:when>
<xsl:otherwise>Modellarten: alle.
</xsl:otherwise>
</xsl:choose></remarks>
  <xsl:for-each select="/atom:feed/atom:entry">
  <xsl:sort select="atom:id"/>
                  <xsl:variable name="lid" select="substring-after(substring-after(atom:id,concat($baseuri,'/')),'/')"/>
                  <xsl:variable name="code">
                  <xsl:choose>
                  <xsl:when test="contains($lid,'-1')">
	                  <xsl:value-of select="substring-before($lid,'-1')"/>
                  </xsl:when>
                  <xsl:otherwise>
	                  <xsl:value-of select="$lid"/>
                  </xsl:otherwise>
                  </xsl:choose>
				  </xsl:variable>
  <dictionaryEntry>
  <Definition gml:id="{/atom:feed/atom:title}_{$lid}">
  <xsl:if test="atom:summary">
  <description>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:summary"/>
            </xsl:call-template>
  </description>
  </xsl:if>
  <identifier codeSpace="urn:ietf:rfc:2616"><xsl:value-of select="atom:id"/></identifier>
  <xsl:if test="atom:title">
  <name>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="substring-after(atom:title,'/')"/>
            </xsl:call-template>
  </name>
  </xsl:if>
  <xsl:if test="$code and $code!=substring-after(atom:title,'/')">
  <name>
  	<xsl:value-of select="$code"/>
  </name>
  </xsl:if>
  </Definition>
  </dictionaryEntry>
  </xsl:for-each>
 </Dictionary>
            </xsl:if>

            <xsl:if test="$level='2'">
                  <xsl:variable name="path" select="substring-after($id,concat($baseuri,'/'))"/>
                  <xsl:variable name="lid" select="substring-after($path,'/')"/>
                  <xsl:variable name="code">
                  <xsl:choose>
                  <xsl:when test="contains($lid,'-1')">
	                  <xsl:value-of select="substring-before($lid,'-1')"/>
                  </xsl:when>
                  <xsl:otherwise>
	                  <xsl:value-of select="$lid"/>
                  </xsl:otherwise>
                  </xsl:choose>
				  </xsl:variable>
<Definition gml:id="{concat(substring-before($path,'/'),'_',$lid)}" xsi:schemaLocation="http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd">
  <xsl:if test="/atom:entry/atom:summary">
  <description>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:entry/atom:summary"/>
            </xsl:call-template>
  </description>
  </xsl:if>
  <identifier codeSpace="urn:ietf:rfc:2616"><xsl:value-of select="$id"/></identifier>
  <xsl:if test="/atom:entry/atom:title">
  <name>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="substring-after(/atom:entry/atom:title,'/')"/>
            </xsl:call-template>
  </name>
  </xsl:if>
  <xsl:if test="$code and $code!=substring-after(/atom:entry/atom:title,'/')">
  <name>
  	<xsl:value-of select="$code"/>
  </name>
  </xsl:if>
  <remarks>Typ: Werteart.
<xsl:if test="/atom:entry/atom:category[@scheme=concat($baseuri,'/status')]">Status: <xsl:value-of select="/atom:entry/atom:category[@scheme=concat($baseuri,'/status')]/@label"/>.
</xsl:if>
<xsl:if test="/*/atom:link[@rel='predecessor']">Vorgänger: <xsl:value-of select="/*/atom:link[@rel='predecessor']/@href"/>.
</xsl:if>
<xsl:if test="/*/atom:link[@rel='successor']">Nachfolger: <xsl:value-of select="/*/atom:link[@rel='successor']/@href"/>.
</xsl:if>
<xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/version')]">GeoInfoDok-Versionen: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/version')]"><xsl:value-of select="@label"/><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if>
<xsl:if test="/*/atom:published"><xsl:variable name="date" select="/*/atom:published"/>Veröffentlicht: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="/*/atom:updated"><xsl:variable name="date" select="/*/atom:updated"/>Aktualisiert: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:choose>
<xsl:when test="/*/atom:category[@scheme=concat($baseuri,'/profile')]">Modellarten: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/profile')]"><xsl:value-of select="@label"/><xsl:variable name="term" select="@term"/><xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]">(Grunddatenbestand)</xsl:if><xsl:text> </xsl:text></xsl:for-each>.
</xsl:when>
<xsl:otherwise>Modellarten: alle.
</xsl:otherwise>
</xsl:choose></remarks>
  </Definition>
            </xsl:if>
	</xsl:template>


<xsl:template name="replace_ins">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string,'[[ins]]')">
      <xsl:call-template name="replace_del">
        <xsl:with-param name="string" select="substring-before($string,'[[ins]]')"/>
      </xsl:call-template>
      <xsl:value-of select="substring-before(substring-after($string,'[[ins]]'),'[[/ins]]')"/>
      <xsl:call-template name="replace_ins">
        <xsl:with-param name="string" select="substring-after($string,'[[/ins]]')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="replace_del">
        <xsl:with-param name="string" select="$string"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
<xsl:template name="replace_del">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string,'[[del]]')">
      <xsl:value-of select="substring-before($string,'[[del]]')"/>
      <xsl:value-of select="substring-before(substring-after($string,'[[del]]'),'[[/del]]')"/>
      <xsl:call-template name="replace_del">
        <xsl:with-param name="string" select="substring-after($string,'[[/del]]')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
</xsl:stylesheet>
