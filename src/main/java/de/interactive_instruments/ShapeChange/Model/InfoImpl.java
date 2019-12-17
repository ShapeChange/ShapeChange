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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMode;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetRegistry;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 *
 */
public abstract class InfoImpl implements Info {

	boolean postprocessed = false;
	private String lf = System.getProperty("line.separator");

	protected Descriptors descriptors = null;
	protected TaggedValues taggedValuesCache = null;
	protected Stereotypes stereotypesCache = null;

	private static final Pattern langPattern = Pattern
			.compile("^\"(.*)\"@([a-zA-Z0-9\\-]{2,})$");

	public int compareTo(Info i) {

		String my = id();
		String other = i.id();

		return my.compareTo(other);

		/*
		 * 2016-08-11 JE - WARNING: Comparison using hashCodes of ids led to
		 * wrong results when Info objects were used in SortedMaps - the
		 * particular situation was a SortedMap with PropertyInfo objects (from
		 * a very large application schema, the NAS) as keys.
		 */
		// return my.hashCode() - other.hashCode();
	}

	@Override
	public Stereotypes stereotypes() {

		validateStereotypesCache();

		// Return copy of cache
		return options().stereotypesFactory(stereotypesCache);
	}

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
		TaggedValues copy = options().taggedValueFactory(taggedValuesCache,
				tagList);
		return copy;
	}

	public TaggedValues taggedValuesAll() {

		// Validate tagged values cache first
		validateTaggedValuesCache();

		// create clone
		TaggedValues copy = options().taggedValueFactory(taggedValuesCache);
		return copy;
	}

	@Override
	public String taggedValue(String tag) {

		// Validate tagged values cache first
		validateTaggedValuesCache();

		String[] values = taggedValuesCache.get(tag);
		if (values.length == 0)
			return null;
		else if (values.length > 1)
			for (int i = 1; i < values.length; i++) {
				MessageContext mc = model().result().addWarning(null, 701, tag,
						values[0], values[i]);
				addContextDetails(mc);
			}

		return options().internalize(values[0]);
	}

	private void addContextDetails(MessageContext mc) {

		if (mc != null) {
			/*
			 * we want to provide as much information as possible to locate the
			 * element in the model
			 */
			if (this instanceof PropertyInfo) {

				PropertyInfo pi = (PropertyInfo) this;

				mc.addDetail(null, 791, pi.name(), pi.inClass().name());

			} else {
				mc.addDetail(null, 790, this.toString(), this.name());
			}
		}
	}

	@Override
	public String[] taggedValuesForTag(String tag) {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		String[] result = taggedValuesCache.get(tag);
		if (result.length != 0) {
			// we sort since order is important for UnitTests
			Arrays.sort(result);
		}
		return result;
	}

	@Override
	public String taggedValueInLanguage(String tag, String language) {

		List<LangString> values = taggedValuesForTagAsLangStrings(tag);
		if (values.isEmpty()) {
			return null;
		}

		String result = null;
		for (LangString ls : values) {

			if (ls.hasLang() && ls.getLang().equalsIgnoreCase(language)) {

				if (result == null) {
					result = options().internalize(ls.getValue());
				} else {
					MessageContext mc = model().result().addWarning(null, 702,
							tag, language, result, ls.toString());
					addContextDetails(mc);
				}
			}
		}

		return result;
	}

	@Override
	public String[] taggedValuesInLanguage(String tag, String language) {
		validateTaggedValuesCache();

		List<LangString> values = taggedValuesForTagAsLangStrings(tag);
		if (values.isEmpty()) {
			return new String[0];
		}

		List<String> result = new ArrayList<String>();
		for (LangString ls : values) {

			if (ls.hasLang() && ls.getLang().equalsIgnoreCase(language)) {
				result.add(options().internalize(ls.getValue()));
			} else {
				result.add(options().internalize(ls.getValue()));
			}
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public List<LangString> taggedValuesForTagAsLangStrings(String tag) {

		String[] values = taggedValuesForTag(tag);

		List<LangString> result = new ArrayList<LangString>();

		for (String value : values) {

			if (value == null || value.length() == 0)
				continue;

			Matcher m = langPattern.matcher(value);

			if (m.matches()) {
				String text = m.group(1);
				String lang = m.group(2);
				result.add(new LangString(options().internalize(text),
						options().internalize(lang)));
			} else {
				result.add(new LangString(options().internalize(value)));
			}
		}

		// return new Descriptors(tmp);
		return result;
	}

	public Descriptors descriptors() {

		validateDescriptorsCache();

		return this.descriptors;
	}

	/**
	 * @param descriptors
	 *                        the new Descriptors to set; can be
	 *                        <code>null</code> in order to load them (when
	 *                        accessed) from configured descriptor sources
	 */
	public void setDescriptors(Descriptors descriptors) {

		this.descriptors = descriptors;
	}

	/**
	 * Look up the values for the descriptor, using the source as defined by the
	 * configuration (or the default source, if the configuration does not state
	 * anything regarding the source). If the source is a tagged value then the
	 * values in all available languages will be returned. For backwards
	 * compatibility reasons, the empty string will be returned for the
	 * descriptors DOCUMENTATION and DEFINITION if no values were found.
	 * 
	 * @param descriptor
	 * @return values for the descriptor, can be empty but not null;
	 */
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		validateDescriptorsCache();

		/*
		 * Avoid loading and parsing descriptor values again if the cache
		 * already contains a value list (even if it is empty) for the
		 * descriptor. Subclasses that override this method will check if the
		 * value list is empty and should only check once if they can provide
		 * actual values (e.g. for alias or documentation). That means that
		 * overriding methods should keep track, for example using class private
		 * boolean fields, if an attempt has already been made to access the
		 * values for a particular descriptor in a model specific way.
		 */
		if (this.descriptors.has(descriptor)) {

			return this.descriptors.values(descriptor);

		} else {

			List<LangString> result = new ArrayList<LangString>();
			this.descriptors.put(descriptor, result);

			String source = model().descriptorSource(descriptor);
			if (source.startsWith("tag#")) {

				/*
				 * NOTE: the default source for the descriptor 'documentation'
				 * in XMI10 and GCSR is: tag#documentation;description, that is
				 * why we split here and look at multiple tags in the subsequent
				 * for-loop. If a value is found in one iteration, we can break.
				 */
				String[] tags = source.replace("tag#", "").split(";");
				for (String tag : tags) {

					result.addAll(taggedValuesForTagAsLangStrings(tag));

					if (!result.isEmpty()) {
						break;
					}
				}

			} else if (source.startsWith("sc:extract#")) {
				String token = source.replace("sc:extract#", "");
				String doc = documentation();

				if (doc == null || doc.trim().length() == 0) {
					// nothing to extract from ...
				} else {
					String[] ss = doc.split(options().extractSeparator());
					boolean found = false;
					if (token.equals("PROLOG"))
						/*
						 * PROLOG is the start of the documentation before the
						 * first separator
						 */
						found = true;
					for (String s : ss) {
						if (found) {

							// ignore empty values
							if (s.trim().length() != 0) {
								result.add(new LangString(
										options().internalize(s.trim())));
							}
							break;
						} else if (s.trim().equalsIgnoreCase(token)) {
							found = true;
						}
					}
				}
			}

			/*
			 * NOTE: Model type specific sources (e.g. ea:alias, ea:notes, and
			 * ea:guidtoxml) must be handled in model type specific Info
			 * implementations that override this method.
			 */

			/*
			 * NOTE: Backwards compatibility for the descriptors DOCUMENTATION
			 * and DEFINITION, to provide the empty string if no value is found
			 * in the source, is handled by the methods documentation() and
			 * definition().
			 */

			return result;
		}
	}

	private void validateDescriptorsCache() {

		if (this.descriptors == null) {

			this.descriptors = new Descriptors();

			for (Descriptor descriptor : Descriptor.values()) {

				List<LangString> list = this.descriptorValues(descriptor);
				this.descriptors.putCopy(descriptor, list);
			}
		}
	}

	@Override
	public String primaryCode() {
		String[] values = filterDescriptorValues(Descriptor.PRIMARYCODE);
		if (values.length == 0) {
			return null;
		} else {
			return values[0];
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String globalIdentifier() {
		String[] values = filterDescriptorValues(Descriptor.GLOBALIDENTIFIER);
		if (values.length == 0) {
			return null;
		} else {
			return values[0];
		}
	}

	@Override
	public String derivedDocumentation(String template, String novalue) {
		String tmp = (template == null
				? Options.DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE
				: template);
		String nov = (novalue == null
				? Options.DERIVED_DOCUMENTATION_DEFAULT_NOVALUE
				: novalue);
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

		s = this.documentation();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("documentation", s.trim());

		s = this.aliasName();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("alias", s.trim());

		s = this.primaryCode();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("primaryCode", s.trim());

		s = this.globalIdentifier();
		if (s == null || s.trim().isEmpty())
			s = nov;
		replacements.put("globalIdentifier", s.trim());

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
		replacements.put("example", s.trim());

		sa = this.dataCaptureStatements();
		if (sa != null && sa.length > 0) {
			s = "";
			for (String e : sa)
				s += e.trim() + lf + lf;
			if (s.trim().isEmpty())
				s = nov;
		} else
			s = nov;
		replacements.put("dataCaptureStatement", s.trim());

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
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String documentation() {

		String[] values = filterDescriptorValues(Descriptor.DOCUMENTATION);
		if (values.length == 0) {
			return "";
		} else {
			return values[0];
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String definition() {
		String[] values = filterDescriptorValues(Descriptor.DEFINITION);
		if (values.length == 0) {
			return "";
		} else {
			return values[0];
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String description() {

		String[] values = filterDescriptorValues(Descriptor.DESCRIPTION);
		if (values.length == 0) {
			return null;
		} else {
			return values[0];
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String legalBasis() {
		String[] values = filterDescriptorValues(Descriptor.LEGALBASIS);
		if (values.length == 0) {
			return null;
		} else {
			return values[0];
		}
	}

	/**
	 * NOTE: this method is not final since several XXXInfoImpl classes override
	 * it
	 */
	@Override
	public String language() {

		String[] values = filterDescriptorValues(Descriptor.LANGUAGE);
		if (values.length == 0) {
			return null;
		} else {
			return values[0];
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String[] dataCaptureStatements() {

		String[] values = filterDescriptorValues(
				Descriptor.DATACAPTURESTATEMENT);

		return values;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String[] examples() {
		String[] values = filterDescriptorValues(Descriptor.EXAMPLE);

		return values;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String aliasName() {

		String[] values = filterDescriptorValues(Descriptor.ALIAS);
		if (values.length == 0) {
			return null;
		} else {
			return values[0];
		}
	}

	/**
	 * Filters the given values depending upon the type of descriptor and the
	 * language setting. At first, we search for strings with a language
	 * identifier that matches the configured one. If that search does not yield
	 * a result, search again for strings without a language identifier. In both
	 * cases, only the first match is returned if the descriptor is single
	 * valued - any additional match will be logged as a warning.
	 * 
	 * @param descriptor
	 * @param values
	 * @return array of values that apply to the descriptor; can be empty but
	 *         not <code>null</code>
	 */
	private String[] filterDescriptorValues(Descriptor descriptor) {

		/*
		 * IMPORTANT: This method must not use #descriptors() to get the values
		 * of a descriptor, because the validation of the descriptors cache
		 * requires accessing some descriptors to compute others (e.g. when
		 * extracting from the documentation). Those descriptors may not have
		 * been loaded when descriptors that depend on them are being loaded
		 * during validation of the descriptors cache. Access to the required
		 * descriptors applies this method, i.e. filtering of descriptor values,
		 * and since some input model types may provide a specific way to load
		 * some descriptors (like alias, global identifier and documentation in
		 * the case of an EA model) by overwriting the descriptorValues(...)
		 * method, we use that method here to get the actual values for the
		 * descriptor.
		 */
		List<LangString> lsList = descriptorValues(descriptor);

		List<String> result = new ArrayList<String>();

		// first search for strings with matching language identifier
		for (LangString ls : lsList) {

			if (ls.hasLang()
					&& ls.getLang().equalsIgnoreCase(options().language())) {

				if (descriptor.isSingleValued() && result.size() != 0) {
					MessageContext mc = model().result().addWarning(null, 704,
							descriptor.getName(), result.get(0).toString(),
							ls.toString());
					addContextDetails(mc);
				} else {
					result.add(ls.getValue());
				}
			}
		}

		/*
		 * if no language specific strings exist, search for ones without
		 * language tag
		 */
		if (result.isEmpty()) {
			for (LangString ls : lsList) {

				if (!ls.hasLang()) {

					if (descriptor.isSingleValued() && result.size() != 0) {
						MessageContext mc = model().result().addWarning(null,
								704, descriptor.getName(),
								result.get(0).toString(), ls.toString());
						addContextDetails(mc);
					} else {
						result.add(ls.getValue());
					}
				}
			}
		}

		return result.toArray(new String[0]);
	}

	public String encodingRule(String platform) {
		String s = taggedValue(platform + "EncodingRule");
		if (s == null || s.isEmpty()
				|| options().ignoreEncodingRuleTaggedValues()) {
		    
		    TargetRegistry tgtreg = options().getTargetRegistry();
		    
		    String tgtClassName = tgtreg.targetClassName(platform);
		    
		    if(tgtClassName != null) {
			s = options().parameter(tgtClassName,"defaultEncodingRule");
			if (s == null)
			    s = tgtreg.targetDefaultEncodingRule(platform);
		    }
		}
		if (s != null)
			s = s.toLowerCase();
		if (!options().getRuleRegistry().encRuleExists(s)) {
			result().addError(null, 181, s, platform);
		}
		return s;
	}

	public boolean matches(String rule) {
	    
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
		if (!options().getRuleRegistry().hasRule(rule)) {
			result().addError(null, 164, rule);
			return false;
		}

		/*
		 * If the test is target-specific match only if the target is active,
		 * i.e. is 'enabled' for rules and not 'disabled' for requirements and
		 * recommendations.
		 */
		if (ra[0].equals("rule") && !ra[1].equals("all")) {
			if (!options().targetMode(options().targetClassName(rule))
					.equals(ProcessMode.enabled))
				return false;
		}

		if (ra[0].matches("re[cq]") && !ra[1].equals("all")) {
			if (options().targetMode(options().targetClassName(rule))
					.equals(ProcessMode.disabled))
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
			param = options().parameter(Options.TargetXmlSchemaClass,
					"enumStyle");
			if (param != null && param.equalsIgnoreCase("local"))
				return true;
		}
		if (rule.equals("rule-xsd-cls-local-basictype")) {
			param = options().parameter(Options.TargetXmlSchemaClass,
					"basicTypeStyle");
			if (param != null && param.equalsIgnoreCase("local"))
				return true;
		}
		if (rule.equals("rule-xsd-pkg-schematron")) {
			param = options().parameter(Options.TargetXmlSchemaClass,
					"schematron");
			if (param != null && param.equalsIgnoreCase("true"))
				return true;
		}
		if (rule.equals("rule-xsd-prop-exclude-derived")) {
			param = options().parameter(Options.TargetXmlSchemaClass,
					"includeDerivedProperties");
			if (param != null && param.equalsIgnoreCase("false"))
				return true;
		}

		/*
		 * check if the rule has been configured for the encoding rule that
		 * applies to the element
		 */
		if (ra[1].equals("all")) {
			
			TargetRegistry tgtreg = options().getTargetRegistry();
			
			for(String targetIdentifier : tgtreg.getTargetIdentifiers()) {
			    String encRule = encodingRule(targetIdentifier);
			    if (encRule != null) {
				if(options().getRuleRegistry().hasRule(rule, encRule)) {
				    return true; 
				}
			    }
			}
			
			return false;

		} else {

			// determine if the applicable encoding rule contains the given rule
			String encRule = encodingRule(ra[1]).toLowerCase();

			if (encRule != null) {

				boolean encRuleHasRule = options().getRuleRegistry().hasRule(rule, encRule);

				if (encRuleHasRule) {

					// now cover cases in which a rule may be overwritten by a
					// parameter
					if (rule.equals("rule-xsd-all-no-documentation")) {

						// if the XML Schema target parameter
						// 'includeDocumentation' is set to true the rule is
						// overwritten
						param = options().parameter(
								Options.TargetXmlSchemaClass,
								"includeDocumentation");
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
	 * @deprecated (since="2.5.0") With UML 2, there may be multiple values per
	 *             tag. Use <code>taggedValuesAll(String tagOrTaglist)</code>
	 *             instead.
	 */
	@Deprecated
	public Map<String, String> taggedValues(String tagList) {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		return taggedValuesCache.getFirstValues(tagList);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public void removeTaggedValue(String tag) {

		// Validate tagged values cache first
		validateTaggedValuesCache();

		taggedValuesCache.remove(tag);
	}

	/**
	 * @deprecated (since="2.5.0") With UML 2, there may be multiple values per
	 *             tag. Use <code>taggedValuesAll()</code> instead.
	 */
	@Deprecated
	public Map<String, String> taggedValues() {
		// Validate tagged values cache first
		validateTaggedValuesCache();

		return taggedValuesCache.getFirstValues();
	}
}
