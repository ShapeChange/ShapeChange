<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- (c) 2001-2016 interactive instruments GmbH, Bonn -->
  
  <!-- =============== -->
  <!-- Output settings -->
  <!-- =============== -->
 <!-- NOTE: Use XML attribute 'encoding' to change the output encoding. Example: encoding="iso-8859-1" -->
  <xsl:output method="html" indent="no"/>
  
  <!-- ================= -->
  <!-- Catalogue content -->
  <!-- ================= -->
  <xsl:key name="modelElement" match="/*/*[@id]" use="@id"/>
  
  <!-- ========== -->
  <!-- Parameters -->
  <!-- ========== -->
  <!-- Set the similarly named targetParameter to 'true' to prevent alphabetic sorting of properties -->
  <xsl:param name="noAlphabeticSortingForProperties">false</xsl:param>
  <!-- Name of the logo to include in the catalogue. May be empty (then do not include a logo). -->
  <xsl:param name="logoFileName"/>
 
  <!-- ======================== -->
  <!-- Transformation templates -->
  <!-- ======================== -->
  <xsl:template match="/">
    <html>
      <head>
        <title> Objektartenkatalog <xsl:value-of select="FeatureCatalogue/name"
            disable-output-escaping="yes"/>
        </title>
        <style type="text/css">
          body
          {
              background-color:#f4f6fe;
          }
          h1
          {
              font-family:Arial, Helvetica, sans-serif;
              font-size:24px;
              color:#151B8D;
              text-align:center;
          }
          h2,
          h3,
          h4
          {
              font-family:Arial, Helvetica, sans-serif;
              color:#151B8D;
          }
          a:link
          {
              color:#325595;
              text-decoration:none;
              border-bottom:1px dotted;
              outline:none;
          }
          a:hover
          {
              border-bottom:1px solid;
              color:#325595;
          }
          p,
          li
          {
              font-family:Arial, Helvetica, sans-serif;
              font-size:12px;
              margin-top:0px;
              margin-bottom:4px;
          }
          .rightalign
          {
              font-size:10px;
              text-align:right;
          }
          .small
          {
              font-size:10px;
          }
          table.att
          {
              font-family:Arial, Helvetica, sans-serif;
              width:100%;
              border-style:none;
              border-collapse:collapse;
              border:0px;
              padding:0px;
          }
          table.link
          {
              font-family:Arial, Helvetica, sans-serif;
              width:100%;
              border-style:none;
              border-collapse:collapse;
              border:0px;
              padding:0px;
          }
          table.overview
          {
              font-family:Arial, Helvetica, sans-serif;
              border-style:none;
              border-collapse:collapse;
              border:0px;
              padding:0px;
          }
          table
          {
              font-family:Arial, Helvetica, sans-serif;
              border-collapse:collapse;
              border:1px solid #98bf21;
              padding:2px 2px 2px 2px;
          }
          tr
          {
              vertical-align:top;
          }
          td.feature,
          td.values,
          td.overview
          {
              border:1px solid #98bf21;
              padding:2px 2px 2px 2px;
          }
          p.title2
          {
              font-size:12px;
              font-weight:bold;
              font-family:Arial, Helvetica, sans-serif;
          }
          p.title
          {
              font-size:14px;
              font-weight:bold;
              font-family:Arial, Helvetica, sans-serif;
          }</style>
      </head>
      <body>
       <xsl:choose>
        <xsl:when test="$logoFileName">
         <div style="display: inline-block;width: 100%;">
          <img src="{$logoFileName}" alt="logo" style="float:left;" />          
          <h1 style="text-align:center;"> Objektartenkatalog <xsl:value-of select="FeatureCatalogue/name"
           disable-output-escaping="yes"/>
          </h1>
         </div>
        </xsl:when>
        <xsl:otherwise>
         <h1> Objektartenkatalog <xsl:value-of select="FeatureCatalogue/name"
          disable-output-escaping="yes"/>
         </h1>
        </xsl:otherwise>
       </xsl:choose>
        <h1> Objektartenkatalog <xsl:value-of select="FeatureCatalogue/name"
            disable-output-escaping="yes"/>
        </h1>
        <p>
          <b>Version:</b>
        </p>
        <p style="margin-left:20px">
          <xsl:value-of select="FeatureCatalogue/versionNumber" disable-output-escaping="yes"/>
        </p>
        <p>
          <b>Datum:</b>
        </p>
        <p style="margin-left:20px">
          <xsl:value-of select="FeatureCatalogue/versionDate" disable-output-escaping="yes"/>
        </p>
        <p>
          <b>Anwendungsbereich:</b>
        </p>
        <xsl:for-each select="FeatureCatalogue/scope">
          <p style="margin-left:20px">
            <xsl:value-of select="." disable-output-escaping="yes"/>
          </p>
        </xsl:for-each>
        <p>
          <b>Verantwortliche Organisation:</b>
        </p>
        <p style="margin-left:20px">
          <xsl:value-of select="FeatureCatalogue/producer" disable-output-escaping="yes"/>
        </p>
        <a>
          <xsl:attribute name="name">overview</xsl:attribute>
          <h2>Liste der Objektarten</h2>
        </a>
        <table class="overview" border="0">
          <xsl:for-each select="FeatureCatalogue/Package|FeatureCatalogue/ApplicationSchema">
            <xsl:sort select="./code"/>
            <xsl:sort select="./name"/>
            <xsl:apply-templates select="." mode="overview"/>
          </xsl:for-each>
        </table>
        <xsl:for-each select="FeatureCatalogue/Package|FeatureCatalogue/ApplicationSchema">
          <xsl:sort select="./code"/>
          <xsl:sort select="./name"/>
          <xsl:apply-templates select="." mode="detail"/>
        </xsl:for-each>
        <hr/>
        <p align="center">
          <small>Dieser Objektartenkatalog wurde generiert mit <a href="http://shapechange.net"
              >ShapeChange</a></small>
        </p>
      </body>
    </html>
  </xsl:template>
  <xsl:template match="Package|ApplicationSchema" mode="overview">
    <xsl:variable name="package" select="."/>
    <xsl:if
      test="/FeatureCatalogue/FeatureType/package[attribute::idref=$package/@id]|/FeatureCatalogue/Package/parent[attribute::idref=$package/@id]">
      <div>
        <tr border="0">
          <td class="package" border="0">
            <xsl:choose>
              <xsl:when test="parent">
                <p>
                  <b>
                    <a>
                      <xsl:attribute name="name">
                        <xsl:value-of select="@id"/>
                        <xsl:text>_toc</xsl:text>
                      </xsl:attribute>
                      <xsl:text>Paket: </xsl:text>
                    </a>
                    <a>
                      <xsl:attribute name="href">#<xsl:value-of select="@id"/>
                      </xsl:attribute>
                      <xsl:value-of select="name" disable-output-escaping="yes"/>
                    </a>
                  </b>
                </p>
                <p style="margin-left:20px">
                  <xsl:text>Teil von: </xsl:text>
                  <a>
                    <xsl:attribute name="href">#<xsl:value-of select="parent/@idref"/>
                      <xsl:text>_toc</xsl:text>
                    </xsl:attribute>
                    <!-- <xsl:value-of select="//*[@id=$package/parent/@idref]/name"
                      disable-output-escaping="yes"/> -->
                    <xsl:value-of select="key('modelElement',$package/parent/@idref)/name"
                      disable-output-escaping="yes"/>
                  </a>
                </p>
              </xsl:when>
              <xsl:otherwise>
                <p>
                  <b>
                    <a>
                      <xsl:attribute name="name">
                        <xsl:value-of select="@id"/>
                        <xsl:text>_toc</xsl:text>
                      </xsl:attribute>
                      <xsl:text>Anwendungsschema: </xsl:text>
                    </a>
                    <a>
                      <xsl:attribute name="href">#<xsl:value-of select="@id"/>
                      </xsl:attribute>
                      <xsl:value-of select="name" disable-output-escaping="yes"/>
                    </a>
                  </b>
                </p>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:for-each select="/FeatureCatalogue/Package[parent/@idref=$package/@id]">
              <xsl:sort select="./code"/>
              <xsl:sort select="./name"/>
              <p style="margin-left:20px">
                <xsl:text>Untergeordnet: </xsl:text>
                <a>
                  <xsl:attribute name="href">#<xsl:value-of select="@id"/>
                    <xsl:text>_toc</xsl:text>
                  </xsl:attribute>
                  <xsl:value-of select="name" disable-output-escaping="yes"/>
                </a>
              </p>
            </xsl:for-each>
          </td>
          <td class="package" border="0"/>
        </tr>
        <xsl:for-each select="/FeatureCatalogue/FeatureType[package/@idref=$package/@id]">
          <xsl:sort select="./code"/>
          <xsl:sort select="./name"/>
          <xsl:variable name="featuretype" select="."/>
          <xsl:if test="$featuretype/type='Feature Type' or $featuretype/type='Spatial Object Type'">
            <tr border="0">
              <td class="type" border="0">
                <p style="margin-left:20px">
                  <a>
                    <xsl:attribute name="href">#<xsl:value-of select="$featuretype/@id"/></xsl:attribute>
                    <xsl:value-of select="$featuretype/name" disable-output-escaping="yes"/>
                  </a>
                </p>
              </td>
              <td border="0">
                <p>
			      <xsl:call-template name="typename">
			        <xsl:with-param name="type" select="$featuretype/type"/>
			      </xsl:call-template>
                </p>
              </td>
            </tr>
		  </xsl:if>
        </xsl:for-each>
      </div>
    </xsl:if>
  </xsl:template>
  <xsl:template match="Package|ApplicationSchema" mode="detail">
    <xsl:variable name="package" select="."/>
    <xsl:if
      test="/FeatureCatalogue/FeatureType/package[attribute::idref=$package/@id]|/FeatureCatalogue/Package/parent[attribute::idref=$package/@id]">
      <hr/>
      <h2>
        <a>
          <xsl:attribute name="name">
            <xsl:value-of select="@id"/>
          </xsl:attribute>
          <xsl:choose>
            <xsl:when test="count($package/parent)=1">
              <xsl:text>Paket: </xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>Anwendungsschema: </xsl:text>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:value-of select="name" disable-output-escaping="yes"/>
        </a>
      </h2>
      <div>
        <xsl:if test="title">
          <p>
            <b>Begriff</b>
          </p>
          <p style="margin-left:20px">
            <xsl:value-of select="title" disable-output-escaping="yes"/>
          </p>
        </xsl:if>
        <xsl:if test="definition">
          <p>
            <b>Definition</b>
          </p>
          <xsl:for-each select="definition">
            <p style="margin-left:20px">
              <xsl:value-of select="." disable-output-escaping="yes"/>
            </p>
          </xsl:for-each>
        </xsl:if>
        <xsl:if test="description">
          <p>
            <b>Beschreibung</b>
          </p>
          <xsl:for-each select="description">
            <p style="margin-left:20px">
              <xsl:value-of select="." disable-output-escaping="yes"/>
            </p>
          </xsl:for-each>
        </xsl:if>
        <xsl:if test="versionNumber">
          <p>
            <b>Version:</b>
          </p>
          <p style="margin-left:20px">
            <xsl:value-of select="versionNumber" disable-output-escaping="yes"/>
          </p>
        </xsl:if>
        <xsl:if test="versionDate">
          <p>
            <b>Datum:</b>
          </p>
          <p style="margin-left:20px">
            <xsl:value-of select="versionDate" disable-output-escaping="yes"/>
          </p>
        </xsl:if>
        <xsl:if test="producer">
          <p>
            <b>Verantwortliche Organisation:</b>
          </p>
          <p style="margin-left:20px">
            <xsl:value-of select="producer" disable-output-escaping="yes"/>
          </p>
        </xsl:if>
        <xsl:if test="/FeatureCatalogue/Package[parent/@idref=$package/@id]">
          <p>
            <b>Untergeordnete Pakete:</b>
          </p>
          <xsl:for-each select="/FeatureCatalogue/Package[parent/@idref=$package/@id]">
            <xsl:sort select="./code"/>
            <xsl:sort select="./name"/>
            <p style="margin-left:20px">
              <a>
                <xsl:attribute name="href">#<xsl:value-of select="@id"/>
                </xsl:attribute>
                <xsl:value-of select="name" disable-output-escaping="yes"/>
              </a>
            </p>
          </xsl:for-each>
        </xsl:if>
        <xsl:if test="parent">
          <p>
            <b>Übergeordnetes Paket:</b>
          </p>
          <p style="margin-left:20px">
            <a>
              <xsl:attribute name="href">#<xsl:value-of select="parent/@idref"/>
              </xsl:attribute>
              <!-- <xsl:value-of select="//*[@id=$package/parent/@idref]/name"
                disable-output-escaping="yes"/> -->
              <xsl:value-of select="key('modelElement',$package/parent/@idref)/name"
                disable-output-escaping="yes"/>
            </a>
          </p>
        </xsl:if>
      </div>
      <div>
        <xsl:variable name="nft2" select="count(diagram)"/>
        <xsl:if test="$nft2 >= 1">
          <p>
            <b>
              <a>
                <xsl:attribute name="href">
                  <xsl:value-of select="diagram/@src"/>
                </xsl:attribute> Diagramm </a>
            </b>
          </p>
        </xsl:if>
      </div>
      <table border="0" class="link">
        <tr border="0">
          <td width="100%" border="0">
            <p align="right" class="small">
              <a href="#overview">zurück zur Übersicht</a>
            </p>
          </td>
        </tr>
      </table>
      <xsl:for-each select="/FeatureCatalogue/FeatureType[package/@idref=$package/@id]">
        <xsl:sort select="./code"/>
        <xsl:sort select="./name"/>
        <xsl:apply-templates select="." mode="detail"/>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
  <xsl:template match="FeatureType" mode="detail">
    <xsl:variable name="featuretype" select="."/>
    <!-- <xsl:variable name="package" select="//*[@id=$featuretype/package/@idref]"/> -->
    <xsl:variable name="package" select="key('modelElement',$featuretype/package/@idref)"/>
    <br/>
    <a>
      <xsl:attribute name="name">
        <xsl:value-of select="$featuretype/@id"/>
      </xsl:attribute>
      <h3>
        <xsl:if test="$featuretype/type">
	      <xsl:call-template name="typename">
	        <xsl:with-param name="type" select="$featuretype/type"/>
	      </xsl:call-template>
	      <xsl:text>: </xsl:text>
	    </xsl:if>
        <xsl:value-of select="$featuretype/name" disable-output-escaping="yes"/>
      </h3>
    </a>
    <table class="feature">
      <tr>
        <td colspan="4" class="feature">
          <p class="title">
            <xsl:value-of select="$featuretype/name" disable-output-escaping="yes"/>
          </p>
        </td>
      </tr>
      <xsl:call-template name="entry">
        <xsl:with-param name="title">Begriff</xsl:with-param>
        <xsl:with-param name="lines" select="$featuretype/title"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
        <xsl:with-param name="title">Definition</xsl:with-param>
        <xsl:with-param name="lines" select="$featuretype/definition"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
        <xsl:with-param name="title">Beschreibung</xsl:with-param>
        <xsl:with-param name="lines" select="$featuretype/description"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
        <xsl:with-param name="title">Abgeleitet aus</xsl:with-param>
        <xsl:with-param name="lines" select="$featuretype/subtypeOf"/>
      </xsl:call-template>
      <xsl:call-template name="subtypeentry">
        <xsl:with-param name="title">Oberklasse von</xsl:with-param>
        <!-- <xsl:with-param name="types" select="//FeatureType[subtypeOf/@idref=$featuretype/@id]"/> -->
        <xsl:with-param name="types" select="/FeatureCatalogue/FeatureType[subtypeOf/@idref=$featuretype/@id]"/>
      </xsl:call-template>
    <xsl:choose>
    	<xsl:when test="$featuretype/type='Feature Type' or $featuretype/type='Object Type'">
	      <xsl:call-template name="entry2">
	        <xsl:with-param name="title">Typ</xsl:with-param>
	        <xsl:with-param name="line">Objektart</xsl:with-param>
	      </xsl:call-template>
    	</xsl:when>
    	<xsl:when test="$featuretype/type='Data Type'">
	      <xsl:call-template name="entry2">
	        <xsl:with-param name="title">Typ</xsl:with-param>
	        <xsl:with-param name="line">Datentyp</xsl:with-param>
	      </xsl:call-template>
    	</xsl:when>
    	<xsl:when test="$featuretype/type='Union Data Type'">
	      <xsl:call-template name="entry2">
	        <xsl:with-param name="title">Typ</xsl:with-param>
	        <xsl:with-param name="line">Union-Datentyp</xsl:with-param>
	      </xsl:call-template>
    	</xsl:when>
    </xsl:choose>
      <xsl:call-template name="entry">
        <xsl:with-param name="title">Code</xsl:with-param>
        <xsl:with-param name="lines" select="$featuretype/code"/>
      </xsl:call-template>
      <xsl:choose>
        <xsl:when test="$noAlphabeticSortingForProperties = 'true'">
          <xsl:for-each select="$featuretype/characterizedBy/@idref">
            <!-- use order of characterizedBy elements -->
            <xsl:apply-templates mode="detail" select="key('modelElement', .)"/>
          </xsl:for-each>          
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="key('modelElement', $featuretype/characterizedBy/@idref)">
            <!-- apply an alphabetical sort of feature type characteristics (attributes, relationships etc) -->
            <xsl:sort select="./name"/>
            <xsl:apply-templates mode="detail" select="."/>
          </xsl:for-each>
        </xsl:otherwise>
      </xsl:choose>
     <!-- <xsl:apply-templates
        select="/FeatureCatalogue/FeatureAttribute[@id=$featuretype/characterizedBy/@idref]"
        mode="detail"/>
      <xsl:apply-templates
        select="/FeatureCatalogue/RelationshipRole[@id=$featuretype/characterizedBy/@idref]"
        mode="detail"/>
      <xsl:apply-templates
        select="/FeatureCatalogue/FeatureOperation[@id=$featuretype/characterizedBy/@idref]"
        mode="detail"/>-->
      <xsl:call-template name="entry">
        <xsl:with-param name="title">Bedingungen</xsl:with-param>
        <xsl:with-param name="lines" select="$featuretype/constraint"/>
      </xsl:call-template>
    </table>
    <table border="0" class="link">
      <tr border="0">
        <td width="100%" border="0">
          <p align="right" class="small">
            <a>
              <xsl:attribute name="href">#<xsl:value-of select="$package/@id"/>
              </xsl:attribute> zurück zu Paket: <xsl:value-of select="$package/name"
                disable-output-escaping="yes"/>
            </a>
          </p>
        </td>
      </tr>
    </table>
  </xsl:template>
  <xsl:template match="FeatureAttribute" mode="detail">
    <xsl:variable name="featureAtt" select="."/>
    <tr>
      <td colspan="4" class="feature">
        <p class="title2">Attribut:</p>
        <table class="att">
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Name</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/name"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Begriff</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/title"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Definition</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/definition"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Beschreibung</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/description"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Voidable</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/voidable"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Code</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/code"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Multipliztät</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/cardinality"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Typ</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/ValueDataType"/>
          </xsl:call-template>
          <xsl:if test="$featureAtt/ValueDomainType = 1">
            <xsl:call-template name="clentry">
              <xsl:with-param name="title">Werte</xsl:with-param>
              <!-- <xsl:with-param name="values"
                select="/FeatureCatalogue/Value[@id=$featureAtt/enumeratedBy/@idref]"/> -->
                <xsl:with-param name="values"
                select="key('modelElement',$featureAtt/enumeratedBy/@idref)"/>
            </xsl:call-template>
          </xsl:if>
        </table>
      </td>
    </tr>
  </xsl:template>
  <xsl:template match="RelationshipRole" mode="detail">
    <xsl:variable name="featureAtt" select="."/>
    <tr>
      <td colspan="4" class="feature">
        <p class="title2">Assoziationsrolle:</p>
        <table class="att">
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Name</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/name"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Begriff</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/title"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Definition</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/definition"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Beschreibung</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/description"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Voidable</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/voidable"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Code</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/code"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Multiplizität</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/cardinality"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Typ</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/FeatureTypeIncluded"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Assoziationsklasse</xsl:with-param>
            <xsl:with-param name="lines"
              select="key('modelElement',@id=$featureAtt/relation/@idref)/associationClass"/>
            <!--<xsl:with-param name="lines"
              select="//FeatureRelationship[@id=$featureAtt/relation/@idref]/associationClass"/>-->
          </xsl:call-template>
        </table>
      </td>
    </tr>
  </xsl:template>
  <xsl:template match="FeatureOperation" mode="detail">
    <xsl:variable name="featureAtt" select="."/>
    <tr>
      <td colspan="4" class="feature">
        <p class="title2">Operation:</p>
        <table class="att">
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Name</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/name"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Begriff</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/title"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Definition</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/definition"/>
          </xsl:call-template>
          <xsl:call-template name="attentry">
            <xsl:with-param name="title">Beschreibung</xsl:with-param>
            <xsl:with-param name="lines" select="$featureAtt/description"/>
          </xsl:call-template>
          <!--
<xsl:call-template name="attentry">
          <xsl:with-param name="title">???</xsl:with-param>
          <xsl:with-param name="lines" select="$featureAtt/objectFeatureTypeNames"/>
        </xsl:call-template>
-->
        </table>
      </td>
    </tr>
  </xsl:template>
  <xsl:template name="entry">
    <xsl:param name="title"/>
    <xsl:param name="lines"/>
    <xsl:if test="$lines">
      <tr>
        <td colspan="4" class="feature">
          <p class="title2">
            <xsl:value-of select="$title" disable-output-escaping="yes"/>:</p>
          <xsl:for-each select="$lines">
            <xsl:variable name="line" select="."/>
            <xsl:choose>
              <xsl:when test="$line/@idref and key('modelElement',$line/@idref)">
                <p style="margin-left:20px">
                  <a>
                    <xsl:attribute name="href">#<xsl:value-of select="./@idref"/>
                    </xsl:attribute>
                    <xsl:value-of select="." disable-output-escaping="yes"/>
                  </a>
                </p>
              </xsl:when>
             <!-- <xsl:when test="$line/@idref and //*[@id=$line/@idref]">
                <p style="margin-left:20px">
                  <a>
                    <xsl:attribute name="href">#<xsl:value-of select="./@idref"/>
                    </xsl:attribute>
                    <xsl:value-of select="." disable-output-escaping="yes"/>
                  </a>
                </p>
              </xsl:when>-->
              <xsl:otherwise>
                <p style="margin-left:20px">
                  <xsl:value-of select="." disable-output-escaping="yes"/>
                </p>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="entry2">
    <xsl:param name="title"/>
    <xsl:param name="line"/>
      <tr>
        <td colspan="4" class="feature">
          <p class="title2">
            <xsl:value-of select="$title" disable-output-escaping="yes"/>:</p>
                <p style="margin-left:20px">
                  <xsl:value-of select="$line" disable-output-escaping="yes"/>
                </p>
        </td>
      </tr>
  </xsl:template>
  <xsl:template name="subtypeentry">
    <xsl:param name="title"/>
    <xsl:param name="types"/>
    <xsl:if test="$types">
      <tr>
        <td colspan="4" class="feature">
          <p class="title2">
            <xsl:value-of select="$title" disable-output-escaping="yes"/>:</p>
          <xsl:for-each select="$types">
            <xsl:sort select="./code"/>
            <xsl:sort select="./name"/>
            <p style="margin-left:20px">
              <a>
                <xsl:attribute name="href">#<xsl:value-of select="@id"/>
                </xsl:attribute>
                <xsl:value-of select="name" disable-output-escaping="yes"/>
              </a>
            </p>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="attentry">
    <xsl:param name="title"/>
    <xsl:param name="lines"/>
    <xsl:if test="$lines">
      <tr border="0">
        <td width="100px" border="0">
          <p class="title2" style="margin-left:18px">
            <xsl:value-of select="$title" disable-output-escaping="yes"/>:</p>
        </td>
        <td border="0">
          <xsl:for-each select="$lines">
            <xsl:variable name="line" select="."/>
            <xsl:choose>
              <xsl:when test="$line/@idref and key('modelElement',$line/@idref)">
                <p>
                  <a>
                    <xsl:attribute name="href">#<xsl:value-of select="./@idref"/>
                    </xsl:attribute>
                    <xsl:value-of select="." disable-output-escaping="yes"/>
                  </a>
                  <xsl:if test="$line/@category">
                    <xsl:text> </xsl:text>(<xsl:value-of select="./@category"/>)</xsl:if>
                </p>
              </xsl:when>
              <!--<xsl:when test="$line/@idref and //*[@id=$line/@idref]">
                <p>
                  <a>
                    <xsl:attribute name="href">#<xsl:value-of select="./@idref"/>
                    </xsl:attribute>
                    <xsl:value-of select="." disable-output-escaping="yes"/>
                  </a>
                  <xsl:if test="$line/@category">
                    <xsl:text> </xsl:text>(<xsl:value-of select="./@category"/>)</xsl:if>
                </p>
              </xsl:when>-->
              <xsl:otherwise>
                <p>
                  <xsl:value-of select="." disable-output-escaping="yes"/>
                  <xsl:if test="$line/@category">
                    <xsl:text> </xsl:text>(<xsl:value-of select="./@category"/>)</xsl:if>
                </p>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="clentry">
    <xsl:param name="title"/>
    <xsl:param name="values"/>
    <xsl:if test="$values">
      <tr border="0">
        <td width="100px" border="0">
          <p class="title2" style="margin-left:18px">Werte:</p>
        </td>
        <td border="0">
          <table class="values">
            <xsl:for-each select="$values">
              <tr>
                <xsl:variable name="value" select="."/>
                <td class="values">
                  <p>
                    <xsl:value-of select="./code" disable-output-escaping="yes"/>
                  </p>
                </td>
                <td class="values">
                  <xsl:if test="not(./code = ./label)">
                    <p class="small">
                      <b>
                        <xsl:value-of select="./label" disable-output-escaping="yes"/>
                      </b>
                    </p>
                  </xsl:if>
                  <xsl:if test="./definition">
                    <xsl:for-each select="./definition">
                      <p class="small">
                        <xsl:value-of select="." disable-output-escaping="yes"/>
                      </p>
                    </xsl:for-each>
                  </xsl:if>
                  <xsl:if test="./description">
                    <xsl:for-each select="./description">
                      <p class="small">
                        <xsl:value-of select="." disable-output-escaping="yes"/>
                      </p>
                    </xsl:for-each>
                  </xsl:if>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
  <xsl:template name="typename">
    <xsl:param name="type"/>
    <xsl:choose>
    	<xsl:when test="$type='Feature Type'"><xsl:text>Objektart</xsl:text></xsl:when>
    	<xsl:when test="$type='Object Type'"><xsl:text>Objektart</xsl:text></xsl:when>
    	<xsl:when test="$type='Data Type'"><xsl:text>Datentyp</xsl:text></xsl:when>
    	<xsl:when test="$type='Union Data Type'"><xsl:text>Union-Datentyp</xsl:text></xsl:when>
    	<xsl:otherwise><xsl:text></xsl:text></xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>
