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
package de.interactive_instruments.ShapeChange.Target.Statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class ApplicationSchemaStatistic implements SingleTarget, MessageSource {

	private ShapeChangeResult result = null;
	@SuppressWarnings("unused")
	private PackageInfo schema = null;
	@SuppressWarnings("unused")
	private Model model = null;
	private Options options = null;

	private static String outputDirectory = null;
	private static String outputFilename = null;
	private static final String indent1 = "  ";
	private static final String indent2 = indent1 + indent1;

	private int streamBufferSize = 8 * 1042 * 1042;

	private static boolean initialised = false;
	private static boolean printed = false;

	/**
	 * Holds statistics for all selected schemas. Is filled while schemas are
	 * being processed, and read during writeAll.
	 */
	private static List<SchemaStatistic> schemaStats = null;

	/**
	 * Holds statistic information for the schema that is currently being
	 * processed.
	 */
	private static SchemaStatistic schemaStat = null;

	/**
	 * This map is used to keep track of the names of the application schema
	 * that are encountered during processing. Whenever this Target is
	 * initialized with a new application schema package, the name of that
	 * schema is added to the map as a key - with a 1 integer as value in case
	 * that key was not present before, otherwise increasing the existing value
	 * by one (and in that case altering the name of the application schema
	 * during print accordingly [adding "(integer_value)"]). This is used to
	 * ensure that application schema with the same name are disambiguated
	 * during print.
	 */
	private static Map<String, Integer> encounteredAppSchemasByName = null;

	@Override
	public void initialise(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
					throws ShapeChangeAbortException {

		schema = pi;
		model = m;
		options = o;
		result = r;

		if (!initialised) {
			initialised = true;

			encounteredAppSchemasByName = new HashMap<String, Integer>();
			schemaStats = new ArrayList<SchemaStatistic>();

			outputDirectory = options.parameter(this.getClass().getName(),
					"outputDirectory");
			if (outputDirectory == null)
				outputDirectory = options.parameter("outputDirectory");
			if (outputDirectory == null)
				outputDirectory = ".";

			outputFilename = options.parameter(this.getClass().getName(),
					"outputFilename");
			if (outputFilename == null)
				outputFilename = "ModelStatistic";
		}

		// Determine if app schema with same name has been encountered before,
		// and choose name accordingly
		String nameForAppSchema = null;

		if (encounteredAppSchemasByName.containsKey(pi.name())) {
			int count = encounteredAppSchemasByName.get(pi.name()).intValue();
			count++;
			nameForAppSchema = pi.name() + " (" + count + ")";
			encounteredAppSchemasByName.put(pi.name(), Integer.valueOf(count));
		} else {
			nameForAppSchema = pi.name();
			encounteredAppSchemasByName.put(pi.name(), Integer.valueOf(1));
		}

		schemaStat = new SchemaStatistic(nameForAppSchema, pi);
		schemaStats.add(schemaStat);
	}

	@Override
	public void process(ClassInfo ci) {

		ClassStatistic cs = new ClassStatistic(ci);
		schemaStat.add(cs);
	}

	@Override
	public void write() {
		// irrelevant for this SingleTarget
	}

	@Override
	public String getTargetName(){
		return "Application Schema Statistics";
	}

	@Override
	public void writeAll(ShapeChangeResult r) {

		if (printed) {
			return;
		}

		result = r;

		/*
		 * once all classes from selected schemas have been processed we can
		 * also compute statistics for the selected schemas in general
		 */
		for (SchemaStatistic sc : schemaStats) {
			sc.computeStatistics();
		}

		printTextFile();
	}

	public void printTextFile() {

		String fileName = outputFilename + ".txt";
		
		PrintWriter writer = null;
		
		try {

			File outputDirectoryFile = new File(outputDirectory);
			if (!outputDirectoryFile.exists()) {
				FileUtils.forceMkdir(outputDirectoryFile);
			}
			
			File file = new File(outputDirectory, fileName);
			
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8"),this.streamBufferSize));
			
			for (SchemaStatistic ss : schemaStats) {

				writer.println("----------");

				writer.println(ss.getSchemaName());

				writer.println(indent1 + "Number of properties in schema: "
						+ ss.numberOfProperties());
				
				writer.println(indent1 + "Number of relationships to features in schema: "
						+ ss.numberOfFeatureRelationships());

				writer.println(indent1 + "Percent of relationships to features: "
						+ String.format("%.2f",
								ss.percentOfFeatureRelationships()));

				writer.println("----------");

				for (ClassStatistic cs : ss.getClassStatistics()) {

					writer.println(indent1 + categoryAsString(cs.getClassInfo())
							+ " " + cs.getClassInfo().name());

					writer.println(indent2 + "Number of properties: "
							+ cs.numberOfProperties());
					
					writer.println(indent2 + "Number of relationships to features: "
							+ cs.numberOfFeatureRelationships());

					writer.println(indent2
							+ "Percent of relationships to features: "
							+ String.format("%.2f",
									cs.percentOfFeatureRelationships()));
				}
			}
			
			writer.close();

			result.addResult(getTargetName(), outputDirectory, fileName, null);

			printed = true;

		} catch (IOException e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}

			e.printStackTrace(System.err);
			
		} finally {
			
			if(writer != null) {
				writer.flush();
				IOUtils.closeQuietly(writer);
			}
		}
	}

	private String categoryAsString(ClassInfo classInfo) {

		switch (classInfo.category()) {

		case Options.FEATURE:
			return "<<featureType>>";
		case Options.OBJECT:
			return "<<type>>";
		case Options.DATATYPE:
			return "<<dataType>>";
		case Options.ENUMERATION:
			return "<<enumeration>>";
		case Options.CODELIST:
			return "<<codeList>>";
		case Options.ATTRIBUTECONCEPT:
			return "<<attributeConcept>>";
		case Options.ROLECONCEPT:
			return "<<roleConcept>>";
		case Options.FEATURECONCEPT:
			return "<<featureConcept";
		case Options.BASICTYPE:
			return "<<basicType>>";
		case Options.MIXIN:
			return "<<type>> (mixin)";
		case Options.UNION:
			return "<<union>>";
		case Options.OKSTRAFID:
			return "<<fachId>>";
		case Options.OKSTRAKEY:
			return "<<schluesseltabelle>>";
		case Options.VALUECONCEPT:
			return "<<valueConcept>>";
		default:
			return "<unknown stereotyped> class";
		}
	}

	@Override
	public void registerRulesAndRequirements(RuleRegistry r) {
	 // no rules or requirements defined for this target, thus nothing to do	    
	}
	
	@Override
	public void reset() {
		initialised = false;
		outputDirectory = null;
		outputFilename = null;
		printed = false;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class ApplicationSchemaStatistic";
		default:
			return "(" + ApplicationSchemaStatistic.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
