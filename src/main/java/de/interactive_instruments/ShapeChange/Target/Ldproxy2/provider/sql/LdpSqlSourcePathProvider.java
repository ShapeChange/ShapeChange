/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfos;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSpecialPropertiesInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpUtil;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Constants;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Target;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.AbstractLdpSourcePathProvider;
import de.interactive_instruments.ShapeChange.Target.sql_encoding_util.SqlEncodingInfos;
import de.interactive_instruments.ShapeChange.Target.sql_encoding_util.SqlPropertyEncodingInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSqlSourcePathProvider extends AbstractLdpSourcePathProvider {

    protected LdpSqlProviderHelper sqlProviderHelper = new LdpSqlProviderHelper();

    protected ShapeChangeResult result;
    protected Ldproxy2Target target;
    protected MessageSource msgSource;

    protected SqlEncodingInfos encodingInfos;

    public LdpSqlSourcePathProvider(Ldproxy2Target target) {

	this.target = target;
	this.result = target.result;
	this.msgSource = target;

	this.encodingInfos = Ldproxy2Target.sqlEncodingInfos;
    }

    /**
     * @param pi                   the property for which to construct the source
     *                             path on property level
     * @param alreadyVisitedPiList information about previous steps in the source
     *                             path; can be analyzed to detect special cases
     *                             (e.g. lists of data type valued properties)
     * @param contextx             - The context in which the property is encoded
     * @return - TBD
     */
    @Override
    public LdpSourcePathInfos sourcePathPropertyLevel(PropertyInfo pi, List<PropertyInfo> alreadyVisitedPiList,
	    LdpPropertyEncodingContext contextx) {

	LdpSqlPropertyEncodingContext context = (LdpSqlPropertyEncodingContext) contextx;

	LdpSourcePathInfos spRes = new LdpSourcePathInfos();
	spRes.setPi(pi);
	spRes.setContext(context);

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);
	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (!encodingInfos.isEmpty()) {

	    SortedSet<SqlPropertyEncodingInfo> speis = getPropertyEncodingInfos(pi, context);

	    for (SqlPropertyEncodingInfo spei : speis) {

		Optional<String> idSourcePath = Optional.empty();
		Optional<String> valueSourcePath = Optional.empty();
		Optional<Type> idValueType = Optional.empty();
		String refType = null;
		String refUriTemplate = null;
		String targetTable = spei.getTargetTable();
		boolean targetsSingleValue = (new Multiplicity(spei.getPropertyMultiplicity())).maxOccurs == 1;

		if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)
			&& pme.hasTargetType()) {

		    // the property value type is mapped

		    if ("LINK".equalsIgnoreCase(pme.getTargetType())) {
			refUriTemplate = urlTemplateForValueType(pi);
			if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
			    idSourcePath = Optional.of(spei.getIdSourcePath());
			    idValueType = Optional.of(determineIdValueType(spei.getIdValueType()));
			}
		    } else {
			valueSourcePath = Optional.of(spei.getValueSourcePath());
		    }

		} else {

		    // the property value type is not mapped - so is available in the database

		    valueSourcePath = Optional.of(spei.getValueSourcePath());

		    if (isImplementedAsFeatureReference(pi)) {
			idSourcePath = Optional.of(spei.getIdSourcePath());
			idValueType = Optional.of(determineIdValueType(spei.getIdValueType()));
			refType = spei.getPropertyValueType().toLowerCase(Locale.ENGLISH);
		    }
		}

		LdpSqlSourcePathInfo spi = new LdpSqlSourcePathInfo(idSourcePath, valueSourcePath, idValueType, refType,
			refUriTemplate, targetsSingleValue, targetTable);
		spRes.addSourcePathInfo(spi);
	    }
	}

	/*
	 * If we have identified source path information via the sql encoding infos,
	 * return them. Otherwise, try to determine them the old-fashioned way.
	 */
	if (!spRes.isEmpty()) {
	    return spRes;
	}

	Optional<String> idSourcePath = Optional.empty();
	Optional<String> valueSourcePath = Optional.empty();
	Optional<Type> idValueType = null;
	String refType = null;
	String refUriTemplate = null;
	String targetTable = null;
	boolean targetsSingleValue = pi.cardinality().maxOccurs == 1;

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1
		    valueSourcePath = Optional.of(databaseColumnName(pi));
		    targetsSingleValue = true;

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    // property type is mapped to LINK

		    String tableName = databaseTableNameForMappedValueType(pi);
		    targetTable = tableName;

		    // TODO: PK Column of target table not yet configurable
		    // -> define primary key column for table mapping
//		    String pkTargetTable = Ldproxy2Target.primaryKeyColumn;

		    refUriTemplate = urlTemplateForValueType(pi);

		    if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {

			// feature ref encoding
			if (pi.cardinality().maxOccurs == 1) {
			    idSourcePath = Optional.of(databaseColumnName(pi));
//	an actual value is not available in this case		   
			    // valueSourcePath = "[" + databaseColumnName(pi) + "=" + pkTargetTable + "]" +
			    // targetTable;
			} else {

			    String baseSourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList);

//				an actual value is not available in this case
//			    valueSourcePath = baseSourcePath + "/[" + targetTable
//				    + Ldproxy2Target.associativeTableColumnSuffix + "=" + pkTargetTable + "]"
//				    + targetTable;

			    idSourcePath = Optional.of(
				    baseSourcePath + "/" + targetTable + Ldproxy2Target.associativeTableColumnSuffix);
			}

			// TODO determine value type via new map entry characteristic
			// Right now, we always use Integer for types mapped as LINK
			idValueType = Optional.of(Type.INTEGER);

		    } else {

			// link object encoding

			if (pi.cardinality().maxOccurs == 1) {
			    // no source path to add on property level
			} else {

			    valueSourcePath = Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList) + "/[" + tableName
				    + Ldproxy2Target.associativeTableColumnSuffix + "="
				    + Ldproxy2Target.primaryKeyColumn + "]" + tableName);
			}
		    }

		} else {

		    // value type is a simple ldproxy type
		    if (pi.cardinality().maxOccurs == 1) {

			valueSourcePath = Optional.of(databaseColumnName(pi));

		    } else {

			String sortKeyAddition = "{sortKey=" + sqlProviderHelper.databaseTableName(pi.inClass(), true)
				+ "}";
			if (pi.matches(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD)) {
			    sortKeyAddition = "";
			}
			valueSourcePath = Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				+ associativeTableName(pi, alreadyVisitedPiList) + sortKeyAddition + "/"
				+ databaseColumnName(pi));
		    }
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(msgSource, 118, typeName);
		valueSourcePath = Optional.of("FIXME");
	    }

	} else {

	    // property type is NOT mapped

	    ClassInfo typeCi = pi.typeClass();

	    if (typeCi == null) {

		MessageContext mc = result.addError(msgSource, 118, typeName);
		if (mc != null) {
		    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		}
		valueSourcePath = Optional.of("FIXME");

	    } else {

		if (typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST) {

		    if (pi.cardinality().maxOccurs == 1) {

			/*
			 * Note: Addition of code list foreign key suffix is handled in method
			 * databaseColumnName(..).
			 */
			valueSourcePath = Optional.of(databaseColumnName(pi));

		    } else {

			String sortKeyAddition = "{sortKey=" + sqlProviderHelper.databaseTableName(pi.inClass(), true)
				+ "}";
			if (pi.matches(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD)) {
			    sortKeyAddition = "";
			}

			String path1 = "[" + primaryKeyColumn(pi.inClass()) + "="
				+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				+ associativeTableName(pi, alreadyVisitedPiList) + sortKeyAddition + "/";
			String path2;
			if (typeCi.category() == Options.CODELIST
				&& typeCi.matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {
			    path2 = sqlProviderHelper.databaseTableName(typeCi, true);
			} else {
			    path2 = databaseColumnName(pi);
			}

			String path = path1 + path2;
			valueSourcePath = Optional.of(path);
		    }

		} else if (typeCi.category() == Options.DATATYPE) {

		    if (typeCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES)) {

			targetTable = associativeTableName(pi, alreadyVisitedPiList);

			valueSourcePath = Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]" + targetTable);

		    } else {

			targetTable = sqlProviderHelper.databaseTableName(typeCi, false);

			if (pi.cardinality().maxOccurs == 1) {

			    valueSourcePath = Optional.of(
				    "[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]" + targetTable);
			} else {

			    valueSourcePath = Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList) + "/["
				    + sqlProviderHelper.databaseTableName(typeCi, true) + "=" + primaryKeyColumn(typeCi)
				    + "]" + targetTable);
			}
		    }

		} else {

		    // value type is type with identity
		    refType = LdpInfo.configIdentifierName(typeCi);
		    idValueType = Optional.of(determineIdMemberType(typeCi));
		    targetTable = sqlProviderHelper.databaseTableName(typeCi, false);

		    if (pi.reverseProperty() != null && pi.reverseProperty().isNavigable()) {

			// bi-directional association
			if (pi.cardinality().maxOccurs > 1 && pi.reverseProperty().cardinality().maxOccurs > 1) {

			    // n:m

			    if (LdpInfo.isReflexive(pi)) {

				String baseSourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
					+ sqlProviderHelper.databaseTableName(pi.inClass(), false) + "_"
					+ databaseColumnNameReflexiveProperty(pi.reverseProperty(), true) + "]"
					+ associativeTableName(pi, alreadyVisitedPiList) + "/";

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional
					    .of(baseSourcePath + sqlProviderHelper.databaseTableName(typeCi, false)
						    + "_" + databaseColumnNameReflexiveProperty(pi, true));
				}

				valueSourcePath = Optional
					.of(baseSourcePath + "[" + sqlProviderHelper.databaseTableName(typeCi, false)
						+ "_" + databaseColumnNameReflexiveProperty(pi, true) + "="
						+ primaryKeyColumn(pi.inClass()) + "]" + targetTable);

			    } else {

				String baseSourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
					+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
					+ associativeTableName(pi, alreadyVisitedPiList) + "/";

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional
					    .of(baseSourcePath + sqlProviderHelper.databaseTableName(typeCi, true));
				}
				valueSourcePath = Optional
					.of(baseSourcePath + "[" + sqlProviderHelper.databaseTableName(typeCi, true)
						+ "=" + primaryKeyColumn(typeCi) + "]" + targetTable);
			    }

			} else if (pi.cardinality().maxOccurs > 1) {

			    // n:1

			    if (LdpInfo.isReflexive(pi)) {

				// case p2 from ppt image (n:1 for bi-directional reflexive association)
				String sourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
					+ databaseColumnNameReflexiveProperty(pi.reverseProperty(), false) + "]"
					+ targetTable;

				valueSourcePath = Optional.of(sourcePath);

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional.of(sourcePath + "/" + primaryKeyColumn(pi.inClass()));
				}

			    } else {

				// case pB from ppt image (n:1 for bi-directional association)
				String sourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
					+ databaseColumnName(pi.reverseProperty()) + "]" + targetTable;

				valueSourcePath = Optional.of(sourcePath);

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional.of(sourcePath + "/" + primaryKeyColumn(typeCi));
				}
			    }

			} else if (pi.reverseProperty().cardinality().maxOccurs > 1) {

			    // n:1

			    if (LdpInfo.isReflexive(pi)) {

				// case p1 from ppt image (n:1 for bi-directional reflexive association)

				valueSourcePath = Optional.of("[" + databaseColumnNameReflexiveProperty(pi, false) + "="
					+ primaryKeyColumn(typeCi) + "]" + targetTable);

				if (isImplementedAsFeatureReference(pi)) {

				    // no need for a table join in this case
				    idSourcePath = Optional.of(databaseColumnNameReflexiveProperty(pi, false));

				} else {

				    if (!LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {
					// the source path on property level is omitted in this case
					// sourcePath = databaseColumnNameReflexiveProperty(pi, false);
					valueSourcePath = Optional.empty();
				    }
				}

			    } else {

				// case pA from ppt image (n:1 for bi-directional association)

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional.of(databaseColumnName(pi));
				}
				/*
				 * NOTE: will be omitted if tag ldpTitleAttribute on typeCi has a valid value
				 */
				valueSourcePath = Optional.of("[" + databaseColumnName(pi) + "="
					+ primaryKeyColumn(typeCi) + "]" + targetTable);
			    }

			} else {

			    // 1:1

			    if (LdpInfo.isReflexive(pi)) {

				valueSourcePath = Optional.of("[" + databaseColumnNameReflexiveProperty(pi, false) + "="
					+ primaryKeyColumn(typeCi) + "]" + targetTable);

				if (isImplementedAsFeatureReference(pi)) {

				    // no need for a table join in this case
				    idSourcePath = Optional.of(databaseColumnNameReflexiveProperty(pi, false));

				} else {

				    if (!LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {
					// the source path on property level is omitted in this case
					valueSourcePath = Optional.empty();
				    }
				}

			    } else {

				// max mult = 1 on both ends
				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional.of(databaseColumnName(pi));
				}

				/*
				 * NOTE: will be omitted if tag ldpTitleAttribute on typeCi has a valid value
				 */
				valueSourcePath = Optional.of("[" + databaseColumnName(pi) + "="
					+ primaryKeyColumn(typeCi) + "]" + targetTable);
			    }
			}

		    } else {

			// attribute or uni-directional association

			if (pi.cardinality().maxOccurs == 1) {

			    // n:1

			    if (LdpInfo.isReflexive(pi)) {

				valueSourcePath = Optional.of("[" + databaseColumnNameReflexiveProperty(pi, false) + "="
					+ primaryKeyColumn(typeCi) + "]" + targetTable);

				if (isImplementedAsFeatureReference(pi)) {
				    // no need for a table join in this case
				    idSourcePath = Optional.of(databaseColumnNameReflexiveProperty(pi, false));
				} else {

				    if (!LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {

					// the source path on property level is omitted in this case
					// sourcePath = databaseColumnNameReflexiveProperty(pi, false);
					valueSourcePath = Optional.empty();
				    }
				}

			    } else {

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional.of(databaseColumnName(pi));
				}

				/*
				 * NOTE: will be omitted if tag ldpTitleAttribute on typeCi has a valid value
				 */
				valueSourcePath = Optional.of("[" + databaseColumnName(pi) + "="
					+ primaryKeyColumn(typeCi) + "]" + targetTable);

			    }

			} else {

			    // n:m

			    if (LdpInfo.isReflexive(pi)) {

				String baseSourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
					+ sqlProviderHelper.databaseTableName(pi.inClass(), false) + "_"
					+ databaseColumnNameReflexiveProperty(pi.reverseProperty(), true) + "]"
					+ associativeTableName(pi, alreadyVisitedPiList) + "/";

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional
					    .of(baseSourcePath + sqlProviderHelper.databaseTableName(typeCi, false)
						    + "_" + databaseColumnNameReflexiveProperty(pi, true));
				}

				valueSourcePath = Optional
					.of(baseSourcePath + "[" + sqlProviderHelper.databaseTableName(typeCi, false)
						+ "_" + databaseColumnNameReflexiveProperty(pi, true) + "="
						+ primaryKeyColumn(pi.inClass()) + "]" + targetTable);

			    } else {

				String baseSourcePath = "[" + primaryKeyColumn(pi.inClass()) + "="
					+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
					+ associativeTableName(pi, alreadyVisitedPiList) + "/";

				if (isImplementedAsFeatureReference(pi)) {
				    idSourcePath = Optional
					    .of(baseSourcePath + sqlProviderHelper.databaseTableName(typeCi, true));
				}

				valueSourcePath = Optional
					.of(baseSourcePath + "[" + sqlProviderHelper.databaseTableName(typeCi, true)
						+ "=" + primaryKeyColumn(typeCi) + "]" + targetTable);
			    }
			}
		    }
		}
	    }
	}

	if (valueSourcePath.isPresent() || idSourcePath.isPresent()) {

	    LdpSqlSourcePathInfo spi = new LdpSqlSourcePathInfo(idSourcePath, valueSourcePath, idValueType, refType,
		    refUriTemplate, targetsSingleValue, targetTable);
	    spRes.addSourcePathInfo(spi);
	}

	return spRes;
    }

    private Type determineIdValueType(String idValueType) {
	return "string".equalsIgnoreCase(idValueType) ? Type.STRING : Type.INTEGER;
    }

    private Type determineIdMemberType(ClassInfo ci) {

	LdpSpecialPropertiesInfo specPropInfo = target.specialPropertiesInfo(ci);

	if (!specPropInfo.isMultipleIdentifierPisEncountered() && specPropInfo.getIdentifierPiOfCi() != null) {
	    return target.ldproxyType(specPropInfo.getIdentifierPiOfCi());
	}

	// default when creating new identifier members in type definitions is integer
	return Type.INTEGER;
    }

    private String databaseColumnName(PropertyInfo pi) {

	String suffix = "";

	Type t = target.ldproxyType(pi);

//	if (!(LdpUtil.isLdproxySimpleType(t) || LdpUtil.isLdproxyGeometryType(t))) {
//
//	    if (LdpInfo.valueTypeIsTypeWithIdentity(pi) || target.isMappedToLink(pi)) {
//		suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffix;
//	    } else if (pi.categoryOfValue() == Options.DATATYPE) {
//		suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffixDatatype;
//	    }
//
//	} else if (pi.categoryOfValue() == Options.CODELIST && Ldproxy2Target.model.classByIdOrName(pi.typeInfo())
//		.matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {
//
//	    // Support SqlDdl target parameter foreignKeyColumnSuffixCodelist
//	    suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffixCodelist;
//	}

	boolean isReflexiveProperty = pi.inClass().id().equals(pi.typeInfo().id);

	if (!(LdpUtil.isLdproxySimpleType(t) || LdpUtil.isLdproxyGeometryType(t))) {

	    if (target.isMappedToLink(pi)) {

		String typeName = pi.typeInfo().name;
		String piEncodingRule = pi.encodingRule("sql");

		String repCat = Ldproxy2Target.mapEntryParamInfos.getCharacteristic(typeName, piEncodingRule,
			Ldproxy2Constants.ME_PARAM_LINK_INFOS, Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_REP_CAT);

		if (repCat != null && repCat.equalsIgnoreCase("datatype")) {
		    suffix = Ldproxy2Target.foreignKeyColumnSuffixDatatype;
		} else if (repCat != null && repCat.equalsIgnoreCase("codelist")) {
		    suffix = Ldproxy2Target.foreignKeyColumnSuffixCodelist;
		} else {
		    suffix = (isReflexiveProperty && Ldproxy2Target.reflexiveRelationshipFieldSuffix != null)
			    ? Ldproxy2Target.reflexiveRelationshipFieldSuffix
			    : Ldproxy2Target.foreignKeyColumnSuffix;
		}
	    } else if (pi.categoryOfValue() == Options.DATATYPE) {
		suffix = Ldproxy2Target.foreignKeyColumnSuffixDatatype;
	    } else {
		suffix = (isReflexiveProperty && Ldproxy2Target.reflexiveRelationshipFieldSuffix != null)
			? Ldproxy2Target.reflexiveRelationshipFieldSuffix
			: Ldproxy2Target.foreignKeyColumnSuffix;
	    }

	} else if (pi.categoryOfValue() == Options.CODELIST && Ldproxy2Target.model.classByIdOrName(pi.typeInfo())
		.matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {
	    suffix = Ldproxy2Target.foreignKeyColumnSuffixCodelist;
	}

	return databaseColumnName(pi, suffix);
    }

    private String databaseColumnName(PropertyInfo pi, String suffix) {

	String result = pi.name();

	result = result + suffix;

	result = result.toLowerCase(Locale.ENGLISH);

	result = StringUtils.substring(result, 0, Ldproxy2Target.maxNameLength);

	return result;
    }

    public List<String> sourcePathsLinkLevelTitle(PropertyInfo pi) {

	List<String> result = new ArrayList<>();

	if (!target.isMappedToLink(pi) && LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {

	    PropertyInfo titleAtt = LdpInfo.getTitleAttribute(pi.typeClass());
	    if (titleAtt.cardinality().minOccurs == 0) {
		/*
		 * PK field shall be listed first, since the last listed sourcePaths "wins", and
		 * that should be the title attribute, if it exists
		 */
		result.add(primaryKeyColumn(pi));
	    }
	    result.add(databaseColumnName(titleAtt));

	} else {

	    if (pi.cardinality().maxOccurs == 1) {
		result.add(databaseColumnName(pi));
	    } else {
		result.add(primaryKeyColumn(pi));
	    }
	}

	return result;
    }

    private String databaseColumnNameReflexiveProperty(PropertyInfo pi, boolean inAssociativeTable) {

	String suffix = "";

	if (LdpInfo.isTypeWithIdentityValueType(pi)) {

	    if (inAssociativeTable) {
		suffix = Ldproxy2Target.associativeTableColumnSuffix;
	    } else {

		suffix = (Ldproxy2Target.reflexiveRelationshipFieldSuffix != null)
			? Ldproxy2Target.reflexiveRelationshipFieldSuffix
			: Ldproxy2Target.foreignKeyColumnSuffix;
	    }

	} else if (pi.categoryOfValue() == Options.DATATYPE) {
	    suffix = Ldproxy2Target.foreignKeyColumnSuffixDatatype;
	}

	return databaseColumnName(pi, suffix);
    }

    private String databaseTableNameForMappedValueType(PropertyInfo pi) {

	String tableName = null;

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {

	    tableName = Ldproxy2Target.mapEntryParamInfos.getCharacteristic(valueTypeName,
		    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_LINK_INFOS,
		    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_TABLE_NAME);
	}

	if (StringUtils.isBlank(tableName)) {
	    tableName = valueTypeName.toLowerCase(Locale.ENGLISH);
	}

	return tableName;
    }

    public String sourcePathLinkLevelHref(PropertyInfo pi) {

	if (pi.cardinality().maxOccurs == 1) {

	    if (!target.isMappedToLink(pi) && LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {
		return primaryKeyColumn(pi);
	    } else {
		return databaseColumnName(pi);
	    }
	} else {
	    return primaryKeyColumn(pi);
	}
    }

    private String primaryKeyColumn(PropertyInfo pi) {

	/*
	 * TODO - Inspect map entries first? Do we need to support defining the primary
	 * key columns in map entries?
	 */

	ClassInfo typeCi = pi.typeClass();
	if (typeCi == null) {
	    return Ldproxy2Target.primaryKeyColumn;
	} else {
	    return primaryKeyColumn(typeCi);
	}
    }

    /**
     * Look for SqlPropertyEncodingInfos that have same sourceTable as defined by
     * the context, and that have originalPropertyName (if not set, then
     * propertyName) equal to that of pi, and originalInClassName (if not set, then
     * inClassName) equal to that of pi, and originalSchemaName (if not set, then
     * schemaName) equal to that of pi.
     * 
     * @param pi      - tbd
     * @param context - tbd
     * @return - tbd
     */
    protected SortedSet<SqlPropertyEncodingInfo> getPropertyEncodingInfos(PropertyInfo pi,
	    LdpSqlPropertyEncodingContext context) {

	SortedSet<SqlPropertyEncodingInfo> result = new TreeSet<>();

	for (SqlPropertyEncodingInfo sei : this.encodingInfos.getSqlPropertyEncodingInfos()) {

	    String sourceTable = sei.getSourceTable();

	    if (sourceTable.equals(context.getSourceTable())) {

		String name = sei.hasOriginalPropertyName() ? sei.getOriginalPropertyName() : sei.getPropertyName();
		String schema = sei.hasOriginalSchemaName() ? sei.getOriginalSchemaName() : sei.getSchemaName();
		String inClass = sei.hasOriginalInClassName() ? sei.getOriginalInClassName() : sei.getInClassName();

		String piSchema = pi.model().schemaPackage(pi.inClass()).name();

		if (pi.name().equals(name) && piSchema.equals(schema) && pi.inClass().name().equals(inClass)) {
		    result.add(sei);
		}
	    }
	}

	return result;
    }

    private String primaryKeyColumn(ClassInfo ci) {

//	if (ci.matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {
//	    for (PropertyInfo pi : ci.properties().values()) {
//		if (pi.stereotype("identifier")) {
//		    return databaseColumnName(pi);
//		}
//	    }
//	}
	LdpSpecialPropertiesInfo specPropInfo = target.specialPropertiesInfo(ci);
	if (!specPropInfo.isMultipleIdentifierPisEncountered() && specPropInfo.getIdentifierPiOfCi() != null) {
	    return databaseColumnName(specPropInfo.getIdentifierPiOfCi());
	} else {
	    return Ldproxy2Target.primaryKeyColumn;
	}
    }

    private String associativeTableName(PropertyInfo pi, List<PropertyInfo> alreadyVisitedPiList) {

	String result = null;

	/*
	 * Check case of usage specific data type table first. We need to create table
	 * names as are created by the SqlDdl target for
	 * rule-sql-cls-data-types-oneToMany-severalTables. That is the case if the
	 * owner of pi is a data type that matches that rule.
	 */
	if (isEncodedInUsageSpecificDataTypeTable(pi)) {

	    /*
	     * We need to follow the list of already visited properties from the end along
	     * all properties owned by complex data types in order to construct the table
	     * name.
	     */
	    String suffix = "_" + pi.name();
	    String tableName = null;

	    for (int i = alreadyVisitedPiList.size() - 1; i >= 0; i--) {

		PropertyInfo previousPi = alreadyVisitedPiList.get(i);
		ClassInfo prevPiOwnerCi = previousPi.inClass();

		/*
		 * We also gather the name of the first property (looked at from the end of the
		 * list of already visited properties) which is not owned by a complex data
		 * type. That is why the suffix modification is not part of the following
		 * if-else-test.
		 */
		suffix = "_" + previousPi.name() + suffix;

		if (prevPiOwnerCi != null && prevPiOwnerCi.category() == Options.DATATYPE
			&& Ldproxy2Target.model.isInSelectedSchemas(prevPiOwnerCi)
			&& target.mapEntry(prevPiOwnerCi).isEmpty()
			&& prevPiOwnerCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES)) {
		    /*
		     * As long as the owner of the currently visited property is a complex data type
		     * that matches the criteria for creation of several tables, we continue
		     * iterating through the list of previous properties.
		     */
		} else {
		    tableName = previousPi.inClass().name();
		    break;
		}
	    }

	    result = tableName + suffix;

	} else {

	    if (StringUtils.isNotBlank(pi.taggedValue("associativeTable"))) {
		result = pi.taggedValue("associativeTable");
	    } else if (pi.association() != null
		    && StringUtils.isNotBlank(pi.association().taggedValue("associativeTable"))) {
		result = pi.association().taggedValue("associativeTable");
	    } else {

		// tag associativeTable not set or without value -> proceed

		String tableNamePi = determineTableName(pi);

		if (target.isMappedToLink(pi) || pi.isAttribute() || pi.reverseProperty() == null
			|| !pi.reverseProperty().isNavigable()) {

		    result = tableNamePi;

		} else {

		    // both pi and its reverseProperty are navigable

		    // choose name based on alphabetical order
		    // take into account the case of a reflexive association
		    String tableNameRevPi = determineTableName(pi.reverseProperty());

		    if (tableNamePi.compareTo(tableNameRevPi) <= 0) {
			result = tableNamePi;
		    } else {
			result = tableNameRevPi;
		    }
		}
	    }
	}

	result = StringUtils.substring(result, 0, Ldproxy2Target.maxNameLength);
	return result;
    }

    private boolean isEncodedInUsageSpecificDataTypeTable(PropertyInfo pi) {

	ClassInfo piOwnerCi = pi.inClass();
	return (piOwnerCi != null && piOwnerCi.category() == Options.DATATYPE
		&& Ldproxy2Target.model.isInSelectedSchemas(piOwnerCi) && target.mapEntry(piOwnerCi).isEmpty()
		&& piOwnerCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES));
    }

    private String determineTableName(PropertyInfo pi) {

	String tableName = pi.inClass().name();
	String propertyName = pi.name();
	String res = tableName + "_" + propertyName;
	return res;
    }

    public String urlTemplateForValueType(PropertyInfo pi) {

	String urlTemplate = null;

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {

	    urlTemplate = Ldproxy2Target.mapEntryParamInfos.getCharacteristic(valueTypeName,
		    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_LINK_INFOS,
		    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE);

	    if (urlTemplate != null) {
		urlTemplate = urlTemplate.replaceAll("\\(value\\)", "{{value}}").replaceAll("\\(serviceUrl\\)",
			"{{serviceUrl}}");
	    }
	}

	if (StringUtils.isBlank(urlTemplate)) {
	    urlTemplate = "{{serviceUrl}}/collections/" + valueTypeName.toLowerCase(Locale.ENGLISH)
		    + "/items/{{value}}";
	}

	return urlTemplate;
    }

    @Override
    public String sourcePathTypeLevel(ClassInfo ci) {
	return "/" + sqlProviderHelper.databaseTableName(ci, false);
    }

    @Override
    public String defaultPrimaryKey() {
	return Ldproxy2Target.primaryKeyColumn;
    }

    @Override
    public String defaultSortKey() {
	return Ldproxy2Target.primaryKeyColumn;
    }

    @Override
    public boolean isEncodedWithDirectValueSourcePath(PropertyInfo pi, LdpPropertyEncodingContext context) {

	if (pi.cardinality().maxOccurs == 1 && !this.encodingInfos.isEmpty()) {
	    /*
	     * For single valued properties, check if there is a deviation in the property
	     * name, i.e., the name of pi is not equal to the actual property name defined
	     * in (one of) the encoding info(s). In such a case, we need to encode the
	     * source path within the type definition and not in the fragment, using the
	     * source path from the encoding infos. The reason is that otherwise, the name
	     * of pi, which is used as source path (retrieved via method
	     * databaseColumnName(..); potentially also adding a suffix), would not be the
	     * actual column name in the database schema.
	     * 
	     * An example, where this was relevant, was conversion of type CI_Date, with
	     * property 'date', and multiplicity 1. In SQL, 'date' is a reserved keyword.
	     * Thus, a suffix was appended to the column name in the database schema. Using
	     * 'date' as sourcePath for property 'date' in the ldproxy provider
	     * configuration, more specifically the fragment definition of CI_Date, would
	     * not be correct. The situation is taken into account with this check.
	     * 
	     * NOTE: In the future, we could also try to check the actual value source path
	     * given in the property encoding info. The databaseColumnName(..) methods take
	     * into account certain suffixes, which complicates the matter.
	     */
	    SortedSet<SqlPropertyEncodingInfo> speis = getPropertyEncodingInfos(pi,
		    (LdpSqlPropertyEncodingContext) context);

	    for (SqlPropertyEncodingInfo spei : speis) {

		/*
		 * If the encoding info has an original property name, it is equal to that of
		 * pi.
		 */
		if (!spei.getPropertyName().equals(pi.name())) {
		    return false;
		}
	    }
	}

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1
		    return true;

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    return pi.cardinality().maxOccurs == 1;

		} else {

		    // value type is a simple ldproxy type
		    return pi.cardinality().maxOccurs == 1;
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(msgSource, 118, typeName);
		return true;
	    }
	}

	ClassInfo typeCi = pi.typeClass();

	if (typeCi == null) {

	    MessageContext mc = result.addError(msgSource, 118, typeName);
	    if (mc != null) {
		mc.addDetail(msgSource, 1, pi.fullNameInSchema());
	    }
	    return true;

	} else {

	    if (pi.cardinality().maxOccurs == 1
		    && (pi.categoryOfValue() == Options.ENUMERATION || pi.categoryOfValue() == Options.CODELIST)) {
		return true;
	    } else {
		return false;
	    }
	}
    }

    @Override
    public boolean multipleSourcePathsUnsupportedinFragments() {
	return true;
    }

    @Override
    public String sourcePathFeatureRefId(PropertyInfo pi) {
	return primaryKeyColumn(pi);
    }

}
