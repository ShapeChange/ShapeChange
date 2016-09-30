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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.SBVR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericFolConstraint;
import de.interactive_instruments.antlr.ShapeChangeAntlr.SbvrParserHelper;
import de.interactive_instruments.antlr.sbvr.SBVRLexer;
import de.interactive_instruments.antlr.sbvr.SBVRParser;

/**
 * Loads SBVR rule information from an Excel file so that the information can be
 * added as First Order Logic constraint - with type 'SBVR' - to classes in the
 * model.
 * 
 * @author Johannes Echterhoff
 *
 */
public class SbvrRuleLoader implements MessageSource {

	private Options options;
	private ShapeChangeResult result;
	private Model model;

	private SbvrParserHelper helper;

	/**
	 * Map with SBVR rule information obtained from an external source.
	 * 
	 * <ul>
	 * <li>key: class name</li>
	 * <li>value: mapping of schema package name to SBVR rule info
	 * <ul>
	 * <li>key: schema package name ({@value #UNSPECIFIED_SCHEMA_PACKAGE_NAME}
	 * if no schema package name has been provided)</li>
	 * <li>value: list of SBVR rules that apply to classes in that schema (the
	 * list is sorted according to lexical order on a) the class name and b) the
	 * rule text)</li>
	 * </ul>
	 * </ul>
	 */
	private TreeMap<String, TreeMap<String, List<SbvrRuleInfo>>> sbvrRules;

	public static final String UNSPECIFIED_SCHEMA_PACKAGE_NAME = "UNSPECIFIED";

	public SbvrRuleLoader(String sbvrFileLocation, Options options,
			ShapeChangeResult result, Model model) {

		this.options = options;
		this.result = result;
		this.model = model;

		if (sbvrFileLocation != null) {
			
			/*
			 * Before loading the rules, load model verbs and nouns because we might
			 * need to parse the main class name for a rule from the rule text.
			 */
			helper = SbvrUtil.createParserHelper(model);

			java.io.File sbvrFile = new java.io.File(sbvrFileLocation);
			boolean ex = true;
			if (!sbvrFile.exists()) {
				ex = false;
				if (!sbvrFileLocation.toLowerCase().endsWith(".xlsx")) {
					sbvrFileLocation += ".xlsx";
					sbvrFile = new java.io.File(sbvrFileLocation);
					ex = sbvrFile.exists();
				}
			}
			if (!ex) {

				result.addError(null, 36, sbvrFileLocation);

			} else {

				try {

					Workbook sbvrXls = WorkbookFactory.create(sbvrFile);
					sbvrRules = parseSBVRRuleInfos(sbvrXls);

				} catch (InvalidFormatException e) {

					result.addError(this, 1, e.getMessage());

				} catch (IOException e) {

					result.addError(this, 2, e.getMessage());
				}
			}
		}
	}

	/**
	 * @param sbvrXls
	 * @return mapping of schema package name to SBVR rules that apply to
	 *         classes in this schema *
	 *         <ul>
	 *         <li>key: class name</li>
	 *         <li>value: mapping of schema package name to SBVR rule info
	 *         <ul>
	 *         <li>key: schema package name (
	 *         {@value #UNSPECIFIED_SCHEMA_PACKAGE_NAME} if no schema package
	 *         name has been provided)</li>
	 *         <li>value: list of SBVR rules that apply to classes in that
	 *         schema (the list is sorted according to lexical order on a) the
	 *         class name and b) the rule text)</li>
	 *         </ul>
	 *         </ul>
	 */
	private TreeMap<String, TreeMap<String, List<SbvrRuleInfo>>> parseSBVRRuleInfos(
			Workbook sbvrXls) {

		TreeMap<String, TreeMap<String, List<SbvrRuleInfo>>> rules = new TreeMap<String, TreeMap<String, List<SbvrRuleInfo>>>();

		if (sbvrXls == null)
			return null;

		Sheet rulesSheet = null;

		for (int i = 0; i < sbvrXls.getNumberOfSheets(); i++) {

			String sheetName = sbvrXls.getSheetName(i);

			if (sheetName.equalsIgnoreCase("Constraints")) {
				rulesSheet = sbvrXls.getSheetAt(i);
				break;
			}
		}

		if (rulesSheet == null) {

			result.addError(this, 3);
			return null;
		}

		// read header row to determine which columns contain relevant
		// information
		Map<String, Integer> fieldIndexes = new HashMap<String, Integer>();

		Row header = rulesSheet.getRow(rulesSheet.getFirstRowNum());

		if (header == null) {
			result.addError(this, 4);
			return null;
		}

		boolean classNameFound = false;
		boolean commentsFound = false;
		boolean ruleNameFound = false;
		boolean ruleTextFound = false;
		boolean schemaPackageFound = false;

		for (short i = header.getFirstCellNum(); i < header.getLastCellNum(); i++) {

			Cell c = header.getCell(i, Row.RETURN_BLANK_AS_NULL);

			if (c == null) {
				// this is allowed
			} else {

				String value = c.getStringCellValue();

				if (value.equalsIgnoreCase(SbvrRuleInfo.CLASS_COLUMN_NAME)) {

					fieldIndexes.put(SbvrRuleInfo.CLASS_COLUMN_NAME, (int) i);
					classNameFound = true;

				} else if (value
						.equalsIgnoreCase(SbvrRuleInfo.COMMENT_COLUMN_NAME)) {

					fieldIndexes.put(SbvrRuleInfo.COMMENT_COLUMN_NAME, (int) i);
					commentsFound = true;

				} else if (value
						.equalsIgnoreCase(SbvrRuleInfo.SCHEMA_PACKAGE_COLUMN_NAME)) {

					fieldIndexes.put(SbvrRuleInfo.SCHEMA_PACKAGE_COLUMN_NAME,
							(int) i);
					schemaPackageFound = true;

				} else if (value
						.equalsIgnoreCase(SbvrRuleInfo.RULE_TEXT_COLUMN_NAME)) {

					fieldIndexes.put(SbvrRuleInfo.RULE_TEXT_COLUMN_NAME,
							(int) i);
					ruleTextFound = true;

				} else if (value
						.equalsIgnoreCase(SbvrRuleInfo.RULE_NAME_COLUMN_NAME)) {

					fieldIndexes.put(SbvrRuleInfo.RULE_NAME_COLUMN_NAME,
							(int) i);
					ruleNameFound = true;
				}
			}
		}

		// if (fieldIndexes.size() != 5) {
		if (!ruleNameFound && !ruleTextFound) {
			// log message that required fields were not found
			result.addError(this, 5);
			return null;
		}

		/*
		 * Read rule content
		 */
		for (int i = rulesSheet.getFirstRowNum() + 1; i <= rulesSheet
				.getLastRowNum(); i++) {

			Row r = rulesSheet.getRow(i);
			int rowNumber = i + 1;

			if (r == null) {
				// ignore empty rows
				continue;
			}

			SbvrRuleInfo sri = new SbvrRuleInfo();

			// get rule name (required)
			Cell c = r.getCell(
					fieldIndexes.get(SbvrRuleInfo.RULE_NAME_COLUMN_NAME),
					Row.RETURN_BLANK_AS_NULL);
			if (c == null) {
				// log message
				result.addWarning(this, 6, "" + rowNumber);
				continue;
			} else {
				String cellValue = c.getStringCellValue();
				if (cellValue != null) {
					if (cellValue.contains(":")) {
						sri.setName(cellValue.substring(cellValue
								.lastIndexOf(":") + 1));
					} else {
						sri.setName(cellValue);
					}
				}
			}

			// get rule text (required)
			c = r.getCell(fieldIndexes.get(SbvrRuleInfo.RULE_TEXT_COLUMN_NAME),
					Row.RETURN_BLANK_AS_NULL);
			if (c == null) {
				// log message
				result.addWarning(this, 7, "" + rowNumber);
				continue;
			} else {
				sri.setText(c.getStringCellValue());
			}

			// get comment (optional)
			if (commentsFound) {
				c = r.getCell(
						fieldIndexes.get(SbvrRuleInfo.COMMENT_COLUMN_NAME),
						Row.RETURN_BLANK_AS_NULL);
				if (c != null) {
					sri.setComment(c.getStringCellValue());
				}
			}

			// get schema package (optional)
			if (schemaPackageFound) {
				c = r.getCell(fieldIndexes
						.get(SbvrRuleInfo.SCHEMA_PACKAGE_COLUMN_NAME),
						Row.RETURN_BLANK_AS_NULL);
				if (c == null) {
					sri.setSchemaPackageName(UNSPECIFIED_SCHEMA_PACKAGE_NAME);
				} else {
					sri.setSchemaPackageName(c.getStringCellValue());
				}
			}

			/*
			 * get class name (optional when loading from excel because later we
			 * can still try parsing it from the rule text)
			 */
			if (classNameFound) {
				c = r.getCell(fieldIndexes.get(SbvrRuleInfo.CLASS_COLUMN_NAME),
						Row.RETURN_BLANK_AS_NULL);
				if (c == null) {
					/*
					 * then after this we'll try to parse the class name from
					 * the rule text
					 */
				} else {
					sri.setClassName(c.getStringCellValue());
				}
			}

			if (sri.getClassName() == null) {

				/*
				 * try parsing the main class name from the rule text
				 */
				result.addInfo(this, 10, sri.getName());

				String mainClassName = parseClassNameFromRuleText(sri.getText());

				if (mainClassName == null) {
					result.addWarning(this, 8, sri.getName());
					continue;
				} else {
					sri.setClassName(mainClassName);
				}
			}

			List<SbvrRuleInfo> rulesList;
			TreeMap<String, List<SbvrRuleInfo>> rulesBySchemaPackageName;

			if (rules.containsKey(sri.getClassName())) {

				rulesBySchemaPackageName = rules.get(sri.getClassName());

				if (rulesBySchemaPackageName.containsKey(sri
						.getSchemaPackageName())) {
					rulesList = rulesBySchemaPackageName.get(sri
							.getSchemaPackageName());
				} else {
					rulesList = new ArrayList<SbvrRuleInfo>();
					rulesBySchemaPackageName.put(sri.getSchemaPackageName(),
							rulesList);
				}

			} else {

				rulesBySchemaPackageName = new TreeMap<String, List<SbvrRuleInfo>>();
				rules.put(sri.getClassName(), rulesBySchemaPackageName);

				rulesList = new ArrayList<SbvrRuleInfo>();
				rulesBySchemaPackageName.put(sri.getSchemaPackageName(),
						rulesList);
			}

			rulesList.add(sri);
		}

		// now sort all lists contained in the map
		for (TreeMap<String, List<SbvrRuleInfo>> rulesBySchemaPackageName : rules
				.values()) {
			for (List<SbvrRuleInfo> rulesList : rulesBySchemaPackageName
					.values()) {

				Collections.sort(rulesList, new Comparator<SbvrRuleInfo>() {

					@Override
					public int compare(SbvrRuleInfo o1, SbvrRuleInfo o2) {

						int classNameComparison = o1.getClassName().compareTo(
								o2.getClassName());

						if (classNameComparison != 0) {
							return classNameComparison;
						} else {
							return o1.getText().compareTo(o2.getText());
						}
					}
				});
			}
		}

		return rules;
	}

	private String parseClassNameFromRuleText(String text) {

		ANTLRInputStream input = new ANTLRInputStream(text);

		// create a lexer that feeds off of input CharStream
		SBVRLexer lexer = new SBVRLexer(input);

		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// create a parser that feeds off the tokens buffer
		SBVRParser parser = new SBVRParser(tokens);
		parser.helper = helper;

		/*
		 * remove ConsoleErrorListener and add our own
		 */
		parser.removeErrorListeners();
		SbvrErrorListener parsingErrorListener = new SbvrErrorListener();
		parser.addErrorListener(parsingErrorListener);

		// execute parsing, starting with rule 'sentence'
		ParseTree tree = parser.sentence();

		// if there were parsing errors, we cannot identify the main class name
		if (parsingErrorListener.hasErrors()) {

			SbvrUtil.printErrors(parsingErrorListener.getErrors(), text, result, false);

			return null;

		} else {

			SbvrClassNameDetectionListener nameDetectionListener = new SbvrClassNameDetectionListener();

			ParseTreeWalker walker = new ParseTreeWalker();
			walker.walk(nameDetectionListener, tree);

			return nameDetectionListener.getMainClassName();
		}
	}

	/**
	 * Enriches the model by adding SBVR constraints to the classes in the given
	 * schema.
	 * 
	 * @param schemaPackage
	 *            a schema package (NOT any of the child packages that are in
	 *            the same target namespace)
	 */
	public void loadSBVRRulesAsConstraints(PackageInfo schemaPackage) {

		// check that there are rules
		if (sbvrRules == null || sbvrRules.isEmpty())
			return;

		// load information for all classes in the schema
		Set<ClassInfo> classesInSchema = model.classes(schemaPackage);

		for (ClassInfo ci : classesInSchema) {

			if (options.isAIXM() && ci.category() == Options.AIXMEXTENSION) {
				// do not add constraints to AIXM <<extension>> types
				continue;
			}

			/*
			 * create SBVR constraints based upon the rules defined for that
			 * class in its schema but also in case of an unspecified schema
			 * (i.e. apply those rules as well where no schema package info has
			 * been provided)
			 */
			List<SbvrRuleInfo> rulesForClass = new ArrayList<SbvrRuleInfo>();

			if (sbvrRules.containsKey(ci.name())) {

				TreeMap<String, List<SbvrRuleInfo>> rulesBySchemaPackageName = sbvrRules
						.get(ci.name());

				if (rulesBySchemaPackageName.containsKey(schemaPackage.name())) {
					rulesForClass.addAll(rulesBySchemaPackageName
							.get(schemaPackage.name()));
				} else {
					// fine, no sbvr rules specific to the given schema defined
					// for this class
				}

				if (rulesBySchemaPackageName
						.containsKey(UNSPECIFIED_SCHEMA_PACKAGE_NAME)) {
					rulesForClass.addAll(rulesBySchemaPackageName
							.get(UNSPECIFIED_SCHEMA_PACKAGE_NAME));
				} else {
					// fine, no sbvr rules without specific schema defined for
					// this class
				}

			} else {
				// no SBVR rules defined for this class in the external source
			}

			/*
			 * Now create SBVR constraints for the rule info contained in the
			 * combined list.
			 * 
			 * NOTE: parsing of the constraints is done during model
			 * postprocessing. See ModelImpl.postprocessFolConstraints()
			 */
			List<Constraint> constraints = ci.constraints();

			if (constraints == null) {

				result.addWarning(this, 9, ci.name());

			} else {

				for (SbvrRuleInfo rule : rulesForClass) {

					GenericFolConstraint genCon = new GenericFolConstraint(ci,
							rule.getName(), "", SbvrConstants.FOL_SOURCE_TYPE,
							rule.getText());

					constraints.add(genCon);
				}
			}
		}
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Invalid format for excel file containing SBVR constraints. Message is: $1$";
		case 2:
			return "Could not read excel file containing SBVR rules. Message is: $1$";
		case 3:
			return "No constraints sheet found in excel file containing SBVR rules.";
		case 4:
			return "Header not found in SBVR rules sheet.";
		case 5:
			return "Did not find required columns in SBVR rules sheet.";
		case 6:
			return "No name found for rule declared in line $1$ of SBVR rules sheet. This kind of message can occur for rows the are seemingly empty in the excel sheet.";
		case 7:
			return "No text found for rule declared in line $1$ of SBVR rules sheet.";
		case 8:
			return "Parsing main class name for rule '$1$' was not successful. This rule will not be added to the model.";
		case 9:
			return "Cannot load SBVR rules from external source for class '$1$' because the constraint vector of the class is null.";
		case 10:
			return "No main class name provided for rule '$1$'. Parsing the name from the rule text.";

		default:
			return "(Unknown message)";
		}
	}
}
