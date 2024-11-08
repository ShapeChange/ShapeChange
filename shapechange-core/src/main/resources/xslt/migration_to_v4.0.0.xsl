<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="2.0">
  <!-- IMPORTANT: Pay attention to how XInclude statements are handled when executing this transformation. If XInclude statements shall be kept as-is (which is likely the case, if you just want to update the qualified class names of ShapeChange processes (validators, transformers, targets), it may be necessary to deactivate XInclude processing in your XSLT processor. For example, in oXygen XML editor, this can be done in the program settings > XML Parser > deactivate XInclude processing in the XInclude options of the parser. -->
  <xsl:variable name="map">
    <entry key="de.interactive_instruments.ShapeChange.Model.EA.EADocument">de.interactive_instruments.shapechange.ea.model.EADocument</entry>
    <entry key="de.interactive_instruments.ShapeChange.Model.Generic.GenericModel">de.interactive_instruments.shapechange.core.model.generic.GenericModel</entry>
    <entry key="de.interactive_instruments.ShapeChange.Model.Xmi10.Xmi10Document">de.interactive_instruments.shapechange.core.model.xmi10.Xmi10Document</entry>
    <entry key="de.interactive_instruments.ShapeChange.ModelValidation.Basic.BasicModelValidator">de.interactive_instruments.shapechange.core.modelvalidation.basic.BasicModelValidator</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.AppConfiguration.AppConfiguration">de.interactive_instruments.shapechange.core.target.appconfiguration.AppConfiguration</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace.ArcGISWorkspace">de.interactive_instruments.shapechange.ea.target.arcgisworkspace.ArcGISWorkspace</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.CDB.CDB">de.interactive_instruments.shapechange.core.target.cdb.CDB</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Codelists.CodelistDictionaries">de.interactive_instruments.shapechange.core.target.codelists.CodelistDictionaries</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Codelists.CodelistDictionariesML">de.interactive_instruments.shapechange.core.target.codelists.CodelistDictionariesML</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Codelists.CodelistRegister">de.interactive_instruments.shapechange.core.target.codelists.CodelistRegister</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Definitions.Definitions">de.interactive_instruments.shapechange.core.target.definitions.Definitions</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Diff.DiffTarget">de.interactive_instruments.shapechange.core.target.diff.DiffTarget</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.EA.UmlModel">de.interactive_instruments.shapechange.ea.target.uml.UmlModel</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.FOL2Schematron.FOL2Schematron">de.interactive_instruments.shapechange.core.target.fol2schematron.FOL2Schematron</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.FeatureCatalogue.FeatureCatalogue">de.interactive_instruments.shapechange.core.target.featurecatalogue.FeatureCatalogue</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.GeoPackage.GeoPackageTemplate">de.interactive_instruments.shapechange.core.target.geopackage.GeoPackageTemplate</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaTarget">de.interactive_instruments.shapechange.core.target.json.JsonSchemaTarget</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.KML.XSLT">de.interactive_instruments.shapechange.core.target.kml.XSLT</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Ldproxy.Config">de.interactive_instruments.shapechange.core.target.ldproxy.Config</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Target">de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Target</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Mapping.Excel">de.interactive_instruments.shapechange.core.target.mapping.Excel</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Metadata.ApplicationSchemaMetadata">de.interactive_instruments.shapechange.core.target.metadata.ApplicationSchemaMetadata</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.ModelExport.ModelExport">de.interactive_instruments.shapechange.core.target.modelexport.ModelExport</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Ontology.OWLISO19150">de.interactive_instruments.shapechange.core.target.ontology.OWLISO19150</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Ontology.RDF">de.interactive_instruments.shapechange.core.target.ontology.RDF</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.OpenApi.OpenApiDefinition">de.interactive_instruments.shapechange.core.target.openapi.OpenApiDefinition</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.ProfileTransfer.ProfileTransferEA">de.interactive_instruments.shapechange.ea.target.profiletransfer.ProfileTransferEA</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.ReplicationSchema.ReplicationXmlSchema">de.interactive_instruments.shapechange.core.target.replicationschema.ReplicationXmlSchema</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.SQL.SqlDdl">de.interactive_instruments.shapechange.core.target.sql.SqlDdl</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.Statistics.ApplicationSchemaStatistic">de.interactive_instruments.shapechange.core.target.statistics.ApplicationSchemaStatistic</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.XmlSchema.XmlSchema">de.interactive_instruments.shapechange.core.target.xmlschema.XmlSchema</entry>
    <entry key="de.interactive_instruments.ShapeChange.Target.gfs.GfsTemplateTarget">de.interactive_instruments.shapechange.core.target.gfs.GfsTemplateTarget</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.AIXM.AIXMSchemaMerger">de.interactive_instruments.shapechange.core.transformation.aixm.AIXMSchemaMerger</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Adding.AttributeCreator">de.interactive_instruments.shapechange.core.transformation.adding.AttributeCreator</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Adding.CodeListLoader">de.interactive_instruments.shapechange.core.transformation.adding.CodeListLoader</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.CityGML.CityGMLTransformer">de.interactive_instruments.shapechange.core.transformation.citygml.CityGMLTransformer</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Constraints.ConstraintConverter"
      >de.interactive_instruments.shapechange.core.transformation.constraints.ConstraintConverter</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Constraints.ConstraintLoader">de.interactive_instruments.shapechange.core.transformation.constraints.ConstraintLoader</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Constraints.ConstraintParserAndValidator"
      >de.interactive_instruments.shapechange.core.transformation.constraints.ConstraintParserAndValidator</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Descriptors.DescriptorTransformer"
      >de.interactive_instruments.shapechange.core.transformation.descriptors.DescriptorTransformer</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Flattening.AssociationClassMapper"
      >de.interactive_instruments.shapechange.core.transformation.flattening.AssociationClassMapper</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Flattening.Flattener">de.interactive_instruments.shapechange.core.transformation.flattening.Flattener</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Identity.IdentityTransform">de.interactive_instruments.shapechange.core.transformation.identity.IdentityTransform</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.LinkedDocument.LinkedDocumentTransformer"
      >de.interactive_instruments.shapechange.core.transformation.linkeddocument.LinkedDocumentTransformer</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.ModelCleaner.ModelCleaner">de.interactive_instruments.shapechange.core.transformation.modelcleaner.ModelCleaner</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Naming.NamingModifier">de.interactive_instruments.shapechange.core.transformation.naming.NamingModifier</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Profiling.ProfileConstraintTransformer"
      >de.interactive_instruments.shapechange.core.transformation.profiling.ProfileConstraintTransformer</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Profiling.ProfileLoader">de.interactive_instruments.shapechange.core.transformation.profiling.ProfileLoader</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.Profiling.Profiler">de.interactive_instruments.shapechange.core.transformation.profiling.Profiler</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.TaggedValues.TaggedValueTransformer"
      >de.interactive_instruments.shapechange.core.transformation.taggedvalues.TaggedValueTransformer</entry>
    <entry key="de.interactive_instruments.ShapeChange.Transformation.TypeConversion.TypeConverter">de.interactive_instruments.shapechange.core.transformation.typeconversion.TypeConverter</entry>
  </xsl:variable>
  <xsl:template match="node() | @*">
    <xsl:copy>
      <xsl:apply-templates select="node() | @*"/>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="@class">
    <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="exists($map/entry[@key = current()])">
          <xsl:value-of select="$map/entry[@key = current()]/text()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message terminate="yes">Error: No mapping was found for qualified class name: <xsl:value-of select="current()"
            />! Please contact the ShapeChange team so that the migration script can be updated.</xsl:message>
          <xsl:value-of select="current()"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
</xsl:stylesheet>
