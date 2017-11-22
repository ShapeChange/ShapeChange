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

package de.interactive_instruments.ShapeChange.Target.Mapping;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

public class Excel implements Target {

	private static final String NS_SS = "urn:schemas-microsoft-com:office:spreadsheet";
	private static final String NS_O = "urn:schemas-microsoft-com:office:office";
	private static final String NS_X = "urn:schemas-microsoft-com:office:excel";
	private static final String NS_HTML = "http://www.w3.org/TR/REC-html40";

	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;
	private boolean printed = false;
	private Document document = null;
	private Element root = null;
	private Element styles = null;
	private Element table = null;
	private String outputDirectory = null;
	private boolean diagnosticsOnly = false;
	private String documentationTemplate = null;
	private String documentationNoValue = null;

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
					throws ShapeChangeAbortException {
		pi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter(".");

		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(), "documentationTemplate");
		documentationNoValue = options.parameter(this.getClass().getName(), "documentationNoValue");		
		
		// Check if we can use the output directory; create it if it
		// does not exist
		File outputDirectoryFile = new File(outputDirectory);
		boolean exi = outputDirectoryFile.exists();
		if (!exi) {
			try {
				FileUtils.forceMkdir(outputDirectoryFile);
			} catch (IOException e) {
				result.addError(null, 600, e.getMessage());
				e.printStackTrace(System.err);
			}
			exi = outputDirectoryFile.exists();
		}
		boolean dir = outputDirectoryFile.isDirectory();
		boolean wrt = outputDirectoryFile.canWrite();
		boolean rea = outputDirectoryFile.canRead();
		if (!exi || !dir || !wrt || !rea) {
			result.addFatalError(null, 601, outputDirectory);
			throw new ShapeChangeAbortException();
		}

		// FIXME correct message?
		result.addDebug(null, 10005, pi.name());

		document = createDocument();

		root = document.createElementNS(NS_SS, "Workbook");
		document.appendChild(root);
		addAttribute(document, root, "xmlns", NS_SS);
		addAttribute(document, root, "xmlns:ss", NS_SS);
		addAttribute(document, root, "xmlns:o", NS_O);
		addAttribute(document, root, "xmlns:x", NS_X);
		addAttribute(document, root, "xmlns:html", NS_HTML);

		Element e1, e2, e3;

		e1 = document.createElementNS(NS_O, "DocumentProperties");
		root.appendChild(e1);

		e2 = document.createElementNS(NS_O, "Author");
		e1.appendChild(e2);
		e2.appendChild(document.createTextNode("ShapeChange"));

		styles = document.createElementNS(NS_SS, "Styles");
		root.appendChild(styles);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "Default");
		addAttribute(document, e1, "ss:Name", "Normal");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Font");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Interior");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "NumberFormat");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Protection");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s0");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Center");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "8.0");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s1");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Center");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "8.0");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Interior");
		addAttribute(document, e2, "ss:Color", "#FCF305");
		addAttribute(document, e2, "ss:Pattern", "Solid");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s2");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Center");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "2");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "2");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "2");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "2");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "12.0");
		addAttribute(document, e2, "ss:Bold", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Interior");
		addAttribute(document, e2, "ss:Color", "#99CCFF");
		addAttribute(document, e2, "ss:Pattern", "Solid");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s3");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Center");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "2");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "2");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "11.0");
		addAttribute(document, e2, "ss:Bold", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Interior");
		addAttribute(document, e2, "ss:Color", "#B0B0B0");
		addAttribute(document, e2, "ss:Pattern", "Solid");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s4");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Left");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "10.0");
		addAttribute(document, e2, "ss:Bold", "1");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s41");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Left");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "10.0");
		addAttribute(document, e2, "ss:Bold", "1");
		addAttribute(document, e2, "ss:Color", "#0000FF");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s42");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Left");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "10.0");
		addAttribute(document, e2, "ss:Bold", "1");
		addAttribute(document, e2, "ss:Color", "#008080");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s5");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Left");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "8.0");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s51");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Left");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "8.0");
		addAttribute(document, e2, "ss:Color", "#0000FF");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Style");
		addAttribute(document, e1, "ss:ID", "s52");
		styles.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Alignment");
		addAttribute(document, e2, "ss:Horizontal", "Left");
		addAttribute(document, e2, "ss:Vertical", "Top");
		addAttribute(document, e2, "ss:WrapText", "1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Borders");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Bottom");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Left");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Right");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e3 = document.createElementNS(NS_SS, "Border");
		addAttribute(document, e3, "ss:Position", "Top");
		addAttribute(document, e3, "ss:LineStyle", "Continuous");
		addAttribute(document, e3, "ss:Weight", "1");
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Font");
		addAttribute(document, e2, "ss:Size", "8.0");
		addAttribute(document, e2, "ss:Color", "#008080");
		e1.appendChild(e2);

		e1 = document.createElementNS(NS_SS, "Worksheet");
		addAttribute(document, e1, "ss:Name", "Matching Table");
		root.appendChild(e1);

		table = document.createElementNS(NS_SS, "Table");
		addAttribute(document, table, "x:FullColumns", "1");
		addAttribute(document, table, "x:FullRows", "1");
		addAttribute(document, table, "ss:StyleID", "s0");
		addAttribute(document, table, "ss:DefaultRowHeight", "10.0");
		addAttribute(document, table, "ss:DefaultColumnWidth", "100.0");
		e1.appendChild(table);

		for (int i = 0; i < 7; i++) {
			e1 = document.createElementNS(NS_SS, "Column");
			addAttribute(document, e1, "ss:StyleID", "s0");
			addAttribute(document, e1, "ss:AutoFitWidth", "0");
			addAttribute(document, e1, "ss:Width", "100.0");
			table.appendChild(e1);
		}

		e1 = document.createElementNS(NS_SS, "Column");
		addAttribute(document, e1, "ss:StyleID", "s1");
		addAttribute(document, e1, "ss:AutoFitWidth", "0");
		addAttribute(document, e1, "ss:Width", "12.0");
		table.appendChild(e1);

		for (int i = 0; i < 9; i++) {
			e1 = document.createElementNS(NS_SS, "Column");
			addAttribute(document, e1, "ss:StyleID", "s0");
			addAttribute(document, e1, "ss:AutoFitWidth", "0");
			addAttribute(document, e1, "ss:Width", "100.0");
			table.appendChild(e1);
		}

		e1 = document.createElementNS(NS_SS, "Row");
		addAttribute(document, e1, "ss:AutoFitHeight", "0");
		addAttribute(document, e1, "ss:Height", "19.0");
		table.appendChild(e1);
		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:MergeAcross", "6");
		addAttribute(document, e2, "ss:StyleID", "s2");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e3.appendChild(document.createTextNode("Application Schema '"
				+ pi.name() + "' (version " + pi.version() + ")"));
		e2.appendChild(e3);
		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s1");
		e1.appendChild(e2);
		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:MergeAcross", "8");
		addAttribute(document, e2, "ss:StyleID", "s2");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e3.appendChild(document.createTextNode(
				"Application Schema <provide name of source schema>"));
		e2.appendChild(e3);

		e1 = document.createElementNS(NS_SS, "Row");
		addAttribute(document, e1, "ss:AutoFitHeight", "1");
		addAttribute(document, e1, "ss:Height", "40.0");
		table.appendChild(e1);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Type"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Documentation"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);

		Element e4, e5;
		e4 = document.createElementNS(NS_HTML, "B");
		e4.appendChild(document.createTextNode("Attribute\r"));
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#0000FF");
		e5.appendChild(document.createTextNode("Association role\r"));
		e4.appendChild(e5);
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#008080");
		e5.appendChild(document.createTextNode("Constraint"));
		e4.appendChild(e5);
		e3.appendChild(e4);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);

		e4 = document.createElementNS(NS_HTML, "B");
		e4.appendChild(document.createTextNode("Attribute / "));
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#0000FF");
		e5.appendChild(document.createTextNode("Association role"));
		e4.appendChild(e5);
		e4.appendChild(document.createTextNode(" / "));
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#008080");
		e5.appendChild(document.createTextNode("Constraint"));
		e4.appendChild(e5);
		e4.appendChild(document.createTextNode(" documentation"));
		e3.appendChild(e4);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Values / Enumerations"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Multiplicity"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Voidable / Non-Voidable"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s1");
		e1.appendChild(e2);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Type"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Documentation"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);

		e4 = document.createElementNS(NS_HTML, "B");
		e4.appendChild(document.createTextNode("Attribute\r"));
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#0000FF");
		e5.appendChild(document.createTextNode("Association role\r"));
		e4.appendChild(e5);
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#008080");
		e5.appendChild(document.createTextNode("Constraint"));
		e4.appendChild(e5);
		e3.appendChild(e4);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);

		e4 = document.createElementNS(NS_HTML, "B");
		e4.appendChild(document.createTextNode("Attribute / "));
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#0000FF");
		e5.appendChild(document.createTextNode("Association role"));
		e4.appendChild(e5);
		e4.appendChild(document.createTextNode(" / "));
		e5 = document.createElementNS(NS_HTML, "Font");
		addAttribute(document, e5, "html:Color", "#008080");
		e5.appendChild(document.createTextNode("Constraint"));
		e4.appendChild(e5);
		e4.appendChild(document.createTextNode(" documentation"));
		e3.appendChild(e4);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Values / Enumerations"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Multiplicity"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Voidable / Non-Voidable"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Status"));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode("Remarks"));

	}

	/** Add attribute to an element */
	protected void addAttribute(Document document, Element e, String name,
			String value) {
		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	protected Document createDocument() {
		Document document = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.newDocument();
		} catch (ParserConfigurationException e) {
			result.addFatalError(null, 2);
			String m = e.getMessage();
			if (m != null) {
				result.addFatalError(m);
			}
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (Exception e) {
			result.addFatalError(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}

		return document;
	}

	private void supertypelist(Element e, SortedSet<String> st) {
		for (Iterator<String> i = st.iterator(); i.hasNext();) {
			ClassInfo sti = model.classById(i.next());
			if (sti.isAbstract()) {
				Element e1 = document.createElementNS(NS_HTML, "I");
				e1.appendChild(document.createTextNode("\r" + sti.name()));
				e.appendChild(e1);
			} else {
				e.appendChild(document.createTextNode("\r" + sti.name()));
			}
			SortedSet<String> st2 = sti.supertypes();
			if (st2 != null)
				supertypelist(e, st2);
		}
	}

	public void process(ClassInfo ci) {
		int cat = ci.category();
		if (cat != Options.FEATURE && cat != Options.OBJECT
				&& cat != Options.MIXIN && cat != Options.DATATYPE
				&& cat != Options.BASICTYPE && cat != Options.UNION) {
			return;
		}
		if (ci.isAbstract())
			return;

		Element e1, e2, e3;

		e1 = document.createElementNS(NS_SS, "Row");
		addAttribute(document, e1, "ss:AutoFitHeight", "1");
		addAttribute(document, e1, "ss:Height", "20.0");
		table.appendChild(e1);

		int rows = createAllPropertyDefinitions(pi, ci, true);

		// Loop over all constraints
		List<Constraint> constraints = ci.constraints();
		for (Constraint co : constraints) {
			rows += createConstraintDefinition(pi, ci, co);
		}

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s4");
		addAttribute(document, e2, "ss:MergeDown", "" + rows);
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);

		Element e4;
		e4 = document.createElementNS(NS_HTML, "B");
		e4.appendChild(document.createTextNode(ci.name()));
		e3.appendChild(e4);

		SortedSet<String> st = ci.supertypes();
		if (st != null && !st.isEmpty()) {

			e3.appendChild(document.createTextNode("\r\r"));

			e4 = document.createElementNS(NS_HTML, "Font");
			addAttribute(document, e4, "html:Size", "8.0");
			addAttribute(document, e4, "html:Color", "#808080");
			e4.appendChild(document.createTextNode("Supertypes:"));
			e3.appendChild(e4);
			e4 = document.createElementNS(NS_HTML, "Font");
			addAttribute(document, e4, "html:Size", "8.0");
			addAttribute(document, e4, "html:Color", "#0000FF");
			supertypelist(e4, st);
			e3.appendChild(e4);
		}

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s5");
		addAttribute(document, e2, "ss:MergeDown", "" + rows);
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		String s = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
		e3.appendChild(document.createTextNode(s));

		for (int i = 0; i < 5; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s5");
			e1.appendChild(e2);
		}

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s1");
		e1.appendChild(e2);

		for (int i = 0; i < 9; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s5");
			e1.appendChild(e2);
		}

		e1 = document.createElementNS(NS_SS, "Row");
		addAttribute(document, e1, "ss:Height", "5.0");
		table.appendChild(e1);

		for (int i = 0; i < 7; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s3");
			e1.appendChild(e2);
		}

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s1");
		e1.appendChild(e2);

		for (int i = 0; i < 9; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s3");
			e1.appendChild(e2);
		}

	}

	private int createAllPropertyDefinitions(PackageInfo asi, ClassInfo ci,
			boolean local) {
		if (ci == null)
			return 0;
		if (ci.pkg() == null) {
			result.addError(null, 139, ci.name());
			return 0;
		}

		int ct = 0;

		SortedSet<String> st = ci.supertypes();
		if (st != null) {
			for (Iterator<String> i = st.iterator(); i.hasNext();) {
				ct += createAllPropertyDefinitions(asi,
						model.classById(i.next()), false);
			}
		}
		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j
				.hasNext();) {
			PropertyInfo propi = j.next();
			ct += createPropertyDefinition(asi, ci, propi, local);
		}

		return ct;
	}

	private int createPropertyDefinition(PackageInfo asi, ClassInfo ci,
			PropertyInfo propi, boolean local) {
		if (!propi.isNavigable())
			return 0;
		if (propi.isRestriction())
			return 0;

		Element e1, e2, e3;

		e1 = document.createElementNS(NS_SS, "Row");
		addAttribute(document, e1, "ss:AutoFitHeight", "1");
		addAttribute(document, e1, "ss:Height", "18.0");
		table.appendChild(e1);

		String ao = "";
		if (!propi.isAttribute())
			ao = "1";

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s4" + ao);
		addAttribute(document, e2, "ss:Index", "3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode(propi.name()));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s5" + ao);
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		String s = propi.derivedDocumentation(documentationTemplate, documentationNoValue);
		e3.appendChild(document.createTextNode(s));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s5" + ao);
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		Type ti = propi.typeInfo();
		s = ti.name + "\r";

		ClassInfo ei = model.classById(propi.typeInfo().id);
		int cate = Options.UNKNOWN;
		if (ei != null)
			cate = ei.category();
		if (ei != null && ei.pkg() != null
				&& (cate == Options.ENUMERATION || cate == Options.CODELIST)) {
			for (Iterator<PropertyInfo> k = ei.properties().values()
					.iterator(); k.hasNext();) {
				PropertyInfo vi = k.next();
				s += "\r* " + vi.name();
			}
		}
		e3.appendChild(document.createTextNode(s));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s5" + ao);
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		Multiplicity m = propi.cardinality();
		if (m.minOccurs == m.maxOccurs)
			e3.appendChild(document.createTextNode("" + m.minOccurs));
		else if (m.maxOccurs == Integer.MAX_VALUE)
			e3.appendChild(document.createTextNode("" + m.minOccurs + "..*"));
		else
			e3.appendChild(document
					.createTextNode("" + m.minOccurs + ".." + m.maxOccurs));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s5" + ao);
		e1.appendChild(e2);
		if (propi.voidable()) {
			e3 = document.createElementNS(NS_SS, "Data");
			addAttribute(document, e3, "ss:Type", "String");
			e2.appendChild(e3);
			e3.appendChild(document.createTextNode("voidable"));
		}

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s1");
		e1.appendChild(e2);

		for (int i = 0; i < 9; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s5");
			e1.appendChild(e2);
		}

		return 1;
	}

	private int createConstraintDefinition(PackageInfo asi, ClassInfo ci,
			Constraint co) {
		Element e1, e2, e3;

		e1 = document.createElementNS(NS_SS, "Row");
		addAttribute(document, e1, "ss:AutoFitHeight", "1");
		addAttribute(document, e1, "ss:Height", "18.0");
		table.appendChild(e1);

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s42");
		addAttribute(document, e2, "ss:Index", "3");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode(co.name()));

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s52");
		e1.appendChild(e2);
		e3 = document.createElementNS(NS_SS, "Data");
		addAttribute(document, e3, "ss:Type", "String");
		e2.appendChild(e3);
		String s = co.text();
		e3.appendChild(document.createTextNode(s));

		for (int i = 0; i < 3; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s5");
			e1.appendChild(e2);
		}

		e2 = document.createElementNS(NS_SS, "Cell");
		addAttribute(document, e2, "ss:StyleID", "s1");
		e1.appendChild(e2);

		for (int i = 0; i < 9; i++) {
			e2 = document.createElementNS(NS_SS, "Cell");
			addAttribute(document, e2, "ss:StyleID", "s5");
			e1.appendChild(e2);
		}

		return 1;
	}

	public void write() {
		if (printed)
			return;

		if (diagnosticsOnly)
			return;

		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "no");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"0");
		outputFormat.setProperty("encoding", "UTF-8");

		try {
			File file = new File(
					outputDirectory + "/" + pi.name() + " Mapping Table.xml");
			FileWriter outputXML = new FileWriter(file);
			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);
			serializer.setWriter(outputXML);
			serializer.asDOMSerializer().serialize(document);
			outputXML.close();
			result.addResult(getTargetName(), outputDirectory,
					pi.name() + " Mapping Table.xml", pi.targetNamespace());
		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}
		printed = true;
	}

	@Override
	public String getTargetName() {
		return "Excel Mapping";
	}
}
