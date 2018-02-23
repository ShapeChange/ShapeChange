# Change Log

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

- SQL DDL target - Improve handling of default values [\#65](https://github.com/ShapeChange/ShapeChange/issues/65)
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


\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*