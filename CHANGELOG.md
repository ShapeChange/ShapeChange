# Changelog

## [2.9.0](https://github.com/ShapeChange/ShapeChange/tree/2.9.0) (2019-12-30)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/2.8.0...2.9.0)

**Implemented enhancements:**

- General - improve performance of unit tests that do not write to EA repositories [\#222](https://github.com/ShapeChange/ShapeChange/issues/222)
- General - create new target that supports conversion to a GeoPackage template [\#221](https://github.com/ShapeChange/ShapeChange/issues/221)
- General - support custom Model interface implementations [\#219](https://github.com/ShapeChange/ShapeChange/issues/219)
- XmlSchema target / Schematron - support generation of value or nil reason checks, and content checks for nil reason [\#218](https://github.com/ShapeChange/ShapeChange/issues/218)
- General - Improve ShapeChange extensibility by introducing a mechanism for ShapeChange Target implementations to register conversion and encoding rules as well as requirements [\#217](https://github.com/ShapeChange/ShapeChange/issues/217)
- General - introduce alias constraintLoading for input parameter checkingConstraints [\#216](https://github.com/ShapeChange/ShapeChange/issues/216)
- UmlModel, SqlDdl, and ArcGIS targets - support setting EA element author and status [\#215](https://github.com/ShapeChange/ShapeChange/issues/215)
- ModelExport target - new rule to suppress isNavigable, and remove default from isNavigable [\#214](https://github.com/ShapeChange/ShapeChange/issues/214)
- General - support additional stereotypes defined by input parameter [\#213](https://github.com/ShapeChange/ShapeChange/issues/213)
- XmlSchema target / Schematron - Support xslt2 query binding [\#209](https://github.com/ShapeChange/ShapeChange/issues/209)
- TypeConverter transformation - dissolve association - provide a way to control inlineOrByReference setting of resulting attribute [\#207](https://github.com/ShapeChange/ShapeChange/issues/207)
- Feature Catalogue target - Support code lists and enumerations in frame-based HTML catalog [\#204](https://github.com/ShapeChange/ShapeChange/issues/204)
- EA model - Ensure plain text formatting for model element notes [\#203](https://github.com/ShapeChange/ShapeChange/issues/203)
- ldproxy target - support for readable tag [\#202](https://github.com/ShapeChange/ShapeChange/issues/202)
- SQL DDL target - Support SQLite/SpatiaLite [\#200](https://github.com/ShapeChange/ShapeChange/issues/200)
- General - Support property metadata [\#199](https://github.com/ShapeChange/ShapeChange/issues/199)
- ModelExport target - new rule to omit descriptors [\#197](https://github.com/ShapeChange/ShapeChange/issues/197)
- SCXML - Add ability to explicitly encode descriptive information / comments for constraints [\#196](https://github.com/ShapeChange/ShapeChange/issues/196)
- Application Schema Metadata target - new rule to identify properties with specific tagged values [\#195](https://github.com/ShapeChange/ShapeChange/issues/195)
- ModelExport target - Suppress code and enum characteristics without semantic meaning [\#194](https://github.com/ShapeChange/ShapeChange/issues/194)
- General - Add an input parameter to specify an XML Schema with which to validate SCXML [\#193](https://github.com/ShapeChange/ShapeChange/issues/193)
- General - Extend property content model with ownership information [\#192](https://github.com/ShapeChange/ShapeChange/issues/192)
- SQL DDL target - Oracle: Support length qualifier for data types [\#191](https://github.com/ShapeChange/ShapeChange/issues/191)
- General - Support code lists modelled as EA elements of type enumeration [\#189](https://github.com/ShapeChange/ShapeChange/issues/189)
- FeatureCatalogue target - Represent tagged values of codes and enums in temporary XML [\#188](https://github.com/ShapeChange/ShapeChange/issues/188)
- Loading SCXML: input configuration parameters and elements should be taken into account  [\#162](https://github.com/ShapeChange/ShapeChange/issues/162)

**Fixed bugs:**

- XmlSchema target - OCL constraints defined for properties not encoded in Schematron [\#205](https://github.com/ShapeChange/ShapeChange/issues/205)
- FeatureCatalogue target - Keep sorting of diagrams specified while loading the input model [\#190](https://github.com/ShapeChange/ShapeChange/issues/190)
- General - Explicit target namespaces in generic model and SCXML cause unexpected behavior [\#181](https://github.com/ShapeChange/ShapeChange/issues/181)

**Closed issues:**

- The semantic validation of the ShapeChange configuration detected one or more errors [\#206](https://github.com/ShapeChange/ShapeChange/issues/206)
- Ontology target : different behaviors for UML tagged values as template for DescriptorTarget [\#165](https://github.com/ShapeChange/ShapeChange/issues/165)

**Merged pull requests:**

- Java11 [\#225](https://github.com/ShapeChange/ShapeChange/pull/225) ([jechterhoff](https://github.com/jechterhoff))
- Geopackage [\#224](https://github.com/ShapeChange/ShapeChange/pull/224) ([jechterhoff](https://github.com/jechterhoff))
- Refactor setting SRSs [\#223](https://github.com/ShapeChange/ShapeChange/pull/223) ([jechterhoff](https://github.com/jechterhoff))
- Ugas [\#220](https://github.com/ShapeChange/ShapeChange/pull/220) ([jechterhoff](https://github.com/jechterhoff))
- Merge PODS SL branch into master [\#212](https://github.com/ShapeChange/ShapeChange/pull/212) ([jechterhoff](https://github.com/jechterhoff))
- validateStereotypesCache\(\) for EA models [\#211](https://github.com/ShapeChange/ShapeChange/pull/211) ([heidivanparys](https://github.com/heidivanparys))
-  Add support for data types modeled as UML data type classifier [\#210](https://github.com/ShapeChange/ShapeChange/pull/210) ([heidivanparys](https://github.com/heidivanparys))
- Ea txt [\#208](https://github.com/ShapeChange/ShapeChange/pull/208) ([jechterhoff](https://github.com/jechterhoff))
- Sdfe [\#201](https://github.com/ShapeChange/ShapeChange/pull/201) ([jechterhoff](https://github.com/jechterhoff))
- Change behaviour length qualifier [\#198](https://github.com/ShapeChange/ShapeChange/pull/198) ([heidivanparys](https://github.com/heidivanparys))

## [2.8.0](https://github.com/ShapeChange/ShapeChange/tree/2.8.0) (2019-03-28)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/2.7.0...2.8.0)

**Implemented enhancements:**

- Application Schema Metadata target - new rule to identify type usage [\#187](https://github.com/ShapeChange/ShapeChange/issues/187)
- ModelExport target - add parameter to specify schema location [\#186](https://github.com/ShapeChange/ShapeChange/issues/186)
- General - Add "schema" to set of well-known stereotypes [\#185](https://github.com/ShapeChange/ShapeChange/issues/185)
- ModelExport - Add schemaLocation to SCXML [\#183](https://github.com/ShapeChange/ShapeChange/issues/183)
- Flattener transformation - Add parameter to control property copy behavior in case of duplicate property [\#180](https://github.com/ShapeChange/ShapeChange/issues/180)

**Fixed bugs:**

- ModelExport - empty \<sc:properties\> should not be created [\#184](https://github.com/ShapeChange/ShapeChange/issues/184)

**Merged pull requests:**

- Pods [\#182](https://github.com/ShapeChange/ShapeChange/pull/182) ([jechterhoff](https://github.com/jechterhoff))

## [2.7.0](https://github.com/ShapeChange/ShapeChange/tree/2.7.0) (2018-12-10)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/2.6.0...2.7.0)

**Implemented enhancements:**

- Feature Catalogue target - Custom style for docx formatted catalogue [\#178](https://github.com/ShapeChange/ShapeChange/issues/178)
- General - Provide configuration option to load all tagged values [\#177](https://github.com/ShapeChange/ShapeChange/issues/177)
- XML Schema target - support mapping of properties [\#176](https://github.com/ShapeChange/ShapeChange/issues/176)
- TypeConverter transformation - dissolve association - define new type by tagged value [\#175](https://github.com/ShapeChange/ShapeChange/issues/175)
- SQL DDL target - replication schema - add geometry annotation [\#174](https://github.com/ShapeChange/ShapeChange/issues/174)
- FeatureCatalogue target - Adding a logo [\#173](https://github.com/ShapeChange/ShapeChange/issues/173)
- FeatureCatalogue target - Option to prevent inclusion of inherited constraints [\#172](https://github.com/ShapeChange/ShapeChange/issues/172)
- XmlSchema target - Add rule to encode descriptors in appInfo annotation [\#170](https://github.com/ShapeChange/ShapeChange/issues/170)
- Ontology target - Property generalization and enrichment [\#167](https://github.com/ShapeChange/ShapeChange/issues/167)
- Ontology target - parameter to suppress log messages about unsupported categories of classes [\#163](https://github.com/ShapeChange/ShapeChange/issues/163)
- Ontology target - Add rule to create rdfs:label of a property from its local name [\#145](https://github.com/ShapeChange/ShapeChange/issues/145)

**Fixed bugs:**

- Common transformer functionality - setting tagged values for associations does not work [\#169](https://github.com/ShapeChange/ShapeChange/issues/169)
- Ontology target - default type implementation does not apply if no conversion rule for codelist is defined [\#154](https://github.com/ShapeChange/ShapeChange/issues/154)

**Closed issues:**

- Feature Catalogue in xml format [\#168](https://github.com/ShapeChange/ShapeChange/issues/168)
- Ontology target : WARN  BaseXMLWriter:81 - Cannot block rule \<http://www.w3.org/TR/rdf-syntax-grammar\#daml:collection\> [\#164](https://github.com/ShapeChange/ShapeChange/issues/164)

**Merged pull requests:**

- Sdfe [\#179](https://github.com/ShapeChange/ShapeChange/pull/179) ([jechterhoff](https://github.com/jechterhoff))
- Owl [\#171](https://github.com/ShapeChange/ShapeChange/pull/171) ([jechterhoff](https://github.com/jechterhoff))

## [2.6.0](https://github.com/ShapeChange/ShapeChange/tree/2.6.0) (2018-09-11)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/2.5.1...2.6.0)

**Implemented enhancements:**

- New transformer to load codes of externally managed code list into model [\#150](https://github.com/ShapeChange/ShapeChange/issues/150)
- ConstraintConverter & XmlSchema target - Derive code list restrictions from OCL and use the information to create Schematron assertions for code list checks [\#149](https://github.com/ShapeChange/ShapeChange/issues/149)
- XmlSchema target / Schematron - Improve translation of oclIsTypeOf\(\) [\#148](https://github.com/ShapeChange/ShapeChange/issues/148)
- SqlDdl and ArcGIS Workspace targets - Represent specific tagged values in UML model created by the target [\#143](https://github.com/ShapeChange/ShapeChange/issues/143)
- SqlDdl target - Support range checks [\#142](https://github.com/ShapeChange/ShapeChange/issues/142)
- SqlDdl target - Support using short names when creating constraint name [\#141](https://github.com/ShapeChange/ShapeChange/issues/141)
- SqlDdl target - Support foreign key options \(on delete, on update\) [\#140](https://github.com/ShapeChange/ShapeChange/issues/140)
- ArcGIS Workspace target - Add support for creation of ArcGIS subtypes [\#158](https://github.com/ShapeChange/ShapeChange/issues/158)
- ldproxy target \(new\) - generate configuration file [\#153](https://github.com/ShapeChange/ShapeChange/issues/153)
- XmlSchema target / Schematron - support single byReference property in iterator expression [\#152](https://github.com/ShapeChange/ShapeChange/issues/152)
- General - Add transformer to create OCL constraints for a profile schema [\#138](https://github.com/ShapeChange/ShapeChange/issues/138)
- XmlSchema target - Add mechanism to skip creation of XSDs \(if just Schematron is of interest\) [\#137](https://github.com/ShapeChange/ShapeChange/issues/137)
- General - Extend the mechanism to select the schemas for processing to transformers [\#136](https://github.com/ShapeChange/ShapeChange/issues/136)
- XmlSchema target / Schematron - Improve support for cast in OCL expression [\#133](https://github.com/ShapeChange/ShapeChange/issues/133)
- General - Update and document code list dictionary targets [\#129](https://github.com/ShapeChange/ShapeChange/issues/129)
- XmlSchema target - Create @gco:isoType attribute for elements in a metadata profile \(ISO 19139 encoded\) [\#127](https://github.com/ShapeChange/ShapeChange/issues/127)
- XmlSchema target: Add configuration parameter to control naming of generated Schematron files [\#31](https://github.com/ShapeChange/ShapeChange/issues/31)
- New target: ldproxy configurations [\#159](https://github.com/ShapeChange/ShapeChange/pull/159) ([cportele](https://github.com/cportele))

**Fixed bugs:**

- FeatureCatalogue target - XSL transformation with external JRE fails \(NPE for hrefMappings in XsltWriter\) [\#157](https://github.com/ShapeChange/ShapeChange/issues/157)
- XML Schema target - fix encoding of \<\<union\>\> subtype without attributes [\#156](https://github.com/ShapeChange/ShapeChange/issues/156)
- Flattener transformation - remove type logic does not work for multiple types with same name [\#155](https://github.com/ShapeChange/ShapeChange/issues/155)
- XmlSchema target - Fix type choice for code list valued property [\#139](https://github.com/ShapeChange/ShapeChange/issues/139)
- XmlSchema target - Fix ISO 19139 union encoding [\#134](https://github.com/ShapeChange/ShapeChange/issues/134)
- XmlSchema target / Schematron - Missing parentheses for inlineOrByReference encoding of a property [\#132](https://github.com/ShapeChange/ShapeChange/issues/132)
- XmlSchema target / Schematron - Wrong id attribute in XPath for ISO 19139 encoded property type [\#126](https://github.com/ShapeChange/ShapeChange/issues/126)
- XmlSchema target / Schematron - Incorrect XPath encoding for ISO 19139 elements with simple content [\#125](https://github.com/ShapeChange/ShapeChange/issues/125)
- XmlSchema target / Schematron - Incorrect XPath encoding for ISO 19139 elements that are not referenceable [\#124](https://github.com/ShapeChange/ShapeChange/issues/124)

**Closed issues:**

- UML taggedValue cannot be written to XML schema [\#146](https://github.com/ShapeChange/ShapeChange/issues/146)
- StandardMapEntries for CI\_Responsibility [\#144](https://github.com/ShapeChange/ShapeChange/issues/144)

**Merged pull requests:**

- Schematron [\#161](https://github.com/ShapeChange/ShapeChange/pull/161) ([jechterhoff](https://github.com/jechterhoff))
- ArcGIS [\#160](https://github.com/ShapeChange/ShapeChange/pull/160) ([jechterhoff](https://github.com/jechterhoff))
- Schematron [\#151](https://github.com/ShapeChange/ShapeChange/pull/151) ([jechterhoff](https://github.com/jechterhoff))
- Sql and arc gis [\#147](https://github.com/ShapeChange/ShapeChange/pull/147) ([jechterhoff](https://github.com/jechterhoff))

## [2.5.1](https://github.com/ShapeChange/ShapeChange/tree/2.5.1) (2018-02-23)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/ShapeChange-2.5.0...2.5.1)

**Implemented enhancements:**

- XmlSchema target - add globalIdentifier annotation [\#131](https://github.com/ShapeChange/ShapeChange/issues/131)
- FeatureCatalogue target - include tagged values [\#130](https://github.com/ShapeChange/ShapeChange/issues/130)
- ArcGIS Workspace target - Add configuration validator to check that ArcGIS MDG Technology is enabled in EA [\#123](https://github.com/ShapeChange/ShapeChange/issues/123)
- General - Recognise stereotype \<\<retired\>\> [\#108](https://github.com/ShapeChange/ShapeChange/issues/108)

**Fixed bugs:**

- SqlDdl target - Database UML model does not contain documentation for code list related columns [\#128](https://github.com/ShapeChange/ShapeChange/issues/128)
- UmlModel target - Wrong UML Profile / MDG for stereotypes [\#122](https://github.com/ShapeChange/ShapeChange/issues/122)

**Closed issues:**

- Use of current\(\) [\#13](https://github.com/ShapeChange/ShapeChange/issues/13)
- Context does not start with "//" [\#12](https://github.com/ShapeChange/ShapeChange/issues/12)

**Merged pull requests:**

- Extend feature catalogue target to include tagged values [\#135](https://github.com/ShapeChange/ShapeChange/pull/135) ([jechterhoff](https://github.com/jechterhoff))
- add \<\<retired\>\> to known stereotypes [\#121](https://github.com/ShapeChange/ShapeChange/pull/121) ([cportele](https://github.com/cportele))

## [ShapeChange-2.5.0](https://github.com/ShapeChange/ShapeChange/tree/ShapeChange-2.5.0) (2018-01-16)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/ShapeChange-2.4.0...ShapeChange-2.5.0)

**Implemented enhancements:**

- SqlDdl target - Create separate DDL files for code insert statements and spatial index statements. [\#118](https://github.com/ShapeChange/ShapeChange/issues/118)
- ArcGIS Workspace target - Simple encoding for reflexive relationship [\#117](https://github.com/ShapeChange/ShapeChange/issues/117)
- ArcGIS Workspace target - Create attribute index [\#116](https://github.com/ShapeChange/ShapeChange/issues/116)
- SqlDdl target - Encoding of unique constraints [\#115](https://github.com/ShapeChange/ShapeChange/issues/115)
- General - New transformer to process linked documents [\#114](https://github.com/ShapeChange/ShapeChange/issues/114)
- SqlDdl target - Support encoding of linked documents [\#113](https://github.com/ShapeChange/ShapeChange/issues/113)
- General - Support loading and merging of linked documents [\#112](https://github.com/ShapeChange/ShapeChange/issues/112)
- SqlDdl target - Create an EA database model [\#111](https://github.com/ShapeChange/ShapeChange/issues/111)
- SqlDdl target - Establish package hierarchy for table elements in database model [\#110](https://github.com/ShapeChange/ShapeChange/issues/110)
- UmlModel target - Support encoding of linked documents [\#109](https://github.com/ShapeChange/ShapeChange/issues/109)
- UmlModel target - Prevent tagged values with null values from causing exceptions [\#15](https://github.com/ShapeChange/ShapeChange/issues/15)

**Fixed bugs:**

- UmlModel target does not check whether EA repository is sufficiently opened during creation of the target [\#14](https://github.com/ShapeChange/ShapeChange/issues/14)

**Closed issues:**

- Minimum JRE version is 1.8 instead of 1.6 [\#43](https://github.com/ShapeChange/ShapeChange/issues/43)

**Merged pull requests:**

- Extend SQL target to create database UML model, fixes and extensions to UML and ArcGIS targets [\#120](https://github.com/ShapeChange/ShapeChange/pull/120) ([jechterhoff](https://github.com/jechterhoff))

## [ShapeChange-2.4.0](https://github.com/ShapeChange/ShapeChange/tree/ShapeChange-2.4.0) (2017-11-22)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/ShapeChange-2.3.0...ShapeChange-2.4.0)

**Implemented enhancements:**

- Ontology target - no class prefix for schema unique property with local scope [\#103](https://github.com/ShapeChange/ShapeChange/issues/103)
- SQL DDL target - Option to remove empty lines in output [\#90](https://github.com/ShapeChange/ShapeChange/issues/90)
- SQL DDL target - Add text from external file to DDL output [\#89](https://github.com/ShapeChange/ShapeChange/issues/89)
- General - Transformation to support inheritance of tagged values [\#88](https://github.com/ShapeChange/ShapeChange/issues/88)
- General - Transformation to dissolve associations [\#87](https://github.com/ShapeChange/ShapeChange/issues/87)
- SQL DDL target - class attribute as primary key [\#86](https://github.com/ShapeChange/ShapeChange/issues/86)
- General - addition of time and version in target output [\#85](https://github.com/ShapeChange/ShapeChange/issues/85)
- General - XSL transformation on target output [\#84](https://github.com/ShapeChange/ShapeChange/issues/84)
- SQL DDL target - documentation via explicit comments [\#83](https://github.com/ShapeChange/ShapeChange/issues/83)
- General - Add new target to transfer a profile into an EA repository [\#81](https://github.com/ShapeChange/ShapeChange/issues/81)
- General - New transformation to load profiles [\#80](https://github.com/ShapeChange/ShapeChange/issues/80)
- General - Add model export to / import from ShapeChange specific format [\#79](https://github.com/ShapeChange/ShapeChange/issues/79)
- General - Profile parameters [\#78](https://github.com/ShapeChange/ShapeChange/issues/78)
- General - New descriptor global identifier [\#76](https://github.com/ShapeChange/ShapeChange/issues/76)
- JSON Schema target - Suppression of model elements [\#75](https://github.com/ShapeChange/ShapeChange/issues/75)
- SQL DDL target - Suppression of model elements [\#74](https://github.com/ShapeChange/ShapeChange/issues/74)
- SQL DDL target - Avoid collisions between names of check constraints [\#73](https://github.com/ShapeChange/ShapeChange/issues/73)
- SQL DDL target - Allow configuration of primary key specification [\#72](https://github.com/ShapeChange/ShapeChange/issues/72)
- SQL DDL target - Check constraint to ensure time fields are zero if Date is mapped to Oracles DATE data type [\#71](https://github.com/ShapeChange/ShapeChange/issues/71)
- SQL DDL target - Oracle: Setting dimension values for geometry metadata [\#70](https://github.com/ShapeChange/ShapeChange/issues/70)
- SQL DDL target - size of character strings in replication schemas [\#69](https://github.com/ShapeChange/ShapeChange/issues/69)
- SQL DDL target - nillable for replication schema elements [\#68](https://github.com/ShapeChange/ShapeChange/issues/68)
- SQL DDL target - Ignore case when normalizing names [\#67](https://github.com/ShapeChange/ShapeChange/issues/67)
- Merge functionality of Replication XML Schema target into SQL DDL target [\#66](https://github.com/ShapeChange/ShapeChange/issues/66)
- SQL DDL target - Improve handling of default values [\#65](https://github.com/ShapeChange/ShapeChange/issues/65)
- SQL DDL target - Add support for SQL Server [\#64](https://github.com/ShapeChange/ShapeChange/issues/64)
- Flatten multiplicity - don't copy if maxOccurs is 1 [\#60](https://github.com/ShapeChange/ShapeChange/issues/60)
- Ontology target - rule to add predicate for properties with value type that has tagged value 'vocabulary' or 'codeList' [\#55](https://github.com/ShapeChange/ShapeChange/issues/55)
- General - add documentation as possible replacement in derivedDocumentation\(...\) [\#54](https://github.com/ShapeChange/ShapeChange/issues/54)

**Fixed bugs:**

- XML Schema target - facet on basic type [\#53](https://github.com/ShapeChange/ShapeChange/issues/53)

**Closed issues:**

- Namespace for ISO 19150-2 [\#92](https://github.com/ShapeChange/ShapeChange/issues/92)
- SQL DDL target - names of check constraints can collide in Oracle [\#44](https://github.com/ShapeChange/ShapeChange/issues/44)

**Merged pull requests:**

- Merge sql \(and other\) enhancements [\#107](https://github.com/ShapeChange/ShapeChange/pull/107) ([jechterhoff](https://github.com/jechterhoff))
- Add support for default string values within " [\#106](https://github.com/ShapeChange/ShapeChange/pull/106) ([heidivanparys](https://github.com/heidivanparys))
- Create the resulting log files with UTF-8 encoding [\#105](https://github.com/ShapeChange/ShapeChange/pull/105) ([heidivanparys](https://github.com/heidivanparys))
- Do not add import when targetElement not present [\#104](https://github.com/ShapeChange/ShapeChange/pull/104) ([heidivanparys](https://github.com/heidivanparys))
- New classes in package Target are not ignored [\#102](https://github.com/ShapeChange/ShapeChange/pull/102) ([heidivanparys](https://github.com/heidivanparys))
- Add concept of "main schema" in Converter and in SqlDdl [\#101](https://github.com/ShapeChange/ShapeChange/pull/101) ([heidivanparys](https://github.com/heidivanparys))
- Sort enumerations alphabetically [\#100](https://github.com/ShapeChange/ShapeChange/pull/100) ([heidivanparys](https://github.com/heidivanparys))
- Update namespace handling ReplicationSchema [\#99](https://github.com/ShapeChange/ShapeChange/pull/99) ([heidivanparys](https://github.com/heidivanparys))
- Fix configuration loading of the process mode of targets [\#98](https://github.com/ShapeChange/ShapeChange/pull/98) ([heidivanparys](https://github.com/heidivanparys))
- ReplicationSchema: no maxLength unless size set [\#97](https://github.com/ShapeChange/ShapeChange/pull/97) ([heidivanparys](https://github.com/heidivanparys))
- Add logging when setting tagged values [\#96](https://github.com/ShapeChange/ShapeChange/pull/96) ([heidivanparys](https://github.com/heidivanparys))
- Update DDL encoding of complex data types [\#95](https://github.com/ShapeChange/ShapeChange/pull/95) ([heidivanparys](https://github.com/heidivanparys))
- Add error message when a default encoding rule is specified but not configured [\#94](https://github.com/ShapeChange/ShapeChange/pull/94) ([heidivanparys](https://github.com/heidivanparys))
- Add log statements when connecting to and reading an eap-file [\#93](https://github.com/ShapeChange/ShapeChange/pull/93) ([heidivanparys](https://github.com/heidivanparys))
- Add more information to manifest [\#91](https://github.com/ShapeChange/ShapeChange/pull/91) ([heidivanparys](https://github.com/heidivanparys))
- Profiling enhancements [\#82](https://github.com/ShapeChange/ShapeChange/pull/82) ([jechterhoff](https://github.com/jechterhoff))
- Major revision of SQL DDL target + enhancements [\#77](https://github.com/ShapeChange/ShapeChange/pull/77) ([jechterhoff](https://github.com/jechterhoff))
- Update SdoDimArrayExpression.toString\(\) [\#63](https://github.com/ShapeChange/ShapeChange/pull/63) ([heidivanparys](https://github.com/heidivanparys))
- Make ToCharExpression more general [\#62](https://github.com/ShapeChange/ShapeChange/pull/62) ([heidivanparys](https://github.com/heidivanparys))
- Update setting of minOccurs in ReplicationSchemaVisitor [\#61](https://github.com/ShapeChange/ShapeChange/pull/61) ([heidivanparys](https://github.com/heidivanparys))
- Add logging to Flattener when using regexes [\#59](https://github.com/ShapeChange/ShapeChange/pull/59) ([heidivanparys](https://github.com/heidivanparys))
- add rule-owl-prop-external-reference [\#57](https://github.com/ShapeChange/ShapeChange/pull/57) ([jechterhoff](https://github.com/jechterhoff))
- improves facet detection, adds documentation to derivedDocumentation\(\) [\#56](https://github.com/ShapeChange/ShapeChange/pull/56) ([jechterhoff](https://github.com/jechterhoff))

## [ShapeChange-2.3.0](https://github.com/ShapeChange/ShapeChange/tree/ShapeChange-2.3.0) (2017-01-16)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/ShapeChange-2.2.0...ShapeChange-2.3.0)

**Implemented enhancements:**

- ArcGIS Target - Length for properties with code list or enumeration as value type [\#51](https://github.com/ShapeChange/ShapeChange/issues/51)
- ArcGIS Target - Precision and scale [\#46](https://github.com/ShapeChange/ShapeChange/issues/46)
- ArcGIS Target - Set field type for CodedValueDomain [\#42](https://github.com/ShapeChange/ShapeChange/issues/42)
- Transformations - New transformer to convert enumerations to code lists [\#41](https://github.com/ShapeChange/ShapeChange/issues/41)
- SQL DDL target - foreign key oracle naming style [\#39](https://github.com/ShapeChange/ShapeChange/issues/39)
- NamingModifier transformation - rule to achieve Oracle style naming [\#38](https://github.com/ShapeChange/ShapeChange/issues/38)
- Profiler transformation - allow whitespace around profile identifiers [\#37](https://github.com/ShapeChange/ShapeChange/issues/37)
- SQL DDL target - rule to encode code list as table [\#36](https://github.com/ShapeChange/ShapeChange/issues/36)
- ArcGIS Workspace target - setting HasZ and HasM [\#35](https://github.com/ShapeChange/ShapeChange/issues/35)
- Ontology target - Update the target based on ISO 19150-2 [\#30](https://github.com/ShapeChange/ShapeChange/issues/30)
- ArcGIS Workspace target - set initial values [\#23](https://github.com/ShapeChange/ShapeChange/issues/23)
- ArcGIS Workspace target - Make max name length configurable [\#22](https://github.com/ShapeChange/ShapeChange/issues/22)
- ArcGIS Workspace target - isNullable only if property is optional [\#21](https://github.com/ShapeChange/ShapeChange/issues/21)
- ArcGIS Workspace target - range restrictions from OCL constraints [\#20](https://github.com/ShapeChange/ShapeChange/issues/20)
- ArcGIS Workspace target - length constraints [\#18](https://github.com/ShapeChange/ShapeChange/issues/18)

**Fixed bugs:**

- ArcGIS Workspace target - description tagged value for domains [\#19](https://github.com/ShapeChange/ShapeChange/issues/19)

**Merged pull requests:**

- Pods [\#52](https://github.com/ShapeChange/ShapeChange/pull/52) ([jechterhoff](https://github.com/jechterhoff))
- Add xslTransformerFactory in testEA\_Docx.xml [\#49](https://github.com/ShapeChange/ShapeChange/pull/49) ([heidivanparys](https://github.com/heidivanparys))
- Split class BasicTest into multiple test classes [\#48](https://github.com/ShapeChange/ShapeChange/pull/48) ([heidivanparys](https://github.com/heidivanparys))

## [ShapeChange-2.2.0](https://github.com/ShapeChange/ShapeChange/tree/ShapeChange-2.2.0) (2016-11-09)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/ShapeChange-2.1.0...ShapeChange-2.2.0)

**Implemented enhancements:**

- General - Mechanism to semantically validate a ShapeChange configuration file [\#32](https://github.com/ShapeChange/ShapeChange/issues/32)
- GenericModel - diagrams should be copied as well [\#27](https://github.com/ShapeChange/ShapeChange/issues/27)
- Transformations - validating constraints in transformed model [\#26](https://github.com/ShapeChange/ShapeChange/issues/26)
- General - New Transformer for association classes [\#25](https://github.com/ShapeChange/ShapeChange/issues/25)
- OCL - code list value validation [\#24](https://github.com/ShapeChange/ShapeChange/issues/24)
- Model - Add support for isUnique [\#10](https://github.com/ShapeChange/ShapeChange/issues/10)
- XmlSchema target - Extend "rule-xsd-cls-mixin-classes-non-mixin-supertypes" [\#9](https://github.com/ShapeChange/ShapeChange/issues/9)
- FeatureCatalogue target - Include code list / vocabulary URI [\#7](https://github.com/ShapeChange/ShapeChange/issues/7)
- General - New application schema metadata target [\#4](https://github.com/ShapeChange/ShapeChange/issues/4)
- General - Enhance status code reporting while processing [\#3](https://github.com/ShapeChange/ShapeChange/issues/3)
- FeatureCatalogue target - Show differences between input model and a reference model [\#2](https://github.com/ShapeChange/ShapeChange/issues/2)
- FeatureCatalogue target - Include UML diagrams in frame based HTML feature catalogue [\#1](https://github.com/ShapeChange/ShapeChange/issues/1)

**Fixed bugs:**

- ArcGIS Workspace target performs Type modifications [\#29](https://github.com/ShapeChange/ShapeChange/issues/29)
- Avoid name collisions [\#11](https://github.com/ShapeChange/ShapeChange/issues/11)

**Merged pull requests:**

- Merge changes from TB12 [\#34](https://github.com/ShapeChange/ShapeChange/pull/34) ([jechterhoff](https://github.com/jechterhoff))
- tb12 results [\#33](https://github.com/ShapeChange/ShapeChange/pull/33) ([jechterhoff](https://github.com/jechterhoff))
- closes \#27 - keep diagram info when creating GenericModel [\#28](https://github.com/ShapeChange/ShapeChange/pull/28) ([jechterhoff](https://github.com/jechterhoff))
- Resolve issues 9, 10 and 11 [\#17](https://github.com/ShapeChange/ShapeChange/pull/17) ([cportele](https://github.com/cportele))
- code list URI for feature catalogue [\#8](https://github.com/ShapeChange/ShapeChange/pull/8) ([jechterhoff](https://github.com/jechterhoff))
- Schema metadata, status codes, schema diff, diagrams in frame html FC [\#5](https://github.com/ShapeChange/ShapeChange/pull/5) ([jechterhoff](https://github.com/jechterhoff))

## [ShapeChange-2.1.0](https://github.com/ShapeChange/ShapeChange/tree/ShapeChange-2.1.0) (2016-03-02)

[Full Changelog](https://github.com/ShapeChange/ShapeChange/compare/3957619d6dec94cb2e8f20ccd3985967bcff8d95...ShapeChange-2.1.0)



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
