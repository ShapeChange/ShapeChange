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
package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class TypeConverterTest extends WindowsBasicTest {

    @Test
    public void testRuleTrfEnumerationToCodelist() {
	/*
	 * Test rule-trf-enumeration-to-codelist of TypeConverter transformation
	 */
	multiTest(
		"src/test/resources/typeConverter/enumerationToCodelist/testEA_typeConverter_enumerationToCodelist.xml",
		new String[] { "xsd" }, "testResults/typeConversion/enumerationToCodelist",
		"src/test/resources/typeConverter/enumerationToCodelist/reference");
    }

    @Test
    public void testRuleTrfDissolveAssociations() {
	/*
	 * Test rule-trf-dissolveAssociations of TypeConverter transformation
	 */
	multiTest("src/test/resources/typeConverter/dissolveAssociations/testEA_typeConverter_dissolveAssociations.xml",
		new String[] { "xsd" }, "testResults/typeConversion/dissolveAssociations",
		"src/test/resources/typeConverter/dissolveAssociations/reference");
    }

    @Test
    public void testRuleTrfTargetElementByReference() {
	/*
	 * Test rule-trf-dissolveAssociations of TypeConverter transformation for the
	 * case that: -> tagged value inlineOrByReference is set to byReference on the
	 * navigable association end of the association to be dissolved -> the type
	 * referred to in parameter attributeType is a type present in the model
	 */
	multiTest(
		"src/test/resources/typeConverter/targetElementByReference/testEA_typeConverter_targetElementByReference.xml",
		new String[] { "xsd" }, "testResults/typeConversion/targetElementByReference",
		"src/test/resources/typeConverter/targetElementByReference/reference");
    }
}