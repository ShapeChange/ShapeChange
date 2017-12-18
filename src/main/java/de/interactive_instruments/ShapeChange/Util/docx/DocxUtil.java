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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Util.docx;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.Parts;
import org.docx4j.openpackaging.parts.WordprocessingML.AlternativeFormatInputPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.org.apache.poi.util.IOUtils;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.Br;
import org.docx4j.wml.CTAltChunk;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DocxUtil {

	private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

	/**
	 * Merge two docx files.
	 * 
	 * @param topPackage
	 * @param bottomFile
	 * @param outputFile
	 * @return
	 * @throws Exception
	 */
	public static File merge(WordprocessingMLPackage topPackage,
			File bottomFile, File outputFile) throws Exception {

		/*
		 * For the time being, we use an approach that does not perfectly merge
		 * two docx files. In the future, we could add support for the merge
		 * method provided by the docx4j enterprise version (see
		 * https://github.com/plutext/docx4j/blob/master/src/samples/docx4j/org/
		 * docx4j/samples/MergeDocx.java). If that version was not available on
		 * the classpath, the current approach could be used as fallback.
		 */

		return mergeUsingCTAltChunk(topPackage, bottomFile, outputFile);
	}

	/**
	 * Merge two docx files using an approach that is based on CTAltChunk.
	 * 
	 * @param separatorTexts
	 * @param topFile
	 * @param bottomFile
	 * @param outputFile
	 * @return
	 * @throws Exception
	 */
	protected static File mergeUsingCTAltChunk(
			WordprocessingMLPackage topPackage, File bottomFile,
			File outputFile) throws Exception {

		/*
		 * Based on
		 * https://stackoverflow.com/questions/2494549/is-there-any-java-library
		 * -maybe-poi-which-allows-to-merge-docx-files
		 * 
		 */

		FileInputStream bottomIs = new FileInputStream(bottomFile);

		MainDocumentPart topMainPart = topPackage.getMainDocumentPart();

		// Get binary representation of bottom file
		byte[] bottomAsBytes = IOUtils.toByteArray(bottomIs);

		/*
		 * Determine a suitable name for the new part, one that is not already
		 * taken (in case of multiple merges).
		 */
		Parts docParts = topPackage.getParts();
		Set<PartName> docPartsNames = docParts.getParts().keySet();
		Set<String> plainPartNames = new HashSet<String>();
		for (PartName pn : docPartsNames) {
			plainPartNames.add(pn.getName());
		}

		String partName = null;
		int index = 0;
		do {
			partName = "/part" + index + ".docx";
			index++;
		} while (plainPartNames.contains(partName));

		/*
		 * Now add the bottom file as another part to the top package, and add a
		 * CTAltChunk to the main document of the top package that references
		 * this new part.
		 */

		AlternativeFormatInputPart afiPart = new AlternativeFormatInputPart(
				new PartName(partName));
		afiPart.setContentType(new ContentType(CONTENT_TYPE));
		afiPart.setBinaryData(bottomAsBytes);
		Relationship altChunkRel = topMainPart.addTargetPart(afiPart);

		CTAltChunk chunk = Context.getWmlObjectFactory().createCTAltChunk();
		chunk.setId(altChunkRel.getId());

		topMainPart.addObject(chunk);

		topMainPart.convertAltChunks();

		/*
		 * Finally, save the modified top package to the output file and return
		 * that file.
		 */
		topPackage.save(outputFile);

		return outputFile;
	}

	public static P createPageBreak() {

		org.docx4j.wml.ObjectFactory wmlObjectFactory = new org.docx4j.wml.ObjectFactory();

		// Create object for p
		P p = wmlObjectFactory.createP();
		// Create object for r
		R r = wmlObjectFactory.createR();
		p.getContent().add(r);
		// Create object for br
		Br br = wmlObjectFactory.createBr();
		r.getContent().add(br);
		br.setType(org.docx4j.wml.STBrType.PAGE);

		return p;
	}

	public static P createHorizontalLine() {

		org.docx4j.wml.ObjectFactory wmlObjectFactory = new org.docx4j.wml.ObjectFactory();

		// Create object for p
		P p = wmlObjectFactory.createP();
		// Create object for pPr
		PPr ppr = wmlObjectFactory.createPPr();
		p.setPPr(ppr);
		// Create object for pBdr
		PPrBase.PBdr pprbasepbdr = wmlObjectFactory.createPPrBasePBdr();
		ppr.setPBdr(pprbasepbdr);
		// Create object for bottom
		CTBorder border = wmlObjectFactory.createCTBorder();
		pprbasepbdr.setBottom(border);
		border.setVal(org.docx4j.wml.STBorder.SINGLE);
		border.setSz(BigInteger.valueOf(6));
		border.setColor("auto");
		border.setSpace(BigInteger.valueOf(1));

		return p;
	}

	public static P createText(String text) {

		org.docx4j.wml.ObjectFactory factory = Context.getWmlObjectFactory();

		P p = factory.createP();

		if (text != null) {
			Text t = factory.createText();
			t.setValue(text);

			R run = factory.createR();
			run.getContent().add(t);

			p.getContent().add(run);
		}

		return p;
	}

}
