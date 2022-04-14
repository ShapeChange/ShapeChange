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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.helpers.AttributesImpl;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement2;
import de.interactive_instruments.ShapeChange.Util.XMLWriter;
import de.interactive_instruments.ShapeChange.Util.ZipHandler;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ModelDiffWriter extends AbstractModelWriter {

    protected List<DiffElement2> diffs = null;
    protected ModelWriter modelWriter = null;

    public ModelDiffWriter(Options o, ShapeChangeResult r, String encoding, File outputXmlFile, boolean zipOutput,
	    String schemaLocation, boolean profilesInModelSetExplicitly,
	    SortedSet<String> defaultProfilesForClassesWithoutExplicitProfiles, List<DiffElement2> diffs) {

	super(o, r, encoding, outputXmlFile, zipOutput, schemaLocation);

	this.diffs = diffs;

	try {

	    OutputStream fout = new FileOutputStream(outputXmlFile);
	    OutputStream bout = new BufferedOutputStream(fout, streamBufferSize);
	    OutputStreamWriter outputXML = new OutputStreamWriter(bout, this.encoding);

	    this.writer = new XMLWriter(outputXML, this.encoding);

	    this.modelWriter = new ModelWriter(writer, o, r, null, true, null, true, true, true,
		    profilesInModelSetExplicitly, defaultProfilesForClassesWithoutExplicitProfiles, true);

	} catch (Exception e) {

	    String msg = e.getMessage();
	    if (msg != null) {
		result.addError(msg);
	    }
	    e.printStackTrace(System.err);
	}

    }

    public void write(Model sourceModel, Model targetModel, boolean includeData, boolean printModelElementPaths) {

	try {
	    writer.forceNSDecl("http://www.w3.org/2001/XMLSchema-instance", "xsi");
	    writer.forceNSDecl(NS, "sc");
	    writer.startDocument();

	    AttributesImpl atts = new AttributesImpl();
	    atts.addAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "xsi:schemaLocation",
		    "CDATA", NS + " " + schemaLocation);

	    String scversion = "[dev]";
	    String scunittesting = System.getProperty("scunittesting");
	    if ("true".equalsIgnoreCase(scunittesting)) {
		scversion = "unittest";
	    } else {
		InputStream stream = getClass().getResourceAsStream("/sc.properties");
		if (stream != null) {
		    Properties properties = new Properties();
		    properties.load(stream);
		    scversion = properties.getProperty("sc.version");
		}
	    }
	    atts.addAttribute("", "diffProducer", "", "string", "ShapeChange");
	    atts.addAttribute("", "diffProducerVersion", "", "string", scversion);

	    writer.startElement(NS, "ModelDiff", "", atts);

	    if (includeData) {
		writer.startElement(NS, "sourceModel");
		modelWriter.printModel(sourceModel, new AttributesImpl());
		writer.endElement(NS, "sourceModel");
	    }

	    if (includeData) {
		writer.startElement(NS, "targetModel");
		modelWriter.printModel(targetModel, new AttributesImpl());
		writer.endElement(NS, "targetModel");
	    }

	    writer.startElement(NS, "diffs");

	    for (DiffElement2 elmt : diffs) {

		writer.startElement(NS, "DiffElement");

		if (elmt.sourceInfo != null) {
		    if (includeData) {
			printDataElement("sourceId", elmt.sourceInfo.id());
		    }
		    if (printModelElementPaths) {
			printDataElement("sourceSchemaPath", elmt.sourceInfo.fullNameInSchema());
		    }
		}
		if (elmt.targetInfo != null) {
		    if (includeData) {
			printDataElement("targetId", elmt.targetInfo.id());
		    }
		    if (printModelElementPaths) {
			printDataElement("targetSchemaPath", elmt.targetInfo.fullNameInSchema());
		    }
		}

		printDataElement("change", elmt.change.toString());
		printDataElement("elementChangeType", elmt.elementChangeType.toString());
		if (elmt.subElement != null) {
		    if (includeData) {
			printDataElement("subElementId", elmt.subElement.id());
		    }
		    if (printModelElementPaths) {
			printDataElement("subElementSchemaPath", elmt.subElement.fullNameInSchema());
		    }
		}
		if (StringUtils.isNotBlank(elmt.tag)) {
		    printDataElement("tag", elmt.tag);
		}
		if (elmt.diff != null && !elmt.diff.isEmpty()) {
		    writer.dataElement(NS, "from", elmt.diff_from());
		    writer.dataElement(NS, "to", elmt.diff_to());
		}

		writer.endElement(NS, "DiffElement");
	    }

	    writer.endElement(NS, "diffs");

	    writer.endElement(NS, "ModelDiff");

	    writer.endDocument();
	    writer.close();

	    if (zipOutput) {

		File outputZipFile = new File(outputXmlFile.getParentFile(), outputXmlFile.getName() + ".zip");
		ZipHandler.zipFile(outputXmlFile, outputZipFile);
	    }

	} catch (Exception e) {

	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }
	    e.printStackTrace(System.err);

	} finally {

	    // close writer
	    if (writer != null) {
		try {
		    writer.close();
		} catch (IOException e) {
		    String m = e.getMessage();
		    if (m != null) {
			result.addError(m);
		    }
		    e.printStackTrace(System.err);
		}
	    }
	}
    }
}
