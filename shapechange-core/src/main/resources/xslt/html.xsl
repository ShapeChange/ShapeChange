<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 <!-- (c) 2001-2016 interactive instruments GmbH, Bonn -->

 <!-- ==================== -->
 <!-- Imports and Includes -->
 <!-- ==================== -->

 <!-- include the stylesheet with localization variables -->
 <xsl:include href="localization.xsl"/>

 <!-- =============== -->
 <!-- Output settings -->
 <!-- =============== -->
 <!-- NOTE: Use XML attribute 'encoding' to change the output encoding. Example: encoding="iso-8859-1" -->
 <xsl:output indent="yes" method="html"/>

 <!-- ================= -->
 <!-- Catalogue content -->
 <!-- ================= -->
 <xsl:key match="/*/*[@id]" name="modelElement" use="@id"/>

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
    <title>
     <xsl:value-of select="$fc.FeatureCatalogue"/>
     <xsl:value-of disable-output-escaping="yes" select="FeatureCatalogue/name"/>
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
       <h1 style="text-align:center;">
        <xsl:value-of select="$fc.FeatureCatalogue"/>
        <xsl:value-of disable-output-escaping="yes" select="FeatureCatalogue/name"/>
       </h1>
      </div>
     </xsl:when>
     <xsl:otherwise>
      <h1>
       <xsl:value-of select="$fc.FeatureCatalogue"/>
       <xsl:value-of disable-output-escaping="yes" select="FeatureCatalogue/name"/>
      </h1>
     </xsl:otherwise>
    </xsl:choose>    
    <p>
     <b><xsl:value-of select="$fc.Version"/>:</b>
    </p>
    <p style="margin-left:20px">
     <xsl:value-of disable-output-escaping="yes" select="FeatureCatalogue/versionNumber"/>
    </p>
    <p>
     <b><xsl:value-of select="$fc.Date"/>:</b>
    </p>
    <p style="margin-left:20px">
     <xsl:value-of disable-output-escaping="yes" select="FeatureCatalogue/versionDate"/>
    </p>
    <p>
     <b><xsl:value-of select="$fc.Scope"/>:</b>
    </p>
    <xsl:for-each select="FeatureCatalogue/scope">
     <p style="margin-left:20px">
      <xsl:value-of disable-output-escaping="yes" select="."/>
     </p>
    </xsl:for-each>
    <p>
     <b><xsl:value-of select="$fc.ResponsibleOrganization"/>:</b>
    </p>
    <p style="margin-left:20px">
     <xsl:value-of disable-output-escaping="yes" select="FeatureCatalogue/producer"/>
    </p>
    <a>
     <xsl:attribute name="name">overview</xsl:attribute>
     <h2>
      <xsl:value-of select="$fc.Toc"/>
     </h2>
    </a>
    <table border="0" class="overview">
     <xsl:for-each select="FeatureCatalogue/Package | FeatureCatalogue/ApplicationSchema">
      <xsl:sort select="./code"/>
      <xsl:sort select="./name"/>
      <xsl:apply-templates mode="overview" select="."/>
     </xsl:for-each>
    </table>
    <xsl:for-each select="FeatureCatalogue/Package | FeatureCatalogue/ApplicationSchema">
     <xsl:sort select="./code"/>
     <xsl:sort select="./name"/>
     <xsl:apply-templates mode="detail" select="."/>
    </xsl:for-each>
    <hr/>
    <p align="center">
     <small>
      <xsl:value-of select="$fc.GeneratedBy"/>
      <xsl:text> </xsl:text>
      <a href="http://shapechange.net">ShapeChange</a>
     </small>
    </p>
   </body>
  </html>
 </xsl:template>
 <xsl:template match="Package | ApplicationSchema" mode="overview">
  <xsl:variable name="package" select="."/>
  <xsl:if
   test="/FeatureCatalogue/FeatureType[not(type = 'Code List Type' or type = 'Enumeration Type')]/package[attribute::idref = $package/@id] | /FeatureCatalogue/Package/parent[attribute::idref = $package/@id]">
   <div>
    <tr border="0">
     <td border="0" class="package">
      <xsl:choose>
       <xsl:when test="parent">
        <p>
         <b>
          <a>
           <xsl:attribute name="name">
            <xsl:value-of select="@id"/>
            <xsl:text>_toc</xsl:text>
           </xsl:attribute>
           <xsl:value-of select="$fc.Package"/>
           <xsl:text>: </xsl:text>
          </a>
          <a>
           <xsl:attribute name="href">#<xsl:value-of select="@id"/>
           </xsl:attribute>
           <xsl:value-of disable-output-escaping="yes" select="name"/>
          </a>
         </b>
        </p>
        <p style="margin-left:20px">
         <xsl:value-of select="$fc.Parent"/>
         <xsl:text>: </xsl:text>
         <a>
          <xsl:attribute name="href">#<xsl:value-of select="parent/@idref"/>
           <xsl:text>_toc</xsl:text>
          </xsl:attribute>
          <xsl:value-of disable-output-escaping="yes"
           select="key('modelElement', $package/parent/@idref)/name"/>
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
           <xsl:value-of select="$fc.ApplicationSchema"/>
           <xsl:text>: </xsl:text>
          </a>
          <a>
           <xsl:attribute name="href">#<xsl:value-of select="@id"/>
           </xsl:attribute>
           <xsl:value-of disable-output-escaping="yes" select="name"/>
          </a>
         </b>
        </p>
       </xsl:otherwise>
      </xsl:choose>
      <xsl:for-each select="/FeatureCatalogue/Package[parent/@idref = $package/@id]">
       <xsl:sort select="./code"/>
       <xsl:sort select="./name"/>
       <p style="margin-left:20px">
        <xsl:value-of select="$fc.SubPackage"/>
        <xsl:text>: </xsl:text>
        <a>
         <xsl:attribute name="href">#<xsl:value-of select="@id"/>
          <xsl:text>_toc</xsl:text>
         </xsl:attribute>
         <xsl:value-of disable-output-escaping="yes" select="name"/>
        </a>
       </p>
      </xsl:for-each>
     </td>
     <td border="0" class="package"/>
    </tr>
    <xsl:for-each select="/FeatureCatalogue/FeatureType[not(type = 'Code List Type' or type = 'Enumeration Type')][package/@idref = $package/@id]">
     <xsl:sort select="./code"/>
     <xsl:sort select="./name"/>
     <xsl:variable name="featuretype" select="."/>
     <xsl:if test="$featuretype/type = 'Feature Type' or $featuretype/type = 'Spatial Object Type'">
      <tr border="0">
       <td border="0" class="type">
        <p style="margin-left:20px">
         <a>
          <xsl:attribute name="href">#<xsl:value-of select="$featuretype/@id"/></xsl:attribute>
          <xsl:value-of disable-output-escaping="yes" select="$featuretype/name"/>
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
 <xsl:template match="Package | ApplicationSchema" mode="detail">
  <xsl:variable name="package" select="."/>
  <!-- TBD: for empty packages (no classes, no child packages) this results in the package being referenced from its parent but not showing up in the feature catalogue -->
  <xsl:if
   test="/FeatureCatalogue/FeatureType[not(type = 'Code List Type' or type = 'Enumeration Type')]/package[attribute::idref = $package/@id] | /FeatureCatalogue/Package/parent[attribute::idref = $package/@id]">
   <hr/>
   <h2>
    <a>
     <xsl:attribute name="name">
      <xsl:value-of select="@id"/>
     </xsl:attribute>
     <xsl:choose>
      <xsl:when test="count($package/parent) = 1">
       <xsl:value-of select="$fc.Package"/>
       <xsl:text>: </xsl:text>
      </xsl:when>
      <xsl:otherwise>
       <xsl:value-of select="$fc.ApplicationSchema"/>
       <xsl:text>: </xsl:text>
      </xsl:otherwise>
     </xsl:choose>
     <xsl:value-of disable-output-escaping="yes" select="name"/>
    </a>
   </h2>
   <div>
    <xsl:if test="title">
     <p>
      <b>
       <xsl:value-of select="$fc.Title"/>
      </b>
     </p>
     <p style="margin-left:20px">
      <xsl:value-of disable-output-escaping="yes" select="title"/>
     </p>
    </xsl:if>
    <xsl:if test="definition">
     <p>
      <b>
       <xsl:value-of select="$fc.Definition"/>
      </b>
     </p>
     <xsl:for-each select="definition">
      <p style="margin-left:20px">
       <xsl:value-of disable-output-escaping="yes" select="."/>
      </p>
     </xsl:for-each>
    </xsl:if>
    <xsl:if test="description">
     <p>
      <b>
       <xsl:value-of select="$fc.Description"/>
      </b>
     </p>
     <xsl:for-each select="description">
      <p style="margin-left:20px">
       <xsl:value-of disable-output-escaping="yes" select="."/>
      </p>
     </xsl:for-each>
    </xsl:if>
    <xsl:if test="example">
     <p>
      <b>
       <xsl:value-of select="$fc.Example"/>
      </b>
     </p>
     <xsl:for-each select="example">
      <p style="margin-left:20px">
       <xsl:value-of disable-output-escaping="yes" select="."/>
      </p>
     </xsl:for-each>
    </xsl:if>
    <xsl:if test="legalBasis">
     <p>
      <b>
       <xsl:value-of select="$fc.LegalBasis"/>
      </b>
     </p>
     <xsl:for-each select="legalBasis">
      <p style="margin-left:20px">
       <xsl:value-of disable-output-escaping="yes" select="."/>
      </p>
     </xsl:for-each>
    </xsl:if>
    <xsl:if test="dataCaptureStatement">
     <p>
      <b>
       <xsl:value-of select="$fc.DataCaptureStatement"/>
      </b>
     </p>
     <xsl:for-each select="dataCaptureStatement">
      <p style="margin-left:20px">
       <xsl:value-of disable-output-escaping="yes" select="."/>
      </p>
     </xsl:for-each>
    </xsl:if>
    <xsl:if test="versionNumber">
     <p>
      <b><xsl:value-of select="$fc.Version"/>:</b>
     </p>
     <p style="margin-left:20px">
      <xsl:value-of disable-output-escaping="yes" select="versionNumber"/>
     </p>
    </xsl:if>
    <xsl:if test="versionDate">
     <p>
      <b><xsl:value-of select="$fc.Date"/>:</b>
     </p>
     <p style="margin-left:20px">
      <xsl:value-of disable-output-escaping="yes" select="versionDate"/>
     </p>
    </xsl:if>
    <xsl:if test="producer">
     <p>
      <b><xsl:value-of select="$fc.ResponsibleOrganization"/>:</b>
     </p>
     <p style="margin-left:20px">
      <xsl:value-of disable-output-escaping="yes" select="producer"/>
     </p>
    </xsl:if>
    <xsl:if test="/FeatureCatalogue/Package[parent/@idref = $package/@id]">
     <p>
      <b><xsl:value-of select="$fc.SubPackage"/>:</b>
     </p>
     <xsl:for-each select="/FeatureCatalogue/Package[parent/@idref = $package/@id]">
      <xsl:sort select="./code"/>
      <xsl:sort select="./name"/>
      <p style="margin-left:20px">
       <a>
        <xsl:attribute name="href">#<xsl:value-of select="@id"/>
        </xsl:attribute>
        <xsl:value-of disable-output-escaping="yes" select="name"/>
       </a>
      </p>
     </xsl:for-each>
    </xsl:if>
    <xsl:if test="parent">
     <p>
      <b><xsl:value-of select="$fc.ParentPackage"/>:</b>
     </p>
     <p style="margin-left:20px">
      <a>
       <xsl:attribute name="href">#<xsl:value-of select="parent/@idref"/>
       </xsl:attribute>
       <xsl:value-of disable-output-escaping="yes"
        select="key('modelElement', $package/parent/@idref)/name"/>
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
        </xsl:attribute>
        <xsl:value-of select="$fc.Diagram"/>
       </a>
      </b>
     </p>
    </xsl:if>
   </div>
   <table border="0" class="link">
    <tr border="0">
     <td border="0" width="100%">
      <p align="right" class="small">
       <a href="#overview">
        <xsl:value-of select="$fc.backToToc"/>
       </a>
      </p>
     </td>
    </tr>
   </table>
   <xsl:for-each select="/FeatureCatalogue/FeatureType[not(type = 'Code List Type' or type = 'Enumeration Type')][package/@idref = $package/@id]">
    <xsl:sort select="./code"/>
    <xsl:sort select="./name"/>
    <xsl:apply-templates mode="detail" select="."/>
   </xsl:for-each>
  </xsl:if>
 </xsl:template>

 <xsl:template match="FeatureType" mode="detail">
  <xsl:variable name="featuretype" select="."/>
  <xsl:variable name="package" select="key('modelElement', $featuretype/package/@idref)"/>
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
    <xsl:value-of disable-output-escaping="yes" select="$featuretype/name"/>
   </h3>
  </a>
  <table class="feature">
   <tr border="0">
    <!--<td colspan="4" class="feature" border="0">-->
    <td border="0" class="feature">
     <p class="title">
      <xsl:value-of disable-output-escaping="yes" select="$featuretype/name"/>
     </p>
    </td>
   </tr>
   <tr>
    <td class="feature">
     <table class="att">
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.Title"/>
       <xsl:with-param name="lines" select="$featuretype/title"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.Definition"/>
       <xsl:with-param name="lines" select="$featuretype/definition"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.Description"/>
       <xsl:with-param name="lines" select="$featuretype/description"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.Example"/>
       <xsl:with-param name="lines" select="$featuretype/example"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.LegalBasis"/>
       <xsl:with-param name="lines" select="$featuretype/legalBasis"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.DataCaptureStatement"/>
       <xsl:with-param name="lines" select="$featuretype/dataCaptureStatement"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.SubtypeOf"/>
       <xsl:with-param name="lines" select="$featuretype/subtypeOf"/>
      </xsl:call-template>
      <xsl:call-template name="subtypeentry">
       <xsl:with-param name="title" select="$fc.SupertypeOf"/>
       <xsl:with-param name="types"
        select="/FeatureCatalogue/FeatureType[not(type = 'Code List Type' or type = 'Enumeration Type')][subtypeOf/@idref = $featuretype/@id]"/>
      </xsl:call-template>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.Type"/>
       <xsl:with-param name="lines" select="$featuretype/type"/>
      </xsl:call-template>
      <xsl:if test="count($featuretype/isAbstract) > 0">
       <xsl:call-template name="entrytext">
        <xsl:with-param name="title" select="$fc.Abstract"/>
        <xsl:with-param name="value" select="$fc.true"/>
       </xsl:call-template>
      </xsl:if>
      <xsl:call-template name="entry">
       <xsl:with-param name="title" select="$fc.Code"/>
       <xsl:with-param name="lines" select="$featuretype/code"/>
      </xsl:call-template>
      <xsl:call-template name="entryTaggedValues">
       <xsl:with-param name="tvs" select="$featuretype/taggedValues/taggedValue"/>
      </xsl:call-template>
     </table>
    </td>
   </tr>
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
   <xsl:for-each select="$featuretype/constraint">
    <!-- apply an alphabetical sort of feature type constraints; constraints without name are listed first -->
    <xsl:sort select="./name"/>
    <xsl:apply-templates mode="detail" select="."/>
   </xsl:for-each>
  </table>
  <table border="0" class="link">
   <tr border="0">
    <td border="0" width="100%">
     <p align="right" class="small">
      <a>
       <xsl:attribute name="href">#<xsl:value-of select="$package/@id"/>
       </xsl:attribute><xsl:value-of select="$fc.backToPackage"/>: <xsl:value-of
        disable-output-escaping="yes" select="$package/name"/>
      </a>
     </p>
    </td>
   </tr>
  </table>
 </xsl:template>
 <xsl:template match="FeatureAttribute" mode="detail">
  <xsl:variable name="featureAtt" select="."/>
  <tr>
   <!-- <td colspan="4" class="feature"> -->
   <td class="feature">
    <p class="title2"><xsl:value-of select="$fc.Attribute"/>:</p>
    <table class="att">
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Name"/>
      <xsl:with-param name="lines" select="$featureAtt/name"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Title"/>
      <xsl:with-param name="lines" select="$featureAtt/title"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Definition"/>
      <xsl:with-param name="lines" select="$featureAtt/definition"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Description"/>
      <xsl:with-param name="lines" select="$featureAtt/description"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Example"/>
      <xsl:with-param name="lines" select="$featureAtt/example"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.LegalBasis"/>
      <xsl:with-param name="lines" select="$featureAtt/legalBasis"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.DataCaptureStatement"/>
      <xsl:with-param name="lines" select="$featureAtt/dataCaptureStatement"/>
     </xsl:call-template>
     <xsl:if test="count($featureAtt/voidable) > 0">
      <xsl:variable name="val">
       <xsl:choose>
        <xsl:when test="$featureAtt/voidable[. = 'true' or . = '1']">
         <xsl:value-of select="$fc.true"/>
        </xsl:when>
        <xsl:otherwise>
         <xsl:value-of select="$fc.false"/>
        </xsl:otherwise>
       </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="attentrytext">
       <xsl:with-param name="title" select="$fc.Voidable"/>
       <xsl:with-param name="value" select="$val"/>
      </xsl:call-template>
     </xsl:if>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Code"/>
      <xsl:with-param name="lines" select="$featureAtt/code"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Multiplicity"/>
      <xsl:with-param name="lines" select="$featureAtt/cardinality"/>
     </xsl:call-template>
     <xsl:if test="count($featureAtt/isDerived) > 0">
      <xsl:call-template name="attentrytext">
       <xsl:with-param name="title" select="$fc.Derived"/>
       <xsl:with-param name="value" select="$fc.true"/>
      </xsl:call-template>
     </xsl:if>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.InitialValue"/>
      <xsl:with-param name="lines" select="$featureAtt/initialValue"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.ValueType"/>
      <xsl:with-param name="lines" select="$featureAtt/ValueDataType"/>
     </xsl:call-template>
     <xsl:if test="$featureAtt/ValueDomainType = 1">
      <xsl:call-template name="clentry">
       <xsl:with-param name="title" select="$fc.Values"/>
       <xsl:with-param name="values" select="key('modelElement', $featureAtt/enumeratedBy/@idref)"/>
      </xsl:call-template>
     </xsl:if>
     <xsl:call-template name="attTaggedValues">
      <xsl:with-param name="tvs" select="$featureAtt/taggedValues/taggedValue"/>
     </xsl:call-template>
    </table>
   </td>
  </tr>
 </xsl:template>
 <xsl:template match="constraint" mode="detail">
  <tr>
   <td class="feature">
    <p class="title2"><xsl:value-of select="$fc.Constraint"/>:</p>
    <table class="att">
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Name"/>
      <xsl:with-param name="lines" select="name"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Description"/>
      <xsl:with-param name="lines" select="description"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Expression"/>
      <xsl:with-param name="lines" select="expression"/>
     </xsl:call-template>
    </table>
   </td>
  </tr>
 </xsl:template>
 <xsl:template match="RelationshipRole" mode="detail">
  <xsl:variable name="featureAtt" select="."/>
  <tr>
   <!-- <td colspan="4" class="feature"> -->
   <td class="feature">
    <p class="title2"><xsl:value-of select="$fc.AssociationRole"/>:</p>
    <table class="att">
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Name"/>
      <xsl:with-param name="lines" select="$featureAtt/name"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Title"/>
      <xsl:with-param name="lines" select="$featureAtt/title"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Definition"/>
      <xsl:with-param name="lines" select="$featureAtt/definition"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Description"/>
      <xsl:with-param name="lines" select="$featureAtt/description"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Example"/>
      <xsl:with-param name="lines" select="$featureAtt/example"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.LegalBasis"/>
      <xsl:with-param name="lines" select="$featureAtt/legalBasis"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.DataCaptureStatement"/>
      <xsl:with-param name="lines" select="$featureAtt/dataCaptureStatement"/>
     </xsl:call-template>
     <xsl:if test="count($featureAtt/voidable) > 0">
      <xsl:variable name="val">
       <xsl:choose>
        <xsl:when test="$featureAtt/voidable[. = 'true' or . = '1']">
         <xsl:value-of select="$fc.true"/>
        </xsl:when>
        <xsl:otherwise>
         <xsl:value-of select="$fc.false"/>
        </xsl:otherwise>
       </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="attentrytext">
       <xsl:with-param name="title" select="$fc.Voidable"/>
       <xsl:with-param name="value" select="$val"/>
      </xsl:call-template>
     </xsl:if>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Code"/>
      <xsl:with-param name="lines" select="$featureAtt/code"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Multiplicity"/>
      <xsl:with-param name="lines" select="$featureAtt/cardinality"/>
     </xsl:call-template>
     <xsl:if test="count($featureAtt/isDerived) > 0">
      <xsl:call-template name="attentrytext">
       <xsl:with-param name="title" select="$fc.Derived"/>
       <xsl:with-param name="value" select="$fc.true"/>
      </xsl:call-template>
     </xsl:if>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.InitialValue"/>
      <xsl:with-param name="lines" select="$featureAtt/initialValue"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.ValueType"/>
      <xsl:with-param name="lines" select="$featureAtt/FeatureTypeIncluded"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.AssociationClass"/>
      <xsl:with-param name="lines"
       select="key('modelElement', @id = $featureAtt/relation/@idref)/associationClass"/>
     </xsl:call-template>
     <xsl:call-template name="attTaggedValues">
      <xsl:with-param name="tvs" select="$featureAtt/taggedValues/taggedValue"/>
     </xsl:call-template>
    </table>
   </td>
  </tr>
 </xsl:template>
 <xsl:template match="FeatureOperation" mode="detail">
  <xsl:variable name="featureAtt" select="."/>
  <tr>
   <!-- <td colspan="4" class="feature"> -->
   <td class="feature">
    <p class="title2"><xsl:value-of select="$fc.Operation"/>:</p>
    <table class="att">
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Name"/>
      <xsl:with-param name="lines" select="$featureAtt/name"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Title"/>
      <xsl:with-param name="lines" select="$featureAtt/title"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Definition"/>
      <xsl:with-param name="lines" select="$featureAtt/definition"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Description"/>
      <xsl:with-param name="lines" select="$featureAtt/description"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.Example"/>
      <xsl:with-param name="lines" select="$featureAtt/example"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.LegalBasis"/>
      <xsl:with-param name="lines" select="$featureAtt/legalBasis"/>
     </xsl:call-template>
     <xsl:call-template name="attentry">
      <xsl:with-param name="title" select="$fc.DataCaptureStatement"/>
      <xsl:with-param name="lines" select="$featureAtt/dataCaptureStatement"/>
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
   <tr border="0">
    <!--<td colspan="4" class="feature" border="0">-->
    <td border="0">
     <p class="title2">
      <xsl:value-of disable-output-escaping="yes" select="$title"/>:</p>
     <xsl:for-each select="$lines">
      <xsl:variable name="line" select="."/>
      <p style="margin-left:20px">
       <xsl:choose>
        <xsl:when test="$line/@idref and key('modelElement', $line/@idref)">
         <a>
          <xsl:attribute name="href">#<xsl:value-of select="./@idref"/>
          </xsl:attribute>
          <!-- If this entry is about the type of a feature type, localize the $line. -->
          <!-- This is a workaround for avoiding the #RTREEFRAG issue when using call-template inside a with-param. -->
          <xsl:choose>
           <xsl:when test="$title = $fc.Type">
            <xsl:call-template name="typename">
             <xsl:with-param name="type" select="$line"/>
            </xsl:call-template>
           </xsl:when>
           <xsl:otherwise>
            <xsl:value-of disable-output-escaping="yes" select="."/>
           </xsl:otherwise>
          </xsl:choose>
         </a>
        </xsl:when>
        <xsl:otherwise>
         <!-- If this entry is about the type of a feature type, localize the $line. -->
         <!-- This is a workaround for avoiding the #RTREEFRAG issue when using call-template inside a with-param. -->
         <xsl:choose>
          <xsl:when test="$title = $fc.Type">
           <xsl:call-template name="typename">
            <xsl:with-param name="type" select="$line"/>
           </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
           <xsl:value-of disable-output-escaping="yes" select="."/>
          </xsl:otherwise>
         </xsl:choose>
        </xsl:otherwise>
       </xsl:choose>
      </p>
     </xsl:for-each>
    </td>
   </tr>
  </xsl:if>
 </xsl:template>
 <xsl:template name="subtypeentry">
  <xsl:param name="title"/>
  <xsl:param name="types"/>
  <xsl:if test="$types">
   <tr border="0">
    <!--<td colspan="4" class="feature">-->
    <td border="0" class="feature">
     <p class="title2">
      <xsl:value-of disable-output-escaping="yes" select="$title"/>:</p>
     <xsl:for-each select="$types">
      <xsl:sort select="./code"/>
      <xsl:sort select="./name"/>
      <p style="margin-left:20px">
       <a>
        <xsl:attribute name="href">#<xsl:value-of select="@id"/>
        </xsl:attribute>
        <xsl:value-of disable-output-escaping="yes" select="name"/>
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
    <td border="0" width="100px">
     <p class="title2" style="margin-left:18px">
      <xsl:value-of disable-output-escaping="yes" select="$title"/>:</p>
    </td>
    <td border="0">
     <xsl:for-each select="$lines">
      <xsl:variable name="line" select="."/>
      <p>
       <xsl:choose>
        <xsl:when test="$line/@idref and key('modelElement', $line/@idref)">
         <a>
          <xsl:attribute name="href">#<xsl:value-of select="./@idref"/>
          </xsl:attribute>
          <!-- If this entry is about the type of a feature type, localize the $line. -->
          <!-- This is a workaround for avoiding the #RTREEFRAG issue when using call-template inside a with-param. -->
          <xsl:choose>
           <xsl:when test="$title = $fc.Type">
            <xsl:call-template name="typename">
             <xsl:with-param name="type" select="$line"/>
            </xsl:call-template>
           </xsl:when>
           <xsl:otherwise>
            <xsl:value-of disable-output-escaping="yes" select="."/>
           </xsl:otherwise>
          </xsl:choose>
         </a>
        </xsl:when>
        <xsl:when test="$line/@codeList">
         <a href="{$line/@codeList}" target="_blank">

          <!-- If this entry is about the type of a feature type, localize the $line. -->
          <!-- This is a workaround for avoiding the #RTREEFRAG issue when using call-template inside a with-param. -->
          <xsl:choose>
           <xsl:when test="$title = $fc.Type">
            <xsl:call-template name="typename">
             <xsl:with-param name="type" select="$line"/>
            </xsl:call-template>
           </xsl:when>
           <xsl:otherwise>
            <xsl:value-of disable-output-escaping="yes" select="."/>
           </xsl:otherwise>
          </xsl:choose>
         </a>
        </xsl:when>
        <xsl:otherwise>
         <!-- If this entry is about the type of a feature type, localize the $line. -->
         <!-- This is a workaround for avoiding the #RTREEFRAG issue when using call-template inside a with-param. -->
         <xsl:choose>
          <xsl:when test="$title = $fc.Type">
           <xsl:call-template name="typename">
            <xsl:with-param name="type" select="$line"/>
           </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
           <xsl:value-of disable-output-escaping="yes" select="."/>
          </xsl:otherwise>
         </xsl:choose>
        </xsl:otherwise>
       </xsl:choose>
       <xsl:if test="$line/@category">
        <xsl:text> (</xsl:text>
        <xsl:call-template name="typename">
         <!-- Here the @category value is in lower case; in order for the @category to also be translated, the same case needs to be used in @category and FeatureType/type-->
         <xsl:with-param name="type" select="./@category"/>
        </xsl:call-template>
        <xsl:text>)</xsl:text>
       </xsl:if>
      </p>
     </xsl:for-each>
    </td>
   </tr>
  </xsl:if>
 </xsl:template>
 <xsl:template name="attentrytext">
  <xsl:param name="title"/>
  <xsl:param name="value"/>
  <xsl:if test="$value">
   <tr border="0">
    <td border="0" width="100px">
     <p class="title2" style="margin-left:18px">
      <xsl:value-of disable-output-escaping="yes" select="$title"/>:</p>
    </td>
    <td border="0">
     <p>
      <xsl:value-of disable-output-escaping="yes" select="$value"/>
     </p>
    </td>
   </tr>
  </xsl:if>
 </xsl:template>
 <xsl:template name="entrytext">
  <xsl:param name="title"/>
  <xsl:param name="value"/>
  <xsl:if test="$value">
   <tr border="0">
    <td border="0">
     <p class="title2">
      <xsl:value-of disable-output-escaping="yes" select="$title"/>:</p>
     <p style="margin-left:20px">
      <xsl:value-of disable-output-escaping="yes" select="$value"/>
     </p>
    </td>
   </tr>
  </xsl:if>
 </xsl:template>
 <xsl:template name="clentry">
  <xsl:param name="title"/>
  <xsl:param name="values"/>
  <xsl:if test="$values">
   <tr border="0">
    <td border="0" width="100px">
     <p class="title2" style="margin-left:18px"><xsl:value-of select="$fc.Values"/>:</p>
    </td>
    <td border="0">
     <table class="values">
      <xsl:for-each select="$values">
       <tr>
        <xsl:variable name="value" select="."/>
        <td class="values">
         <p>
          <xsl:value-of disable-output-escaping="yes" select="./code"/>
         </p>
        </td>
        <td class="values">
         <xsl:if test="not(./code = ./label)">
          <p class="small">
           <b>
            <xsl:value-of disable-output-escaping="yes" select="./label"/>
           </b>
          </p>
         </xsl:if>
         <xsl:if test="./definition">
          <xsl:for-each select="./definition">
           <p class="small">
            <xsl:value-of disable-output-escaping="yes" select="."/>
           </p>
          </xsl:for-each>
         </xsl:if>
         <xsl:if test="./description">
          <xsl:for-each select="./description">
           <p class="small">
            <xsl:value-of disable-output-escaping="yes" select="."/>
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
 <xsl:template name="attTaggedValues">
  <xsl:param name="tvs"/>
  <xsl:if test="count($tvs) >= 1">
   <tr border="0">
    <td border="0" width="100px">
     <p class="title2" style="margin-left:18px">
      <xsl:value-of disable-output-escaping="yes" select="$fc.TaggedValues"/>
      <xsl:text>:</xsl:text>
     </p>
    </td>
    <td border="0">
     <xsl:call-template name="taggedValues">
      <xsl:with-param name="tvs" select="$tvs"/>
     </xsl:call-template>
    </td>
   </tr>
  </xsl:if>
 </xsl:template>
 <xsl:template name="entryTaggedValues">
  <xsl:param name="tvs"/>
  <xsl:if test="count($tvs) >= 1">
   <tr border="0">
    <td border="0">
     <p class="title2">
      <xsl:value-of disable-output-escaping="yes" select="$fc.TaggedValues"/>
      <xsl:text>:</xsl:text>
     </p>
     <p style="margin-left:20px">
      <xsl:call-template name="taggedValues">
       <xsl:with-param name="tvs" select="$tvs"/>
      </xsl:call-template>
     </p>
    </td>
   </tr>
  </xsl:if>
 </xsl:template>
 <xsl:template name="taggedValues">
  <xsl:param name="tvs"/>
  <table>
   <tr>
    <td width="5">
     <br/>
    </td>
    <td>
     <p class="title2">
      <xsl:value-of disable-output-escaping="yes" select="$fc.TagName"/>
     </p>
    </td>
    <td width="10">
     <br/>
    </td>
    <td>
     <p class="title2">
      <xsl:value-of disable-output-escaping="yes" select="$fc.TagValue"/>
     </p>
    </td>
   </tr>
   <xsl:for-each select="$tvs">
    <xsl:variable name="tv" select="."/>
    <tr>
     <td width="5">
      <br/>
     </td>
     <td valign="top">
      <p>
       <xsl:value-of disable-output-escaping="yes" select="$tv/@tag"/>
      </p>
     </td>
     <td width="10">
      <br/>
     </td>
     <td valign="top">
      <p>
       <xsl:value-of disable-output-escaping="yes" select="$tv/text()"/>
      </p>
     </td>
    </tr>
   </xsl:for-each>
  </table>
 </xsl:template>
 <xsl:template name="typename">
  <xsl:param name="type"/>
  <xsl:choose>
   <xsl:when test="$type = 'Feature Type'">
    <xsl:value-of select="$fc.FeatureType"/>
   </xsl:when>
   <xsl:when test="$type = 'Object Type'">
    <xsl:value-of select="$fc.ObjectType"/>
   </xsl:when>
   <xsl:when test="$type = 'Data Type'">
    <xsl:value-of select="$fc.DataType"/>
   </xsl:when>
   <xsl:when test="$type = 'Union Data Type'">
    <xsl:value-of select="$fc.UnionType"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="$type"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>
</xsl:stylesheet>
