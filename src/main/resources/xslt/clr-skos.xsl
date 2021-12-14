<?xml version="1.0" encoding="UTF-8"?>
<!-- (c) 2013 interactive instruments GmbH -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:atom="http://www.w3.org/2005/Atom" xmlns="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:cl="http://shapechange.net/tmp/skos#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:dc="http://purl.org/dc/elements/1.1/">
	<xsl:output method="xml"/>
	<xsl:param name="baseuri"/>
	<xsl:param name="level"/>
	<xsl:param name="language"/>
	<xsl:template match="/">
    	<xsl:variable name="id" select="/*/atom:id"/>
    	
            <xsl:if test="$level='1'">
<RDF>
<skos:ConceptScheme rdf:about="{$id}">
  <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
  <dc:identifier><xsl:value-of select="$id"/></dc:identifier>
  <dc:language>de</dc:language>
  <xsl:if test="/atom:feed/atom:subtitle">
  <skos:definition rdf:datatype="http://www.w3.org/2001/XMLSchema#string" xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:subtitle"/>
            </xsl:call-template>
  </skos:definition>
  </xsl:if>
  <dc:source>Arbeitsgemeinschaft der Vermessungsverwaltungen der Länder der Bundesrepublik Deutschland (AdV)</dc:source>
  <xsl:if test="/atom:feed/atom:title">
  <skos:prefLabel xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:title"/>
            </xsl:call-template>
  </skos:prefLabel>
  <dc:title xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:title"/>
            </xsl:call-template>
  </dc:title>
  </xsl:if>
  <skos:scopeNote xml:lang="{$language}"><xsl:choose>
<xsl:when test="/atom:feed/atom:category[@scheme=concat($baseuri,'/type')]">Typ: <xsl:value-of select="/atom:feed/atom:category[@scheme=concat($baseuri,'/type')]/@label"/>.</xsl:when>
<xsl:otherwise>Typ: Codeliste / Enumeration.
</xsl:otherwise>
</xsl:choose>
<xsl:choose>
<xsl:when test="/*/atom:category[@scheme=concat($baseuri,'/profile')]">Modellarten: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/profile')]"><xsl:value-of select="@label"/><xsl:variable name="term" select="@term"/><xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]">(Grunddatenbestand)</xsl:if><xsl:text> </xsl:text></xsl:for-each>.
</xsl:when>
<xsl:otherwise>Modellarten: alle.
</xsl:otherwise>
</xsl:choose></skos:scopeNote>
  <skos:historyNote xml:lang="{$language}"><xsl:if test="/atom:feed/atom:category[@scheme=concat($baseuri,'/status')]">Status: <xsl:value-of select="/atom:feed/atom:category[@scheme=concat($baseuri,'/status')]/@label"/>.
</xsl:if>
<xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/version')]">GeoInfoDok-Versionen: <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/version')]"><xsl:value-of select="@label"/><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if>
<xsl:if test="/*/atom:published"><xsl:variable name="date" select="/*/atom:published"/>Veröffentlicht: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="/*/atom:updated"><xsl:variable name="date" select="/*/atom:updated"/>Aktualisiert: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if></skos:historyNote>
</skos:ConceptScheme>
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
<skos:Concept rdf:about="{atom:id}">
  <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
  <dc:identifier><xsl:value-of select="atom:id"/></dc:identifier>
  <dc:language>de</dc:language>
  <xsl:if test="atom:summary">
  <skos:definition rdf:datatype="http://www.w3.org/2001/XMLSchema#string" xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:summary"/>
            </xsl:call-template>
  </skos:definition>
  </xsl:if>
  <dc:source>Arbeitsgemeinschaft der Vermessungsverwaltungen der Länder der Bundesrepublik Deutschland (AdV)</dc:source>
  <xsl:if test="atom:title">
  <skos:prefLabel xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:title"/>
            </xsl:call-template>
  </skos:prefLabel>
  <dc:title xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:title"/>
            </xsl:call-template>
  </dc:title>
  </xsl:if>
  <xsl:if test="$code">
  <skos:notation><xsl:value-of select="$code"/></skos:notation>
  </xsl:if>
  <skos:topConceptOf rdf:resource="{$id}"/>
  <skos:scopeNote xml:lang="{$language}">Typ: Werteart.
<xsl:choose>
<xsl:when test="atom:category[@scheme=concat($baseuri,'/profile')]">Modellarten: <xsl:for-each select="atom:category[@scheme=concat($baseuri,'/profile')]"><xsl:value-of select="@label"/><xsl:variable name="term" select="@term"/><xsl:if test="atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]">(Grunddatenbestand)</xsl:if><xsl:text> </xsl:text></xsl:for-each>.
</xsl:when>
<xsl:otherwise>Modellarten: alle.
</xsl:otherwise>
</xsl:choose></skos:scopeNote>
  <skos:historyNote xml:lang="{$language}"><xsl:if test="atom:category[@scheme=concat($baseuri,'/status')]">Status: <xsl:value-of select="atom:category[@scheme=concat($baseuri,'/status')]/@label"/>.
</xsl:if>
<xsl:if test="atom:category[@scheme=concat($baseuri,'/version')]">GeoInfoDok-Versionen: <xsl:for-each select="atom:category[@scheme=concat($baseuri,'/version')]"><xsl:value-of select="@label"/><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if>
<xsl:if test="atom:published"><xsl:variable name="date" select="atom:published"/>Veröffentlicht: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="atom:updated"><xsl:variable name="date" select="atom:updated"/>Aktualisiert: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if></skos:historyNote>
</skos:Concept>
  </xsl:for-each>
</RDF>
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
<RDF>
<skos:Concept rdf:about="{/atom:entry/atom:id}">
  <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
  <dc:identifier><xsl:value-of select="/atom:entry/atom:id"/></dc:identifier>
  <dc:language>de</dc:language>
  <xsl:if test="/atom:entry/atom:summary">
  <skos:definition rdf:datatype="http://www.w3.org/2001/XMLSchema#string" xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:entry/atom:summary"/>
            </xsl:call-template>
  </skos:definition>
  </xsl:if>
  <dc:source>Arbeitsgemeinschaft der Vermessungsverwaltungen der Länder der Bundesrepublik Deutschland (AdV)</dc:source>
  <xsl:if test="/atom:entry/atom:title">
  <skos:prefLabel xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="substring-after(/atom:entry/atom:title,'/')"/>
            </xsl:call-template>
  </skos:prefLabel>
  <dc:title xml:lang="{$language}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="substring-after(/atom:entry/atom:title,'/')"/>
            </xsl:call-template>
  </dc:title>
  </xsl:if>
  <xsl:if test="$code">
  <skos:notation><xsl:value-of select="$code"/></skos:notation>
  </xsl:if>
  <skos:topConceptOf rdf:resource="{substring-before($id,concat('/',$lid))}"/>
  <skos:scopeNote xml:lang="{$language}">Typ: Werteart.
<xsl:choose>
<xsl:when test="/atom:entry/atom:category[@scheme=concat($baseuri,'/profile')]">Modellarten: <xsl:for-each select="/atom:entry/atom:category[@scheme=concat($baseuri,'/profile')]"><xsl:value-of select="@label"/><xsl:variable name="term" select="@term"/><xsl:if test="/atom:entry/atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]">(Grunddatenbestand)</xsl:if><xsl:text> </xsl:text></xsl:for-each>.
</xsl:when>
<xsl:otherwise>Modellarten: alle.
</xsl:otherwise>
</xsl:choose></skos:scopeNote>
  <skos:historyNote xml:lang="{$language}"><xsl:if test="/atom:entry/atom:category[@scheme=concat($baseuri,'/status')]">Status: <xsl:value-of select="/atom:entry/atom:category[@scheme=concat($baseuri,'/status')]/@label"/>.
</xsl:if>
<xsl:if test="/atom:entry/atom:link[@rel='predecessor']">Vorgänger: <xsl:value-of select="/atom:entry/atom:link[@rel='predecessor']/@href"/>.
</xsl:if>
<xsl:if test="/atom:entry/atom:link[@rel='successor']">Nachfolger: <xsl:value-of select="/atom:entry/atom:link[@rel='successor']/@href"/>.
</xsl:if>
<xsl:if test="/atom:entry/atom:category[@scheme=concat($baseuri,'/version')]">GeoInfoDok-Versionen: <xsl:for-each select="/atom:entry/atom:category[@scheme=concat($baseuri,'/version')]"><xsl:value-of select="@label"/><xsl:text> </xsl:text></xsl:for-each>.
</xsl:if>
<xsl:if test="/atom:entry/atom:published"><xsl:variable name="date" select="/atom:entry/atom:published"/>Veröffentlicht: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if>
<xsl:if test="/atom:entry/atom:updated"><xsl:variable name="date" select="/atom:entry/atom:updated"/>Aktualisiert: <xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/>.
</xsl:if></skos:historyNote>
</skos:Concept>
</RDF>
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
