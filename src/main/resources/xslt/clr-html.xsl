<?xml version="1.0" encoding="iso-8859-1"?>
<!-- (c) 2013 interactive instruments GmbH -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:atom="http://www.w3.org/2005/Atom">
	<xsl:output method="html"/>
	<xsl:param name="baseuri"/>
	<xsl:param name="level"/>
	<xsl:param name="language"/>
	<xsl:template match="/">
    	<xsl:variable name="id" select="/*/atom:id"/>
	
<html>
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	  <title><xsl:value-of select="/*/atom:title"/></title>
      <link rel="stylesheet" type="text/css" href="{$baseuri}/r/css/style.css"/>
      <script type="text/javascript" src="{$baseuri}/r/js/jquery-1.9.1.js"></script>
   </head>
   <body>
      <header>
    	 <div id="topmenu_container"><script>$("#topmenu_container").load("<xsl:value-of select="$baseuri"/>/r/html/header.html");</script></div>
         <div id="breadctxt_container">
          <div id="breadctxt">
            <a href="http://www.adv-online.de/">AdV</a>&#x00A0;
            &gt;
            <a href="http://www.adv-online.de/icc/extdeu/broker.jsp?uMen=4b370024-769d-8801-e1f3-351ec0023010">GeoInfoDok</a>&#x00A0;
            <!--
            &gt;
            <a href="http://portele.de/adv">AdV-Registry</a>&#x00A0;
            -->
            &gt;
            <a href="{$baseuri}"><xsl:choose>
            <xsl:when test="$language='de'">Codelisten und Enumerationen</xsl:when>
            <xsl:otherwise>Code Lists and Enumerations</xsl:otherwise>
            </xsl:choose></a>&#x00A0;
            <xsl:if test="$level='1'">
            &gt;
            <a href="{$baseuri}/{/atom:feed/atom:title}">
            <xsl:call-template name="replace_ins_current">
              <xsl:with-param name="string" select="/atom:feed/atom:title"/>
            </xsl:call-template>
            </a>&#x00A0;
            </xsl:if>
            <xsl:if test="$level='2'">
            &gt;
            <a href="{$baseuri}/{substring-before(/atom:entry/atom:title,'/')}">
            <xsl:call-template name="replace_ins_current">
              <xsl:with-param name="string" select="substring-before(/atom:entry/atom:title,'/')"/>
            </xsl:call-template>
            </a>&#x00A0;
            &gt;
            <a href="{$id}">
            <xsl:call-template name="replace_ins_current">
              <xsl:with-param name="string" select="substring-after(/atom:entry/atom:title,'/')"/>
            </xsl:call-template>
            </a>&#x00A0;
            </xsl:if>
          </div>
         </div>
      </header>
      <article class="exp">
              <xsl:choose>
                <xsl:when test="/*/atom:category[@scheme=concat($baseuri,'/version') and starts-with(@term,'6')] and not /*/atom:category[@scheme=concat($baseuri,'/version') and (starts-with(@term,'7') or starts-with(@term,'0'))]">
                  <xsl:attribute name="bgcolor">#ffe6e6</xsl:attribute>
                </xsl:when>
                <xsl:when test="/*/atom:category[@scheme=concat($baseuri,'/version') and (starts-with(@term,'7') or starts-with(@term,'0'))] and not /*/atom:category[@scheme=concat($baseuri,'/version') and starts-with(@term,'6')]">
                  <xsl:attribute name="bgcolor">#e6ffe6</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
              </xsl:choose>
		<div id='topmenu'>
		<xsl:if test="/*/atom:link[@type='application/atom+xml']">
			<a href="{/*/atom:link[@type='application/atom+xml']/@href}" title='ATOM-Feed'>ATOM</a>&#x00A0;
		</xsl:if>
		<xsl:if test="/*/atom:link[@type='application/gml+xml;version=3.2']">
			<a href="{/*/atom:link[@type='application/gml+xml;version=3.2']/@href}" title='GML-Dictionary'>GML</a>&#x00A0;
		</xsl:if>
		<xsl:if test="/*/atom:link[@type='application/rdf+xml']">
			<a href="{/*/atom:link[@type='application/rdf+xml']/@href}" title='SKOS-ConceptScheme'>SKOS</a>&#x00A0;
		</xsl:if>
		</div>
        <h2>
            <xsl:if test="$level='0' or $level='1'">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:feed/atom:title"/>
            </xsl:call-template>
            </xsl:if>
            <xsl:if test="$level='2'">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="substring-after(/atom:entry/atom:title,'/')"/>
            </xsl:call-template>
            </xsl:if>
        </h2>
         <xsl:if test="/*/atom:subtitle|/*/atom:summary">
	         <p>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/*/atom:subtitle|/*/atom:summary"/>
            </xsl:call-template>
	         </p>
         </xsl:if>
         <p><span>ID:</span><a href="{$id}"><xsl:value-of select="$id"/></a></p>
            <xsl:if test="$level='0'">
	         <p><span>Typ:</span>Register</p>
            </xsl:if>
            <xsl:if test="$level='1'">
         <xsl:choose>
         <xsl:when test="/atom:feed/atom:category[@scheme=concat($baseuri,'/type')]">
	         <p><span>Typ:</span><xsl:value-of select="/atom:feed/atom:category[@scheme=concat($baseuri,'/type')]/@label"/></p>
         </xsl:when>
         <xsl:otherwise>
	         <p><span>Typ:</span>Codeliste / Enumeration</p>
         </xsl:otherwise>
         </xsl:choose>
         <xsl:if test="/atom:feed/atom:category[@scheme=concat($baseuri,'/status')]">
	         <p><span>Status:</span>
              <xsl:value-of select="/atom:feed/atom:category[@scheme=concat($baseuri,'/status')]/@label"/>
	         </p>
         </xsl:if>
            </xsl:if>
            <xsl:if test="$level='2'">
	         <p><span>Typ:</span>Werteart</p>
	         <p><span>Code:</span>
                  <xsl:variable name="code" select="substring-after($id,concat($baseuri,'/',substring-before(/atom:entry/atom:title,'/'),'/'))"/>
                  <xsl:choose>
                  <xsl:when test="contains($code,'-1')">
	                  <xsl:value-of select="substring-before($code,'-1')"/>
                  </xsl:when>
                  <xsl:otherwise>
	                  <xsl:value-of select="$code"/>
                  </xsl:otherwise>
                  </xsl:choose>
	         </p>
         <xsl:if test="/atom:entry/atom:category[@scheme=concat($baseuri,'/status')]">
	         <p><span>Status:</span>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="/atom:entry/atom:category[@scheme=concat($baseuri,'/status')]/@label"/>
            </xsl:call-template>
	         </p>
         </xsl:if>
            </xsl:if>
         <xsl:if test="/*/atom:link[@rel='predecessor']">
	         <p><span>Vorgänger:</span>
				<a href="{/*/atom:link[@rel='predecessor']/@href}" title='Vorgänger'><xsl:value-of select="/*/atom:link[@rel='predecessor']/@href"/></a>
	         </p>
         </xsl:if>
         <xsl:if test="/*/atom:link[@rel='successor']">
	         <p><span>Nachfolger:</span>
				<a href="{/*/atom:link[@rel='successor']/@href}" title='Vorgänger'><xsl:value-of select="/*/atom:link[@rel='successor']/@href"/></a>
	         </p>
         </xsl:if>
         <xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/version')]">
	         <p><span>GeoInfoDok-Versionen:</span>
	         <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/version')]">
	         <xsl:value-of select="@label"/>&#x00A0;
	         </xsl:for-each></p>
         </xsl:if>
         <xsl:if test="/*/atom:published">
         	<xsl:variable name="date" select="/*/atom:published"/>
	         <p><span>Veröffentlicht:</span><xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/></p>
         </xsl:if>
         <xsl:if test="/*/atom:updated">
         	<xsl:variable name="date" select="/*/atom:updated"/>
	         <p><span>Aktualisiert:</span><xsl:value-of select="substring($date,9,2)"/>.<xsl:value-of select="substring($date,6,2)"/>.<xsl:value-of select="substring($date,1,4)"/></p>
         </xsl:if>
         <xsl:choose>
         <xsl:when test="/*/atom:category[@scheme=concat($baseuri,'/profile')]">
	         <p><span>Modellarten:</span>
	         <xsl:for-each select="/*/atom:category[@scheme=concat($baseuri,'/profile')]">
	         <xsl:value-of select="@label"/>&#x00a0;
	         <xsl:variable name="term" select="@term"/>
	         <xsl:if test="/*/atom:category[@scheme=concat($baseuri,'/mandatoryToCapture') and @term=$term]"><small>(Grunddatenbestand)</small></xsl:if>
	         <br/><span>&#x00a0;</span>
	         </xsl:for-each></p>
         </xsl:when>
         <xsl:when test="$level='1' or $level='2'">
	         <p><span>Modellarten:</span>alle</p>
         </xsl:when>
         </xsl:choose>
         <br/>
            <xsl:if test="$level='0'">
<h3>Codelisten und Enumerationen</h3>
         <div class="tb_cont">
            <table>
               <tr>
                  <th>Name</th>
                  <th>Typ</th>
                  <th>Versionen</th>
                  <th>Modellarten</th>
               </tr>
               <xsl:for-each select="/atom:feed/atom:entry">
               <xsl:sort select="atom:id"/>
               <tr>
              <xsl:choose>
                <xsl:when test="atom:category[@scheme=concat($baseuri,'/version') and starts-with(@term,'6')] and count(atom:category[@scheme=concat($baseuri,'/version') and (starts-with(@term,'7') or starts-with(@term,'0'))])=0">
                  <xsl:attribute name="bgcolor">#ffe6e6</xsl:attribute>
                </xsl:when>
                <xsl:when test="atom:category[@scheme=concat($baseuri,'/version') and (starts-with(@term,'7') or starts-with(@term,'0'))] and count(atom:category[@scheme=concat($baseuri,'/version') and starts-with(@term,'6')])=0">
                  <xsl:attribute name="bgcolor">#e6ffe6</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
              </xsl:choose>
                  <td><a href="{atom:id}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:title"/>
            </xsl:call-template>
                  </a>
                  <xsl:if test="atom:summary"><br/><small>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:summary"/>
            </xsl:call-template>
                  </small></xsl:if>
                  </td>
                  <td><xsl:value-of select="atom:category[@scheme=concat($baseuri,'/type')]/@label"/></td>
                  <td><xsl:for-each select="atom:category[@scheme=concat($baseuri,'/version')]/@label">
	         <xsl:value-of select="."/><br/>
	         </xsl:for-each></td>
                  <td>
         <xsl:choose>
         <xsl:when test="atom:category[@scheme=concat($baseuri,'/profile')]">
	         <xsl:for-each select="atom:category[@scheme=concat($baseuri,'/profile')]/@label">
	         <xsl:value-of select="."/><br/>
	         </xsl:for-each>
         </xsl:when>
         <xsl:otherwise>
	         alle
         </xsl:otherwise>
         </xsl:choose>
	</td>
               </tr>         
               </xsl:for-each>
            </table>
        </div>
            </xsl:if>
            <xsl:if test="$level='1'">
<h3>Wertearten</h3>
         <div class="tb_cont">
            <table>
               <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <!--<th>Versionen</th>-->
                  <th>Modellarten</th>
                  <th>Status</th>
               </tr>
               <xsl:for-each select="/atom:feed/atom:entry">
               <xsl:sort select="atom:id"/>
               <tr>
              <xsl:choose>
                <xsl:when test="atom:category[@scheme=concat($baseuri,'/version') and starts-with(@term,'6')] and count(atom:category[@scheme=concat($baseuri,'/version') and (starts-with(@term,'7') or starts-with(@term,'0'))])=0">
                  <xsl:attribute name="bgcolor">#ffe6e6</xsl:attribute>
                </xsl:when>
                <xsl:when test="atom:category[@scheme=concat($baseuri,'/version') and (starts-with(@term,'7') or starts-with(@term,'0'))] and count(atom:category[@scheme=concat($baseuri,'/version') and starts-with(@term,'6')])=0">
                  <xsl:attribute name="bgcolor">#e6ffe6</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                </xsl:otherwise>
              </xsl:choose>
                  <td>
                  <xsl:variable name="code" select="substring-after(atom:id,concat($baseuri,'/',substring-before(atom:title,'/'),'/'))"/>
                  <xsl:choose>
                  <xsl:when test="contains($code,'-1')">
	                  <xsl:value-of select="substring-before($code,'-1')"/>
                  </xsl:when>
                  <xsl:otherwise>
	                  <xsl:value-of select="$code"/>
                  </xsl:otherwise>
                  </xsl:choose>
                  </td>
                  <td><a href="{atom:id}">
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="substring-after(atom:title,'/')"/>
            </xsl:call-template>
                  </a>
                  <xsl:if test="atom:summary"><br/><small>
            <xsl:call-template name="replace_ins">
              <xsl:with-param name="string" select="atom:summary"/>
            </xsl:call-template>
                  </small></xsl:if>
                  </td>
                  <!--<td><xsl:for-each select="atom:category[@scheme=concat($baseuri,'/version')]/@label">
	         <xsl:value-of select="."/><br/>
	         </xsl:for-each></td>-->
                  <td>
         <xsl:choose>
         <xsl:when test="atom:category[@scheme=concat($baseuri,'/profile')]">
	         <xsl:for-each select="atom:category[@scheme=concat($baseuri,'/profile')]/@label">
	         <xsl:value-of select="."/><br/>
	         </xsl:for-each>
         </xsl:when>
         <xsl:otherwise>
	         alle
         </xsl:otherwise>
         </xsl:choose>
	</td>
                  <td><xsl:value-of select="atom:category[@scheme=concat($baseuri,'/status')]/@label"/></td>
               </tr>         
               </xsl:for-each>
            </table>
        </div>
            </xsl:if>
         
         
      </article>
      <footer>
      	<script>$("footer").load("<xsl:value-of select="$baseuri"/>/r/html/footer.html");</script>
      </footer>
      </body>
</html>	

	</xsl:template>
<xsl:template name="replace_ins">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string,'[[ins]]')">
      <xsl:call-template name="replace_del">
        <xsl:with-param name="string" select="substring-before($string,'[[ins]]')"/>
      </xsl:call-template>
      <ins style="background:#e6ffe6;">
        <xsl:value-of select="substring-before(substring-after($string,'[[ins]]'),'[[/ins]]')"/>
      </ins>
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
      <del style="background:#ffe6e6;">
        <xsl:value-of select="substring-before(substring-after($string,'[[del]]'),'[[/del]]')"/>
      </del>
      <xsl:call-template name="replace_del">
        <xsl:with-param name="string" select="substring-after($string,'[[/del]]')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
<xsl:template name="replace_br">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string,'&#xA;')">
      <xsl:value-of select="substring-before($string,'&#xA;')"/>
      <br/>
      <xsl:call-template name="replace_br">
        <xsl:with-param name="string" select="substring-after($string,'&#xA;')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
<xsl:template name="replace_ins_current">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string,'[[ins]]')">
      <xsl:call-template name="replace_del_current">
        <xsl:with-param name="string" select="substring-before($string,'[[ins]]')"/>
      </xsl:call-template>
      <xsl:value-of select="substring-before(substring-after($string,'[[ins]]'),'[[/ins]]')"/>
      <xsl:call-template name="replace_ins_current">
        <xsl:with-param name="string" select="substring-after($string,'[[/ins]]')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="replace_del_current">
        <xsl:with-param name="string" select="$string"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
<xsl:template name="replace_del_current">
  <xsl:param name="string"/>
  <xsl:choose>
    <xsl:when test="contains($string,'[[del]]')">
      <xsl:value-of select="substring-before($string,'[[del]]')"/>
      <xsl:value-of select="substring-before(substring-after($string,'[[del]]'),'[[/del]]')"/>
      <xsl:call-template name="replace_del_current">
        <xsl:with-param name="string" select="substring-after($string,'[[/del]]')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$string"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>
</xsl:stylesheet>
