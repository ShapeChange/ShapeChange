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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

public abstract class InfoImpl implements Info, Comparable<InfoImpl>, MessageSource {

	boolean postprocessed = false;
	private String lf = System.getProperty("line.separator");

	protected String documentation = null;
	protected String aliasName = null;
	protected String definition = null;
	protected String description = null;
	protected String legalBasis = null;
	protected String primaryCode = null;
	protected String language = null;
	protected String[] examples = null;
	protected String[] dataCaptureStatements = null;
	protected TaggedValues taggedValuesCache = null;
	protected Stereotypes stereotypesCache = null;
	private static final Pattern langPattern = Pattern.compile("^\"(.*)\"@([a-zA-Z0-9\\-]{2,})$");

	public int compareTo(InfoImpl i) {
		String my = id();
		String other = i.id();
		return my.hashCode() - other.hashCode();
	}

	public Stereotypes stereotypes() {
		validateStereotypesCache();
		// Return copy of cache
		return options().stereotypesFactory(stereotypesCache);
	} // stereotypes()

	public boolean stereotype(String stereotype) {
		Stereotypes stereotypes = stereotypes();
		String st;
		if (stereotype != null)
			st = model().options().normalizeStereotype(stereotype);
		else
			st = "";
		for (String s : stereotypes.asArray()) {
			String sn = model().options().normalizeStereotype(s);
			if (sn.equalsIgnoreCase(st))
				return true;
		}
		if (st.isEmpty() && stereotypes.isEmpty())
			return true;

		return false;
	}

	public TaggedValues taggedValuesForTagList(String tagList) {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		// create clone
		TaggedValues copy = options().taggedValueFactory(taggedValuesCache, tagList);
		return copy;
	}

	public TaggedValues taggedValuesAll() {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		// create clone
		TaggedValues copy = options().taggedValueFactory(taggedValuesCache);
		return copy;
	}

	public String taggedValue(String tag) {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		String[] values = taggedValuesCache.get(tag);
		if (values == null || values.length == 0)
			return null;
		else if (values.length > 1)
			for (int i = 1; i < values.length; i++) {
				MessageContext mc = model().result().addWarning(this, 201, tag, values[0], values[i]);
				addContextDetails(mc);
			}

		return options().internalize(values[0]);
	} // taggedValue()

	private void addContextDetails(MessageContext mc) {

		/*
		 * we want to provide as much information as possible to locate the
		 * element in the model
		 */
		if (this instanceof PropertyInfo) {

			PropertyInfo pi = (PropertyInfo) this;

			mc.addDetail(this, 1, pi.name(), pi.inClass().name());

		} else {
			mc.addDetail(this, 0, this.toString(), this.name());
		}
	}

	public String[] taggedValuesForTag(String tag) {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		return taggedValuesCache.get(tag);
	} // taggedValuesAll()

	public String taggedValueInLanguage(String tag, String language) {
		validateTaggedValuesCache();

		String[] values = taggedValuesCache.get(tag);
		if (values == null || values.length == 0)
			return null;

		String result = null;
		for (String value : values) {
			if (value == null || value.length() == 0)
				continue;
			Matcher m = langPattern.matcher(value);
			if (m.matches()) {
				String lang = m.group(2);
				if (!lang.equalsIgnoreCase(language))
					continue;
				value = m.group(1);
			}
			if (result == null)
				result = value;
			else {
				MessageContext mc = model().result().addWarning(this, 202, tag, language, result, value);
				addContextDetails(mc);
			}
		}

		return options().internalize(result);
	}

	public String[] taggedValuesInLanguage(String tag, String language) {
		validateTaggedValuesCache();

		String[] values = taggedValuesCache.get(tag);
		if (values == null || values.length == 0)
			return new String[0];

		List<String> result = new ArrayList<String>();
		for (String value : values) {
			if (value == null || value.length() == 0)
				continue;
			Matcher m = langPattern.matcher(value);
			if (m.matches()) {
				String text = m.group(1);
				String lang = m.group(2);
				if (lang.equalsIgnoreCase(language))
					result.add(options().internalize(text));
			} else {
				result.add(options().internalize(value));
			}
		}
		return result.toArray(new String[result.size()]);
	}

	protected String descriptorSource(String descriptor) {
		String source = null;

		if (model().type() == Options.GENERIC) {
			/*
			 * special treatment for generic models, which always store the
			 * descriptor values directly
			 */
			source = "sc:internal";
		} else {
			source = options().descriptorSource(descriptor);

			// if nothing has been configured, use defaults
			if (source == null) {
				if (model().type() == Options.EA7) {
					if (descriptor.equalsIgnoreCase(Options.Descriptor.DOCUMENTATION.toString()))
						source = "ea:notes";
					else if (descriptor.equalsIgnoreCase(Options.Descriptor.ALIAS.toString()))
						source = "ea:alias";
					else if (descriptor.equalsIgnoreCase(Options.Descriptor.DEFINITION.toString()))
						source = "sc:extract#PROLOG";
					else if (descriptor.equalsIgnoreCase(Options.Descriptor.DESCRIPTION.toString()))
						source = "none";
					else
						source = "tag#" + descriptor;
				} else if (model().type() == Options.XMI10 || model().type() == Options.GSIP) {
					if (descriptor.equalsIgnoreCase(Options.Descriptor.DOCUMENTATION.toString()))
						source = "tag#documentation;description";
					else if (descriptor.equalsIgnoreCase(Options.Descriptor.ALIAS.toString()))
						source = "tag#alias";
					else if (descriptor.equalsIgnoreCase(Options.Descriptor.DEFINITION.toString()))
						source = "sc:extract#PROLOG";
					else if (descriptor.equalsIgnoreCase(Options.Descriptor.DESCRIPTION.toString()))
						source = "none";
					else
						source = "tag#" + descriptor;
				} else {
					source = "tag#" + descriptor;
				}
			}
		}

		return source;
	}

	private String descriptorValue(String descriptor, boolean language) {
		String value = null;
		String source = descriptorSource(descriptor);
		if (source.startsWith("tag#")) {
			String[] tags = source.replace("tag#", "").split(";");
			for (String tag : tags) {
				if (language)
					value = taggedValueInLanguage(tag, options().language());
				else
					value = taggedValue(tag);
				if (value != null && !value.isEmpty())
					break;
			}
		} else if (source.equals("ea:alias") && model().type() == Options.EA7) {
			// do nothing now, this happens in the EA classes
		} else if (source.equals("ea:notes") && model().type() == Options.EA7) {
			// do nothing now, this happens in the EA classes
		} else if (source.startsWith("sc:extract#")) {
			String token = source.replace("sc:extract#", "");
			String doc = documentation();

			if (doc == null || doc.trim().length() == 0) {
				// nothing to extract from ...
			} else {
				String[] ss = doc.split(options().extractSeparator());
				boolean found = false;
				if (token.equals("PROLOG"))
					// PROLOG is the start of the documentation before the first
					// separator
					found = true;
				for (String s : ss) {
					if (found) {
						value = s.trim();
						break;
					} else if (s.trim().equalsIgnoreCase(token)) {
						found = true;
					}
				}
			}
		}

		/*
		 * For backwards compatibility, the default differs by descriptor
		 */
		if (value == null)
			if (descriptor.equals(Options.Descriptor.DOCUMENTATION.toString())
					|| descriptor.equals(Options.Descriptor.DEFINITION.toString()))
				value = "";

		return value;
	}

	private String[] descriptorValues(String descriptor, boolean language) {
		String[] values = new String[0];
		String source = descriptorSource(descriptor);
		if (source.startsWith("tag#")) {
			String[] tags = source.replace("tag#", "").split(";");
			for (String tag : tags) {
				if (language)
					values = taggedValuesInLanguage(tag, options().language());
				else
					values = taggedValuesForTag(tag);
				if (values != null && values.length > 0)
					break;
			}
		} else if (source.equals("ea:alias") && model().type() == Options.EA7) {
			// do nothing now, this happens in the EA classes
		} else if (source.equals("ea:notes") && model().type() == Options.EA7) {
			// do nothing now, this happens in the EA classes
		} else if (source.startsWith("sc:extract#")) {
			String token = source.replace("sc:extract#", "");
			String doc = documentation();

			if (doc == null || doc.trim().length() == 0) {
				// nothing to extract from ...
			} else {
				String[] ss = doc.split(options().extractSeparator());
				boolean found = false;
				if (token.equals("PROLOG"))
					// PROLOG is the start of the documentation before the first
					// separator
					found = true;
				for (String s : ss) {
					if (found) {
						values = new String[] { s.trim() };
						break;
					} else if (s.trim().equalsIgnoreCase(token)) {
						found = true;
					}
				}
			}
		}

		return values;
	}

	public String primaryCode() {
		if (primaryCode == null)
			primaryCode = options().internalize(descriptorValue(Options.Descriptor.PRIMARYCODE.toString(), true));
		return primaryCode;
	}

	public String derivedDocumentation(String template, String novalue) {
		String tmp = (template == null ? Options.DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE : template);
		String nov = (novalue == null ? Options.DERIVED_DOCUMENTATION_DEFAULT_NOVALUE : novalue);
		String doc = tmp;

		Pattern pattern = Pattern.compile("\\[\\[(.+?)\\]\\]");
		Matcher matcher = pattern.matcher(tmp);

		// populate the replacements map ...
		HashMap<String, String> replacements = new HashMap<String, String>();

		String s = this.definition();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("definition", s.trim());

		s = this.description();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("description", s.trim());

		s = this.aliasName();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("alias", s.trim());

		s = this.primaryCode();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("primaryCode", s.trim());

		s = this.legalBasis();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("legalBasis", s.trim());

		s = this.language();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("language", s.trim());

		String[] sa = this.examples();
		if (sa != null && sa.length > 0) {
			s = "";
			for (String e : sa)
				s += e.trim() + lf + lf;
			if (s.trim().isEmpty())
				s = nov;
		} else
			s = nov;
		replacements.put("examples", s.trim());

		sa = this.dataCaptureStatements();
		if (sa != null && sa.length > 0) {
			s = "";
			for (String e : sa)
				s += e.trim() + lf + lf;
			if (s.trim().isEmpty())
				s = nov;
		} else
			s = nov;
		replacements.put("dataCaptureStatements", s.trim());

		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			String replacement = replacements.get(matcher.group(1));
			builder.append(doc.substring(i, matcher.start()));
			if (replacement == null)
				builder.append(matcher.group(0));
			else
				builder.append(replacement);
			i = matcher.end();
		}
		builder.append(doc.substring(i, doc.length()));
		return builder.toString();
	}

	/**
	 * @return an empty string if no documentation exists
	 */
	public String documentation() {
		if (documentation == null)
			documentation = options().internalize(descriptorValue(Options.Descriptor.DOCUMENTATION.toString(), true));
		return documentation;
	}

	/**
	 * Retrieve the part of a documentation of an information item that is
	 * considered a definition
	 * 
	 * @return the definition or an empty string if none exists
	 */
	public String definition() {
		if (definition == null)
			definition = options().internalize(descriptorValue(Options.Descriptor.DEFINITION.toString(), true));
		return definition;
	}

	/**
	 * Retrieve the part of a documentation of an information item that is
	 * considered an informative description
	 * 
	 * @return the description or null if none exists
	 */
	public String description() {
		if (description == null)
			description = options().internalize(descriptorValue(Options.Descriptor.DESCRIPTION.toString(), true));
		return description;
	}

	public String legalBasis() {
		if (legalBasis == null)
			legalBasis = options().internalize(descriptorValue(Options.Descriptor.LEGALBASIS.toString(), true));
		return legalBasis;
	}

	public String language() {
		if (language == null)
			language = options().internalize(descriptorValue(Options.Descriptor.LANGUAGE.toString(), true));
		return language;
	}

	public String[] dataCaptureStatements() {
		if (dataCaptureStatements == null)
			dataCaptureStatements = options()
					.internalize(descriptorValues(Options.Descriptor.DATACAPTURESTATEMENT.toString(), true));
		return dataCaptureStatements;
	}

	public String[] examples() {
		if (examples == null)
			examples = options().internalize(descriptorValues(Options.Descriptor.EXAMPLE.toString(), true));
		return examples;
	}

	public String aliasName() {
		// it is important not to set any default alias as this blocks other
		// aliases, e.g. from EA
		if (aliasName == null)
			aliasName = options().internalize(descriptorValue(Options.Descriptor.ALIAS.toString(), true));
		return aliasName;
	}

	public String encodingRule(String platform) {
		String s = taggedValue(platform + "EncodingRule");
		if (s == null || s.isEmpty() || options().ignoreEncodingRuleTaggedValues()) {
			// JE TBD: create enumeration with valid platforms for global use
			if (platform.equalsIgnoreCase("xsd")) {
				s = options().parameter(Options.TargetXmlSchemaClass, "defaultEncodingRule");
				if (s == null)
					s = Options.ISO19136_2007;
			} else if (platform.equalsIgnoreCase("json")) {
				s = options().parameter(Options.TargetJsonSchemaClass, "defaultEncodingRule");
				if (s == null)
					s = "geoservices";
			} else if (platform.equalsIgnoreCase("rdf")) {
				s = options().parameter(Options.TargetRDFClass, "defaultEncodingRule");
				if (s == null)
					s = "*";
			} else if (platform.equalsIgnoreCase("fc")) {
				s = options().parameter(Options.TargetFeatureCatalogueClass, "defaultEncodingRule");
				if (s == null)
					s = "*";
			} else if (platform.equalsIgnoreCase("sql")) {
				s = options().parameter(Options.TargetSQLClass, "defaultEncodingRule");
				if (s == null)
					s = "*";
			} else if (platform.equalsIgnoreCase("owl")) {
				s = options().parameter(Options.TargetOWLISO19150Class, "defaultEncodingRule");
				if (s == null)
					s = "*";
			} else if (platform.equalsIgnoreCase("arcgis")) {
				s = options().parameter(Options.TargetArcGISWorkspaceClass, "defaultEncodingRule");
				if (s == null)
					s = "*";
			} else if (platform.equalsIgnoreCase("sch")) {
				s = options().parameter(Options.TargetFOL2SchematronClass, "defaultEncodingRule");
				if (s == null)
					s = "*";
			} else if (platform.equalsIgnoreCase("rep")) {
				s = options().parameter(Options.TargetReplicationSchemaClass, "defaultEncodingRule");
				if (s == null)
					s = "*";
			}
		}
		if (s != null)
			s = s.toLowerCase();
		return s;
	} // encodingRule()

	public boolean matches(String rule) {
		String encRule = null;
		String[] ra = rule.toLowerCase().split("-", 4);
		/*
		 * test if the rule has the correct format
		 */
		if (ra.length != 4) {
			result().addError(null, 21, rule);
			return false;
		}
		/*
		 * test if the rule is known, if not it cannot match
		 */
		if (!options().hasRule(rule)) {
			result().addError(null, 164, rule);
			return false;
		}

		/*
		 * If the test is target-specific match only if the target is active,
		 * i.e. is 'enabled' for rules and not 'disabled' for requirements and
		 * recommendations.
		 */
		if (ra[0].equals("rule") && !ra[1].equals("all")) {
			if (!options().targetMode(options().targetClassName(rule)).equals("enabled"))
				return false;
		}

		if (ra[0].matches("re[cq]") && !ra[1].equals("all")) {
			if (options().targetMode(options().targetClassName(rule)).equals("disabled"))
				return false;
		}

		if (ra[2].equals("all")) {
			// nothing to test, applies to all elements
		} else if (ra[2].equals("pkg")) {
			if (!(this instanceof PackageInfo))
				return false;
		} else if (ra[2].equals("cls")) {
			if (!(this instanceof ClassInfo))
				return false;
		} else if (ra[2].equals("prop")) {
			if (!(this instanceof PropertyInfo))
				return false;
		} else if (ra[2].equals("rel")) {
			if (!(this instanceof AssociationInfo))
				return false;
		} else if (ra[2].equals("op")) {
			if (!(this instanceof OperationInfo))
				return false;
		} else {
			result().addError(null, 21, rule);
			return false;
		}

		/*
		 * support
		 */
		String param;
		if (rule.equals("rule-xsd-cls-local-enumeration")) {
			param = options().parameter(Options.TargetXmlSchemaClass, "enumStyle");
			if (param != null && param.equalsIgnoreCase("local"))
				return true;
		}
		if (rule.equals("rule-xsd-cls-local-basictype")) {
			param = options().parameter(Options.TargetXmlSchemaClass, "basicTypeStyle");
			if (param != null && param.equalsIgnoreCase("local"))
				return true;
		}
		if (rule.equals("rule-xsd-pkg-schematron")) {
			param = options().parameter(Options.TargetXmlSchemaClass, "schematron");
			if (param != null && param.equalsIgnoreCase("true"))
				return true;
		}
		if (rule.equals("rule-xsd-prop-exclude-derived")) {
			param = options().parameter(Options.TargetXmlSchemaClass, "includeDerivedProperties");
			if (param != null && param.equalsIgnoreCase("false"))
				return true;
		}

		/*
		 * check if the rule has been configured for the encoding rule that
		 * applies to the element
		 */
		if (ra[1].equals("all")) {
			boolean res = false;

			encRule = encodingRule("xsd");
			if (encRule != null)
				res = res || options().hasRule(rule, encRule);

			encRule = encodingRule("json");
			if (encRule != null)
				res = res || options().hasRule(rule, encRule);

			encRule = encodingRule("rdf");
			if (encRule != null)
				res = res || options().hasRule(rule, encRule);

			encRule = encodingRule("fc");
			if (encRule != null)
				res = res || options().hasRule(rule, encRule);

			encRule = encodingRule("sch");
			if (encRule != null)
				res = res || options().hasRule(rule, encRule);

			return res;

		} else {

			// determine if the applicable encoding rule contains the given rule
			encRule = encodingRule(ra[1]).toLowerCase();

			if (encRule != null) {

				boolean encRuleHasRule = options().hasRule(rule, encRule);

				if (encRuleHasRule) {

					// now cover cases in which a rule may be overwritten by a
					// parameter
					if (rule.equals("rule-xsd-all-no-documentation")) {

						// if the XML Schema target parameter
						// 'includeDocumentation' is set to true the rule is
						// overwritten
						param = options().parameter(Options.TargetXmlSchemaClass, "includeDocumentation");
						if (param != null && param.equalsIgnoreCase("true"))
							return false;
						else
							return true;
					}

					// no special case applies:
					return true;

				} else {

					// if the encoding rule does not contain the given rule then
					// there is no match
					return false;
				}
			}
			// TODO do something else if we do not find an encoding rule?
		}

		return false;
	}

	/**
	 * 1. Postprocess the model element to execute any actions that require that
	 * the complete model has been loaded.
	 * 
	 * 2. Validate the model element against all applicable requirements and
	 * recommendations. All rules applicable to all model elements are validated
	 * here, the more specific rules are all validated in the subclasses.
	 */
	public void postprocessAfterLoadingAndValidate() {
		// We don't look at postprocessed, this will be handled in the
		// subclasses.
	}

	/**
	 * @deprecated With UML 2, there may be multiple values per tag. Use
	 *             <code>taggedValuesAll(String tagOrTaglist)</code> instead.
	 */
	@Deprecated
	public Map<String, String> taggedValues(String tagList) {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		return taggedValuesCache.getFirstValues(tagList);
	} // taggedValues()

	/**
	 * @deprecated With UML 2, there may be multiple values per tag. Use
	 *             <code>taggedValuesAll()</code> instead.
	 */
	@Deprecated
	public Map<String, String> taggedValues() {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		return taggedValuesCache.getFirstValues();
	} // taggedValues()

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {

		case 0:
			return "Context: class InfoImpl. Element: $1$. Name: $2$";
		case 1:
			return "Context: class InfoImpl (subtype: PropertyInfo). Name: $1$. In class: $2$";

		case 201:
			return "A single value was requested for tag '$1$', but in addition to returned value '$2$', an additional value '$3$' exists and is ignored.";
		case 202:
			return "A single value was requested for tag '$1$' in language '$2$', but in addition to returned value '$3$', an additional value '$4$' exists and is ignored.";
		case 203:
			return "Multiple values were requested for descriptor '$1$', but the source '$2$' specified in the configuration only supports single values. No values have been returned.";

		default:
			return "(Unknown message)";
		}
	}
}
