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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.app;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class SchematronTest extends BasicTestSCXML {

	@Test
	public void schematronTestCodelist2() {
		multiTest("src/integrationtests/sch/codelist2/testEA_sch_codelist2.xml",
				new String[] { "xsd","sch" }, "testResults/sch/codelist2",
				"src/integrationtests/sch/codelist2/reference");
	}
	
	@Test
	public void schematronTestCodelist2_subtypes() {
		multiTest("src/integrationtests/sch/codelist2_subtypes/testEA_sch_codelist2_subtypes.xml",
				new String[] { "sch" }, "testResults/sch/codelist2_subtypes",
				"src/integrationtests/sch/codelist2_subtypes/reference");
	}
	
	@Test
	public void schematronTestMetadataProfile() {
		multiTest("src/integrationtests/sch/metadataProfile/testEA_sch_metadataProfile.xml",
				new String[] { "sch", "xsd" }, "testResults/sch/metadataProfile",
				"src/integrationtests/sch/metadataProfile/reference");
	}
	
	@Test
	public void schematronTestOclOnCodelistTypedProperty() {
		multiTest("src/integrationtests/sch/oclOnCodelistTypedProperty/testEA_sch_oclOnCodelistTypedProperty.xml",
				new String[] { "sch" }, "testResults/sch/oclOnCodelistTypedProperty",
				"src/integrationtests/sch/oclOnCodelistTypedProperty/reference");
	}
	
	@Test
	public void schematronTestOclPathEncoding() {
		multiTest("src/integrationtests/sch/oclPathEncoding/testEA_sch_oclPathEncoding.xml",
				new String[] { "sch" }, "testResults/sch/oclPathEncoding",
				"src/integrationtests/sch/oclPathEncoding/reference");
	}
	
	@Test
	public void schematronTestOclIsTypeOf() {
		multiTest("src/integrationtests/sch/oclIsTypeOf/testEA_sch_oclIsTypeOf.xml",
				new String[] { "sch" }, "testResults/sch/oclIsTypeOf",
				"src/integrationtests/sch/oclIsTypeOf/reference");
	}
	
	@Test
	public void schematronTestCodeListRestrictionFromOCL() {
		multiTest("src/integrationtests/sch/codelist2_codeListRestrictionFromOCL/testEA_sch_codelist2_codeListRestrictionFromOCL.xml",
				new String[] { "sch" }, "testResults/sch/codelist2_codeListRestrictionFromOCL",
				"src/integrationtests/sch/codelist2_codeListRestrictionFromOCL/reference");
	}
	
	@Test
	public void schematronTest_iterator_with_byReference_property() {
		multiTest("src/integrationtests/sch/iterator_with_byReference_property/testEA_sch_iterator_with_byReference_property.xml",
				new String[] { "sch" }, "testResults/sch/iterator_with_byReference_property",
				"src/integrationtests/sch/iterator_with_byReference_property/reference");
	}
	
	@Test
	public void schematronTest_oclConstraintOnProperties() {
		multiTest("src/integrationtests/sch/oclConstraintOnProperties/testEA_sch_oclConstraintOnProperties.xml",
				new String[] { "sch", "xml" }, "testResults/sch/oclConstraintOnProperties/results",
				"src/integrationtests/sch/oclConstraintOnProperties/reference/results");
	}
	
	@Test
	public void schematronTest_oclPropertyMetadata() {
		multiTest("src/integrationtests/sch/oclPropertyMetadata/testEA_sch_oclPropertyMetadata.xml",
				new String[] { "sch" }, "testResults/sch/oclPropertyMetadata/results",
				"src/integrationtests/sch/oclPropertyMetadata/reference/results");
	}
	
	@Test
	public void schematronTest_xslt2QueryBinding() {
		multiTest("src/integrationtests/sch/xslt2QueryBinding/testEA_sch_xslt2QueryBinding.xml",
				new String[] { "sch" }, "testResults/sch/xslt2QueryBinding/results",
				"src/integrationtests/sch/xslt2QueryBinding/reference/results");
	}
	
	@Test
	public void schematronTest_valueOrNilReason() {
		multiTest("src/integrationtests/sch/valueOrNilReason/testEA_sch_valueOrNilReason.xml",
				new String[] { "xsd","sch" }, "testResults/sch/valueOrNilReason",
				"src/integrationtests/sch/valueOrNilReason/reference");
	}
	
	@Test
	public void schematronTest_segmentation() {
		multiTest("src/integrationtests/sch/segmentation/testEA_sch_segmentation.xml",
				new String[] { "xsd","sch" }, "testResults/sch/segmentation",
				"src/integrationtests/sch/segmentation/reference");
	}
}
