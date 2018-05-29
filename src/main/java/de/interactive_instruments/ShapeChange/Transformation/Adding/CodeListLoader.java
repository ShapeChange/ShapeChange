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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Adding;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class CodeListLoader implements Transformer, MessageSource {

	public static final String RULE_LOAD_CODES = "rule-trf-cls-loadCodes";

	public static final String TV_CL_SOURCE = "codeListSource";
	public static final String TV_CL_SOURCE_CHARSET = "codeListSourceCharset";
	public static final String TV_CL_SOURCE_REPRESENTATION = "codeListSourceRepresentation";

	public enum CodeListSourceRepresentation {

		ISO_639_2("application/x.iso639_2");

		private String repName;

		CodeListSourceRepresentation(String repName) {
			this.repName = repName;
		}

		public String getName() {
			return this.repName;
		}

		public static CodeListSourceRepresentation fromString(String repName)
				throws IllegalArgumentException {
			for (CodeListSourceRepresentation clsr : CodeListSourceRepresentation
					.values()) {
				if (repName.equalsIgnoreCase(clsr.getName())) {
					return clsr;
				}
			}
			// no corresponding enum found
			throw new IllegalArgumentException(
					"Code list representation '" + repName + "' is unknown.");
		}

	}

	private Options options = null;
	private ShapeChangeResult result = null;
	private Set<String> rules = null;
	private TransformerConfiguration config = null;
	private GenericModel model = null;

	@Override
	public void process(GenericModel model, Options options,
			TransformerConfiguration config, ShapeChangeResult result)
			throws ShapeChangeAbortException {

		this.model = model;
		this.options = options;
		this.result = result;
		this.config = config;

		Map<String, ProcessRuleSet> ruleSets = config.getRuleSets();

		// for now we simply get the set of all rules defined for the
		// transformation
		rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		if (rules.contains(RULE_LOAD_CODES)) {

			result.addInfo(null, 20103, RULE_LOAD_CODES);
			applyRuleLoadCodes();
		}
	}

	private void applyRuleLoadCodes() {

		for (GenericClassInfo genCi : model.selectedSchemaClasses()) {

			if (genCi.category() == Options.CODELIST && StringUtils
					.isNotBlank(genCi.taggedValue(TV_CL_SOURCE))) {

				// load tagged values
				String clSource = genCi.taggedValue(TV_CL_SOURCE).trim();

				String clSourceCharsetName = genCi
						.taggedValue(TV_CL_SOURCE_CHARSET);
				Charset clSourceCharset = StandardCharsets.UTF_8;

				if (StringUtils.isNotBlank(clSourceCharsetName)) {
					try {
						clSourceCharset = Charset
								.forName(clSourceCharsetName.trim());
					} catch (Exception e) {
						MessageContext mc = result.addError(this, 100,
								clSourceCharsetName.trim(), genCi.name(),
								e.getMessage());
						if (mc != null) {
							mc.addDetail(this, 2, genCi.fullNameInSchema());
						}
					}
				}

				String clSourceRepresentationName = genCi
						.taggedValue(TV_CL_SOURCE_REPRESENTATION);
				if (StringUtils.isBlank(clSourceRepresentationName)) {
					MessageContext mc = result.addError(this, 101,
							genCi.name());
					if (mc != null) {
						mc.addDetail(this, 2, genCi.fullNameInSchema());
					}
					continue;
				}
				CodeListSourceRepresentation clSourceRepresentation = null;
				try {
					clSourceRepresentation = CodeListSourceRepresentation
							.fromString(clSourceRepresentationName);
				} catch (IllegalArgumentException e) {
					MessageContext mc = result.addError(this, 102, genCi.name(),
							e.getMessage());
					if (mc != null) {
						mc.addDetail(this, 2, genCi.fullNameInSchema());
					}
					continue;
				}

				// load code list source
				if (clSourceRepresentation == CodeListSourceRepresentation.ISO_639_2) {

					File tmpFile = null;

					try {

						tmpFile = File.createTempFile(
								"ShapeChange_CodeListLoader", genCi.name());
						tmpFile.deleteOnExit();

						if (clSource.toLowerCase().startsWith("http")) {

							/*
							 * 2018-05-16 JE: The library of congress web server
							 * where the ISO 639-2 code list is hosted appears
							 * to forbid access from java programs. When simply
							 * opening a connection without setting a specific
							 * request property, a 403 HTTP exception occurs.
							 * According to
							 * https://stackoverflow.com/questions/2529682/
							 * setting-user-agent-of-a-java-urlconnection we
							 * need to trick the server into believing that we
							 * access the list from a web browser. Setting
							 * system property 'http.agent' to 'Chrome' did
							 * work, but I prefer a solution with specific
							 * setting for just the one connection, not a global
							 * setting. Setting the 'User-Agent' as below does
							 * work (at least for now).
							 */

							URL clSourceUrl = new URL(clSource);
							URLConnection urlConn = clSourceUrl
									.openConnection();
							urlConn.setRequestProperty("User-Agent",
									"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");

							FileUtils.copyInputStreamToFile(
									urlConn.getInputStream(), tmpFile);

						} else {

							File clSourceFile = new File(clSource);

							if (clSourceFile.exists()) {
								FileUtils.copyFile(clSourceFile, tmpFile);
							} else {
								result.addError(this, 105, genCi.name(),
										clSourceFile.getAbsolutePath());
								continue;
							}
						}

					} catch (Exception e) {
						result.addError(this, 106, genCi.name(), clSource,
								tmpFile.getAbsolutePath(), e.getMessage());
						continue;

					}

					SortedMap<String, String> nameEn_by_alpha3bib = new TreeMap<>();

					/*
					 * 2018-05-16 JE: The UTF-8 file provided by the library of
					 * congress for 639-2 codes contains a ByteOrderMark (BOM)
					 * at the start of the file. The BOM must be stripped,
					 * otherwise the first value read from the first line
					 * includes it as empty/null character. That character is
					 * not shown when the whole string is printed, but it can
					 * mess up regular expressions and is not removed by
					 * String#trim(). Apache Commons IO BOMInputStream can be
					 * used to remove the BOM.
					 */
					try (FileInputStream fis = new FileInputStream(tmpFile);
							BOMInputStream bomis = new BOMInputStream(fis,
									false);
							Reader reader = new InputStreamReader(bomis,
									clSourceCharset)) {

						Iterable<CSVRecord> records = CSVFormat.newFormat('|')
								.withHeader("alpha3bibliographic",
										"alpha3terminologic", "alpha2",
										"name_en", "name_fr")
								.parse(reader);

						for (CSVRecord record : records) {

							String alpha3bib = StringUtils
									.strip(record.get("alpha3bibliographic"));
							// String alpha3term = StringUtils.strip(record
							// .get("alpha3terminologic"));
							// String alpha2 =
							// StringUtils.strip(record.get("alpha2"));
							String name_en = StringUtils
									.strip(record.get("name_en"));
							// String name_fr =
							// StringUtils.strip(record.get("name_fr"));

							nameEn_by_alpha3bib.put(alpha3bib, name_en);

							// System.out.println(
							// alpha3bib + "," + alpha3term + "," + alpha2
							// + "," + name_en + "," + name_fr);
						}

						int index = 0;
						for (Entry<String, String> code : nameEn_by_alpha3bib
								.entrySet()) {
							index++;

							String alpha3bib = code.getKey();
							String name_en = code.getValue();

							GenericPropertyInfo genPi = new GenericPropertyInfo(
									model, alpha3bib + "_codeFor_" + genCi.id(),
									alpha3bib);

							// set remaining properties required by Info
							// interface
							genPi.setTaggedValues(options.taggedValueFactory(),
									false);
							genPi.setStereotypes(options.stereotypesFactory());

							Descriptors desc = new Descriptors();
							desc.put(Descriptor.DEFINITION, name_en);
							desc.put(Descriptor.DOCUMENTATION, name_en);
							genPi.setDescriptors(desc);
							genPi.setProfiles(new Profiles());

							// set remaining properties required by PropertyInfo
							// interface

							genPi.setDerived(false);
							genPi.setReadOnly(false);
							genPi.setAttribute(true);
							genPi.setTypeInfo(null);
							genPi.setNavigable(true);
							genPi.setOrdered(false);
							genPi.setUnique(false);
							genPi.setComposition(true);
							genPi.setAggregation(false);
							genPi.setCardinality(new Multiplicity());
							genPi.setInitialValue(null);
							genPi.setInlineOrByReference("inlineOrByReference");
							genPi.setReverseProperty(null);
							genPi.setInClass(genCi);
							StructuredNumber strucNum = new StructuredNumber(
									index);
							genPi.setSequenceNumber(strucNum, true);
							genPi.setConstraints(null);
							genPi.setAssociation(null);
							genPi.setRestriction(false);
							genPi.setNilReasonAllowed(false);

							model.add(genPi, genCi);
						}

					} catch (IOException e) {
						MessageContext mc = result.addError(this, 104,
								genCi.name(), e.getMessage());
						if (mc != null) {
							mc.addDetail(this, 2, genCi.fullNameInSchema());
						}
						continue;
					}
				}
			}
		}
	}

	@Override
	public String message(int mnr) {

		/*
		 * NOTE: A leading ?? in a message text suppresses multiple appearance
		 * of a message in the output.
		 */
		switch (mnr) {

		case 1:
			return "Context: property '$1$'";
		case 2:
			return "Context: class '$1$'";

		// 100-199 Messages for RULE_LOAD_CODES
		case 100:
			return "Could not load charset '$1$' defined for source of code list '$2$'. Message is: $3$. UTF-8 will be used as fallback.";
		case 101:
			return "No representation defined for source of code list '$1$'. The code list will be ignored.";
		case 102:
			return "Representation for source of code list '$1$' could not be identified. Message is: $2$. The code list will be ignored.";
		case 103:
			return "";
		case 104:
			return "Exception occurred while reading source file for code list '$1$'. Message is: '$2$'. The code list will be ignored.";
		case 105:
			return "Source file for code list '$1$' not found at location '$2$'. The code list will be ignored.";
		case 106:
			return "Could not copy source file for code list '$1$' from '$2$' to '$3$'. Message is: $4$. The code list will be ignored.";
		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
