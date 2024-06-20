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
package de.interactive_instruments.ShapeChange;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class ConstraintConverterTest extends BasicTestSCXML {

    @Test
    public void test_geometryRestrictionToGeometryTV() {

	/*
	 * Test rule-trf-cls-constraints-geometryRestrictionToGeometryTV-inclusion
	 */
	multiTest(
		"src/test/resources/constraintConverter/geometryRestrictionToGeometryTV/testEA_ConstraintConverter_geometryRestrictionToGeometryTV_inclusion.xml",
		new String[] { "xsd" }, "testResults/constraintConverter/geometryRestrictionToGeometryTV/inclusion",
		"src/test/resources/constraintConverter/geometryRestrictionToGeometryTV/reference/inclusion");

	/*
	 * Test rule-trf-cls-constraints-geometryRestrictionToGeometryTV-exclusion
	 */
	multiTest(
		"src/test/resources/constraintConverter/geometryRestrictionToGeometryTV/testEA_ConstraintConverter_geometryRestrictionToGeometryTV_exclusion.xml",
		new String[] { "xsd" }, "testResults/constraintConverter/geometryRestrictionToGeometryTV/exclusion",
		"src/test/resources/constraintConverter/geometryRestrictionToGeometryTV/reference/exclusion");
    }

    @Test
    public void test_valueTypeRestrictionToTV() {

	multiTest(
		"src/test/resources/constraintConverter/valueTypeRestrictionToTV/testEA_ConstraintConverter_valueTypeRestrictionToTV.xml",
		new String[] { "xsd" }, "testResults/constraintConverter/valueTypeRestrictionToTV",
		"src/test/resources/constraintConverter/valueTypeRestrictionToTV/reference");
    }
}