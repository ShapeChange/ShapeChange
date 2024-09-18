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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.shapechange.core.ocl;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.SortedSet;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.OclConstraint;
import de.interactive_instruments.shapechange.core.model.OperationInfo;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * OclNodes stand for the syntactic constructs of the supported subset of OCL in
 * this package. They form a complete description of the OCL particles and their
 * relation to the UML model.
 * <br><br>
 * OclNode itself is abstract and stands for an OCL expression expressed by
 * roughly in 5 flavors, namely:
 * <ul>
 * <li><i>IfExp</i> - if ... then ... else ... endif
 * <li><i>LetExp</i> - let expression
 * <li><i>LiteralExp</i> - coming along in various flavors
 * <li><i>VariableExp</i> - standing for an actual or implied variable
 * <li><i>PropertyCallExp</i> - selecting an attribute/operation in the UML
 * model or built-in. Concrete flavors are <i>AttributeCallExp</i>,
 * <i>OperationCallExp</i> and <i>IteratorCallExp</i>.
 * </ul>
 * Additionally there is:
 * <ul>
 * <li><i>Expression</i> - which stands for a full blown OCL expression
 * including context and constraint type and name and all, and
 * <li><i>Declaration</i> - a declaration of a variable (may be implicit), which
 * declares and initializes binding variables for <i>Expression</i>,
 * <i>LetExpr</i> and <i>IteratorCallExp</i>.
 * </ul>
 * All OclNodes carry a datatype, which is either built-in or from the UML
 * model.
 * <br><br>
 * Use OclNodes by directly accessing their public fields.
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */

public abstract class OclNode {

    /** Enum describing the implemented Built-in primitive Types */
    public enum BuiltInType {
	REAL, // Floating point numbers
	INTEGER, // Integers
	STRING, // String values
	BOOLEAN, // Logical values
	DATE, // DateTimes, not part of OCL, added for convenience
	CLASS, // Value is a class (not an instance!)
	ENUMERATION, // Value is a property of an enumeration or codelist
	PACKAGE, // Value is a Package
	ANY, // Arbitrary type
	OCLVOID, // OclVoid type with single value "null"
	INVALID, // Invalid type
	VOID, // No-type
	UMLTYPE; // Not built-in, type is UML class

	// Name of enum value in OCL syntax
	String oclName() {
	    String s = name();
	    if (this.equals(OCLVOID))
		return "OclVoid";
	    return s.substring(0, 1) + s.substring(1).toLowerCase();
	}
    }

    // TODO Note this is temporary code we use until ShapeChange can express
    // the required mapping in its configuration file.
    static final HashMap<String, BuiltInType> iso19103Map = new HashMap<String, BuiltInType>();
    static {
	iso19103Map.put("CharacterString", BuiltInType.STRING);
	iso19103Map.put("Character", BuiltInType.STRING);
	iso19103Map.put("Boolean", BuiltInType.BOOLEAN);
	iso19103Map.put("Number", BuiltInType.REAL);
	iso19103Map.put("Real", BuiltInType.REAL);
	iso19103Map.put("Decimal", BuiltInType.REAL);
	iso19103Map.put("Area", BuiltInType.REAL);
	iso19103Map.put("Length", BuiltInType.REAL);
	iso19103Map.put("Measure", BuiltInType.REAL);
	iso19103Map.put("Integer", BuiltInType.INTEGER);
	iso19103Map.put("Date", BuiltInType.DATE);
	iso19103Map.put("DateTime", BuiltInType.DATE);
    }

    // Find out if a UML class or any of its super-classes is in the above
    // HashMap of ISO 19103 class names.
    static public BuiltInType iso19103AssumedBuiltInType(ClassInfo ci) {
	// Find out from the name of the class itself
	BuiltInType bit = iso19103Map.get(ci.name());
	if (bit != null)
	    return bit;
	// If unsuccessful, try super-classes
	SortedSet<String> scids = ci.supertypes();
	if (scids != null) {
	    for (String scid : scids) {
		ClassInfo sci = ci.model().classById(scid);
		if (sci != null) {
		    BuiltInType sbit = iso19103AssumedBuiltInType(sci);
		    if (sbit != null)
			return sbit;
		}
	    }
	}
	// None. Failure ...
	return null;
    }

    // Static tables defining all BuiltIns and operations and their traits

    static public class BuiltInDescr {
	String name;
	BuiltInType applType;
	BuiltInType[] arguTypes;
	int noOfDecls;
	boolean arrow;
	MultiplicityMapping multMap;
	BuiltInType resType;
	int specialTreatment;

	// 1: Result type is instance type of class type
	// 2: Result type is object type
	// 3. Result type is instance type of argument class type
	// 4. Reverse-find a UML type for the built-in if possible
	BuiltInDescr(String name, BuiltInType applType, BuiltInType[] arguTypes, int noOfDecls, boolean arrow,
		MultiplicityMapping multMap, BuiltInType resType) {
	    this(name, applType, arguTypes, noOfDecls, arrow, multMap, resType, 0);
	}

	BuiltInDescr(String name, BuiltInType applType, BuiltInType[] arguTypes, int noOfDecls, boolean arrow,
		MultiplicityMapping multMap, BuiltInType resType, int specialTreatment) {
	    this.name = name;
	    this.applType = applType;
	    this.arguTypes = arguTypes;
	    this.noOfDecls = noOfDecls;
	    this.arrow = arrow;
	    this.multMap = multMap;
	    this.resType = resType;
	    this.specialTreatment = specialTreatment;
	}
    }

    static BuiltInDescr[] builtInDescriptors = {

	    new BuiltInDescr("allInstances", BuiltInType.CLASS, null, 0, false, MultiplicityMapping.ONE2MANY,
		    BuiltInType.UMLTYPE, 1),
	    new BuiltInDescr("size", BuiltInType.ANY, null, 0, true, MultiplicityMapping.MANY2ONE, BuiltInType.INTEGER),
	    new BuiltInDescr("isEmpty", BuiltInType.ANY, null, 0, true, MultiplicityMapping.MANY2ONE,
		    BuiltInType.BOOLEAN),
	    new BuiltInDescr("notEmpty", BuiltInType.ANY, null, 0, true, MultiplicityMapping.MANY2ONE,
		    BuiltInType.BOOLEAN),
	    new BuiltInDescr("exists", BuiltInType.ANY, new BuiltInType[] { BuiltInType.BOOLEAN }, 1, true,
		    MultiplicityMapping.MANY2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("forAll", BuiltInType.ANY, new BuiltInType[] { BuiltInType.BOOLEAN }, 1, true,
		    MultiplicityMapping.MANY2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("isUnique", BuiltInType.ANY, new BuiltInType[] { BuiltInType.ANY }, 1, true,
		    MultiplicityMapping.MANY2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("select", BuiltInType.ANY, new BuiltInType[] { BuiltInType.BOOLEAN }, 1, true,
		    MultiplicityMapping.MANY2MANY, BuiltInType.ANY, 2),
	    new BuiltInDescr("size", BuiltInType.STRING, null, 0, false, MultiplicityMapping.ONE2ONE,
		    BuiltInType.INTEGER),
	    new BuiltInDescr("substring", BuiltInType.STRING,
		    new BuiltInType[] { BuiltInType.INTEGER, BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.STRING, 4),
	    new BuiltInDescr("concat", BuiltInType.STRING, new BuiltInType[] { BuiltInType.STRING }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.STRING, 4),
	    new BuiltInDescr("matches", BuiltInType.STRING, new BuiltInType[] { BuiltInType.STRING }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN, 4),
	    new BuiltInDescr("oclIsKindOf", BuiltInType.ANY, new BuiltInType[] { BuiltInType.CLASS }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("oclIsTypeOf", BuiltInType.ANY, new BuiltInType[] { BuiltInType.CLASS }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("oclAsType", BuiltInType.ANY, new BuiltInType[] { BuiltInType.CLASS }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.ANY, 3),
	    new BuiltInDescr("propertyMetadata", BuiltInType.ANY, null, 0, false, MultiplicityMapping.ONE2ONE,
		    BuiltInType.UMLTYPE, 5) };

    static BuiltInDescr[] operSymbDescriptors = {

	    new BuiltInDescr("or", BuiltInType.BOOLEAN, new BuiltInType[] { BuiltInType.BOOLEAN }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("xor", BuiltInType.BOOLEAN, new BuiltInType[] { BuiltInType.BOOLEAN }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("and", BuiltInType.BOOLEAN, new BuiltInType[] { BuiltInType.BOOLEAN }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("implies", BuiltInType.BOOLEAN, new BuiltInType[] { BuiltInType.BOOLEAN }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("not", BuiltInType.BOOLEAN, null, 0, false, MultiplicityMapping.ONE2ONE,
		    BuiltInType.BOOLEAN),

	    new BuiltInDescr("-", BuiltInType.INTEGER, null, 0, false, MultiplicityMapping.ONE2ONE,
		    BuiltInType.INTEGER),
	    new BuiltInDescr("-", BuiltInType.REAL, null, 0, false, MultiplicityMapping.ONE2ONE, BuiltInType.REAL),

	    new BuiltInDescr("+", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.INTEGER),
	    new BuiltInDescr("+", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("+", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("+", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),

	    new BuiltInDescr("-", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.INTEGER),
	    new BuiltInDescr("-", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("-", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("-", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),

	    new BuiltInDescr("*", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.INTEGER),
	    new BuiltInDescr("*", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("*", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("*", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),

	    new BuiltInDescr("/", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.INTEGER),
	    new BuiltInDescr("/", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("/", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),
	    new BuiltInDescr("/", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.REAL),

	    new BuiltInDescr("=", BuiltInType.ANY, new BuiltInType[] { BuiltInType.ANY }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<>", BuiltInType.ANY, new BuiltInType[] { BuiltInType.ANY }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),

	    new BuiltInDescr("<", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),

	    new BuiltInDescr(">", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),

	    new BuiltInDescr("<=", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<=", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<=", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<=", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),

	    new BuiltInDescr(">=", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">=", BuiltInType.INTEGER, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">=", BuiltInType.REAL, new BuiltInType[] { BuiltInType.INTEGER }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">=", BuiltInType.REAL, new BuiltInType[] { BuiltInType.REAL }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),

	    new BuiltInDescr("<", BuiltInType.DATE, new BuiltInType[] { BuiltInType.DATE }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">", BuiltInType.DATE, new BuiltInType[] { BuiltInType.DATE }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr("<=", BuiltInType.DATE, new BuiltInType[] { BuiltInType.DATE }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN),
	    new BuiltInDescr(">=", BuiltInType.DATE, new BuiltInType[] { BuiltInType.DATE }, 0, false,
		    MultiplicityMapping.ONE2ONE, BuiltInType.BOOLEAN), };

    /**
     * All OclNodes own a DataType, which is either built-in, or from the UML model
     * in the context, or both.
     */
    public static class DataType {

	// Type name
	public String name = null;
	// Built-in type
	public BuiltInType builtInType = BuiltInType.VOID;
	// UML type class linkage
	public ClassInfo umlClass = null;
	public ClassInfo metadataType = null;

	/**
	 * Initialize a DataType from a given UML class represented by a ClassInfo
	 * object.
	 * <br><br>
	 * The Ctor automatically finds out, if the class is one of those which are
	 * mapped to OCL built-in types or any derivatives thereof.
	 * 
	 * @param ci           ClassInfo object
	 * @param metadataType tbd
	 */
	public DataType(ClassInfo ci, ClassInfo metadataType) {
	    this.name = ci.name();
	    this.umlClass = ci;
	    this.metadataType = metadataType;
	    int cat = ci.category();
	    this.builtInType = BuiltInType.UMLTYPE;
	    if (cat == Options.CODELIST || cat == Options.ENUMERATION)
		builtInType = BuiltInType.ENUMERATION;
	    BuiltInType bi = iso19103AssumedBuiltInType(umlClass);
	    if (bi != null)
		builtInType = bi;
	}

	/**
	 * 
	 * Initialize a DataType from an explicit BultInType enum value. This method
	 * always represents a pure OCL type.
	 * 
	 * 
	 * @param bit BuiltInType enum value
	 * @param metadataType  tbd
	 */
	public DataType(BuiltInType bit, ClassInfo metadataType) {
	    this.name = bit.oclName();
	    this.umlClass = null;
	    this.metadataType = metadataType;
	    this.builtInType = bit;
	}

	/**
	 * 
	 * Initialize a DataType from its name alone. This may create a pure OCL type
	 * (if it belongs to the set of OCL Types), or may represent one of the
	 * well-known 19103 types mapped to OCL, or may represent some UML class from
	 * which we only know the name.
	 * 
	 * 
	 * @param name String Name of the type
	 * @param metadataType  tbd
	 */
	public DataType(String name, ClassInfo metadataType) {
	    this.name = name;
	    this.umlClass = null;
	    this.metadataType = metadataType;
	    for (BuiltInType bi : BuiltInType.values()) {
		if (bi.oclName().equals(name)) {
		    builtInType = bi;
		    break;
		}
	    }
	    BuiltInType bi = iso19103Map.get(name);
	    if (bi != null)
		builtInType = bi;
	}

	/**
	 * Find out if this DataType object represents an OCL built-in one, maybe pure
	 * or mapped from 19103.
	 * 
	 * @return is this a built-in type?
	 */
	public boolean isBuiltIn() {
	    return builtInType != BuiltInType.UMLTYPE && builtInType != BuiltInType.ENUMERATION;
	}

	/**
	 * Determine whether this type is a subtype of the given type.
	 * 
	 * @param type Type to be analyzed as indicated
	 * @return Is this type a subtype of the given one?
	 */
	public boolean isSubTypeOf(DataType type) {
	    if (isBuiltIn()) {
		// Check the built-in case ...
		if (!type.isBuiltIn())
		    return false;
		// Built-ins must be identical, except for INTEGER and REAL,
		// which can be promoted to REAL if mixed.
		BuiltInType bit1 = builtInType;
		BuiltInType bit2 = type.builtInType;
		if (bit1 == bit2)
		    return true;
		if (bit1 == BuiltInType.INTEGER && bit2 == BuiltInType.REAL)
		    return true;
		return false;
	    } else {
		// Check the UML case ...
		if (type.isBuiltIn())
		    return false;
		// Cannot compare enum/codelist with normal classes
		BuiltInType bit1 = builtInType;
		BuiltInType bit2 = type.builtInType;
		if (bit1 != bit2)
		    return false;
		// Quick return if of equal type
		if (umlClass == type.umlClass)
		    return true;
		// Check for common supertype ...
		for (ClassInfo ci = umlClass; ci != null; ci = ci.baseClass())
		    if (ci == type.umlClass)
			return true;
		// No, quit.
		return false;
	    }
	}

	/**
	 * Determine common supertype of this type and the one given. If no such type
	 * can be determined null is returned.
	 * 
	 * @param type Type to be analyzed as indicated
	 * @return Common supertype or null
	 */
	public DataType commonSuperType(DataType type) {
	    // If one of the type is compliant to all types it must be the other
	    // one ...
	    if (builtInType == BuiltInType.OCLVOID)
		return type;
	    else if (type.builtInType == BuiltInType.OCLVOID)
		return this;
	    // Checks in particular
	    if (isBuiltIn()) {
		// Check the built-in case ...
		if (!type.isBuiltIn())
		    return null;
		// Built-ins must be identical, except for INTEGER and REAL,
		// which are promoted to REAL if mixed.
		BuiltInType bit1 = builtInType;
		BuiltInType bit2 = type.builtInType;
		if (bit1 == bit2)
		    return this;
		if (bit1 == BuiltInType.INTEGER)
		    bit1 = BuiltInType.REAL;
		if (bit2 == BuiltInType.INTEGER)
		    bit2 = BuiltInType.REAL;
		if (bit1 == bit2)
		    return new DataType(BuiltInType.REAL, null);
		return null;
	    } else {
		// Check the UML case ...
		if (type.isBuiltIn())
		    return null;
		// Cannot compare enum/codelist with normal classes
		BuiltInType bit1 = builtInType;
		BuiltInType bit2 = type.builtInType;
		if (bit1 != bit2)
		    return null;
		// Quick return if of equal type
		if (umlClass == type.umlClass)
		    return this;
		ClassInfo sci = null;
		// Check for common supertype ...
		searchloops: for (ClassInfo ci1 = umlClass; ci1 != null; ci1 = ci1.baseClass()) {
		    for (ClassInfo ci2 = type.umlClass; ci2 != null; ci2 = ci2.baseClass()) {
			if (ci1 == ci2) {
			    sci = ci1;
			    break searchloops;
			}
		    }
		}
		// No, quit.
		if (sci == null)
		    return null;
		// Return the common type.
		return new DataType(sci, null);
	    }
	}
    }

    /** The DataType carried by every OclNode. */
    public DataType dataType = null;

    /**
     * 
     * The method debugPrint outputs the content of an OclNode for the purpose of
     * debugging this software.
     * 
     * 
     * @param stream PrintWriter onto which the debug output is to be directed.
     */
    public void debugPrint(PrintWriter stream) {
	stream.print("[");
	debugPrintContent(stream);
	stream.print(":");
	stream.print(dataType.name);
	stream.print("]");
    }

    /**
     * This is for generating debug output out of OclNodes.
     * 
     * @param stream Writer onto which the debug output is to be directed.
     */
    public void debugPrintContent(PrintWriter stream) {
	stream.print("TODO<");
	stream.print(getClass().getSimpleName());
	stream.print(">");
    }

    /**
     * Inquire the DataType of the OclNode.
     * 
     * @return DataType
     */
    public DataType getDataType() {
	return dataType;
    }

    /**
     * Find out whether this is OclNode is of an OCL built-in datatype.
     * 
     * @return Flag - this is of built-in type.
     */
    public boolean isBuiltInType() {
	return dataType.isBuiltIn();
    }

    /**
     * Find out whether this OclNode represents a multiple entity, such as a Set or
     * other Collection type.
     * 
     * @return Flag - this represents a multiple entity
     */
    public boolean isMultiple() {
	return false;
    }

    /**
     * OclNode.OclExpression wraps a complete OCL expression classifying it as
     * either an invariant or an expression of the derive/init type.
     */
    public static class Expression extends OclNode {

	// Constituents (+Datatype)
	public String name;
	public OclConstraint.ConditionType expressionType;
	public OclNode expression;
	public OclNode.Declaration selfDeclaration;
	public ArrayList<OclNode.Declaration> environmentDeclarations;

	/**
	 * Construct an OclExpression from name, type of expression and the expression
	 * itself.
	 * 
	 * @param name     Name of the expression
	 * @param et       OCL Expression type
	 * @param exp      The expression
	 * @param self     The <i>self</i> declation
	 * @param envDecls Additional environment declarations
	 */
	Expression(String name, OclConstraint.ConditionType et, OclNode exp, OclNode.Declaration self,
		ArrayList<OclNode.Declaration> envDecls) {
	    dataType = exp.getDataType();
	    this.name = name;
	    expressionType = et;
	    expression = exp;
	    selfDeclaration = self;
	    environmentDeclarations = envDecls;
	}

	/**
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(expressionType.toString());
	    stream.print(":");
	    selfDeclaration.debugPrint(stream);
	    stream.print("|");
	    expression.debugPrint(stream);
	}

	/**
	 * Find out whether this OclNode represents a multiple entity, such as a Set or
	 * other Collection type.
	 * 
	 * @return Flag - this represents a multiple entity
	 */
	public boolean isMultiple() {
	    return expression.isMultiple();
	}
    }

    /**
     * 
     * LiteralExp is the common root of all implemented OCL literal expressions. The
     * names of the derived concrete literal classes correspond to the OCL types
     * represented, such as ClassLiteralExp, where the data type is CLASS.
     *
     */
    public static abstract class LiteralExp extends OclNode {

	/**
	 * 
	 * The implementations of this abstract function return the value of the literal
	 * as a String.
	 * 
	 * 
	 * @return The value of the Literal as a String
	 */
	public abstract String asString();
    }

    /**
     * 
     * A RealLiteralExp OclNode represents a real value.
     * 
     */
    public static class RealLiteralExp extends LiteralExp {

	// Constituents
	public double value;

	/**
	 * Initialize.
	 */
	RealLiteralExp(double value) {
	    dataType = new DataType(BuiltInType.REAL, null);
	    this.value = value;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(value);
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return Double.toString(value);
	}
    }

    /**
     * 
     * A IntegerLiteralExp OclNode represents an integer value.
     * 
     */
    public static class IntegerLiteralExp extends LiteralExp {

	// Constituents
	public long value;

	/**
	 * Initialize.
	 */
	IntegerLiteralExp(long value) {
	    dataType = new DataType(BuiltInType.INTEGER, null);
	    this.value = value;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(value);
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return Long.toString(value);
	}
    }

    /**
     * 
     * A StringLiteralExp OclNode represents a String value.
     * 
     */
    public static class StringLiteralExp extends LiteralExp {

	// Constituents
	public String value;

	/**
	 * Initialize.
	 */
	StringLiteralExp(String value) {
	    dataType = new DataType(BuiltInType.STRING, null);
	    this.value = value;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print("'");
	    stream.print(value);
	    stream.print("'");
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return value;
	}
    }

    /**
     * 
     * A BooleanLiteralExp OclNode represents a boolean value.
     * 
     */
    public static class BooleanLiteralExp extends LiteralExp {

	// Constituents
	public boolean value;

	/**
	 * Initialize.
	 */
	BooleanLiteralExp(boolean value) {
	    dataType = new DataType(BuiltInType.BOOLEAN, null);
	    this.value = value;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(value ? "true" : "false");
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return value ? "true" : "false";
	}
    }

    /**
     * 
     * A DateTimeLiteralExp OclNode represents a defined date and time or if
     * constructed without a parameter the current date and time.
     * 
     */
    public static class DateTimeLiteralExp extends LiteralExp {

	// Constituents
	public boolean current;
	public GregorianCalendar dateTime = null;

	/**
	 * Initialize as 'current time'.
	 */
	DateTimeLiteralExp() {
	    dataType = new DataType(BuiltInType.DATE, null);
	    this.current = true;
	}

	/**
	 * Initialize as given calendar time.
	 */
	DateTimeLiteralExp(GregorianCalendar date) {
	    dataType = new DataType(BuiltInType.DATE, null);
	    this.current = false;
	    this.dateTime = date;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(asString());
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    if (current)
		return "*NOW*";
	    else
		return dateTime.getTime().toString();
	}
    }

    /**
     * 
     * A OclVoidLiteralExp OclNode represents the 'null' item, which is of type
     * OclVoid.
     * 
     */
    public static class OclVoidLiteralExp extends LiteralExp {

	/**
	 * Initialize.
	 */
	OclVoidLiteralExp() {
	    dataType = new DataType(BuiltInType.OCLVOID, null);
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print("*NULL*");
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return "*NULL*";
	}
    }

    /**
     * 
     * A ClassLiteralExp OclNode represents a class from the model. Note that the
     * values of a ClassLiteralExp are classes not instances.
     * 
     */
    public static class ClassLiteralExp extends LiteralExp {

	// Constituents
	public ClassInfo umlClass;

	/**
	 * Initialize from the model.
	 */
	ClassLiteralExp(ClassInfo ci) {
	    dataType = new DataType(BuiltInType.CLASS, null);
	    umlClass = ci;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(umlClass.name());
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return umlClass.name();
	}
    }

    /**
     * 
     * A EnumerationLiteralExp represents a property of a enumeration or codelist
     * class from the model. The possible values of such a literal expression are
     * properties of classes (which, in some cases, may not be modeled explicitly).
     * 
     */
    public static class EnumerationLiteralExp extends LiteralExp {

	// Constituents
	/**
	 * WARNING: this may be null if enums/codes are not modeled
	 */
	public PropertyInfo umlProperty;
	public String literalName;

	/**
	 * Initialize from the model.
	 */
	EnumerationLiteralExp(PropertyInfo pi) {
	    dataType = new DataType(pi.inClass(), null);
	    umlProperty = pi;
	    this.literalName = pi.name();
	}

	/**
	 * Initialize with just enumeration class and literal name (for case in which
	 * the literal is not actually modeled as an attribute of the enumeration but is
	 * used in OCL expressions).
	 */
	EnumerationLiteralExp(String literalName, ClassInfo enumeration) {
	    dataType = new DataType(enumeration, null);
	    umlProperty = null;
	    this.literalName = literalName;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream PrintWriter onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print("#");
	    stream.print(literalName);
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return literalName;
	}
    }

    /**
     * 
     * A PackageLiteral OclNode represents a UML package. Note that objects of this
     * class are created only temporarily in the process of resolving qualified
     * names.
     * 
     */
    public static class PackageLiteralExp extends LiteralExp {

	// Constituents
	public PackageInfo umlPackage;

	/**
	 * Initialize from the model.
	 */
	PackageLiteralExp(PackageInfo pi) {
	    dataType = new DataType(BuiltInType.PACKAGE, null);
	    umlPackage = pi;
	}

	/**
	 * Return the contained PackageInfo object.
	 * 
	 * @return Wrapped PackageInfo object
	 */
	PackageInfo getPackage() {
	    return umlPackage;
	}

	/**
	 * This returns the value as a String.
	 * 
	 * @return String value
	 */
	public String asString() {
	    return umlPackage.name();
	}
    }

    /**
     * Objects of this class represent the declaration of variables in some context,
     * such as a let-expression or an iterator reference. Declaration objects are
     * also created implicitly, for example to represent the non-explicitly declared
     * binding variable of an iterator.
     * <br><br>
     * There will always be an implicitly created Declaration object for the
     * <i>self</i> variable, which is attached to the outmost expression scope.
     * Declaration objects have a reference to their surrounding Declaration. By
     * walking this queue all variables explicitly or implicitly in effect for a
     * place in the syntax tree, can always be determined.
     */
    public static class Declaration extends OclNode {

	// Constituents ...
	// dataType is inherited
	public String name = null; // Name of the variable
	public OclNode initialValue = null; // Initial value expression
	public Declaration nextOuter = null; // Next out Declaration in scope
	public OclNode ownerNode = null; // Owner expression construct
	public boolean isImplicit = false; // Implicitly generated?

	/**
	 * Initialize a Declaration from all its constituents.
	 * 
	 * @param name    The name, may be null
	 * @param dt      The DateType of the declaration
	 * @param ival    Initial value or null
	 * @param declCtx Context of next outer declaration
	 * @param owner   The OclNode owning this Declaration (such as a LetExp)
	 * @param impl    Is this implicitly generated?
	 */
	public Declaration(String name, DataType dt, OclNode ival, Declaration declCtx, OclNode owner, boolean impl) {
	    this.name = name != null ? name : "(noname)";
	    dataType = dt;
	    initialValue = ival;
	    nextOuter = declCtx;
	    ownerNode = owner;
	    isImplicit = impl;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print(name);
	}
    }

    /**
     * VariableExp objects stand for an instance of a variable in some expression.
     * The variable is always associated with a single Declaration object, which
     * defines the meaning of a variable.
     * <br><br>
     * Note that all access paths to model contents start with a VariableExp, mostly
     * <i>self</i>.
     */
    public static class VariableExp extends OclNode {

	// Constituents ...
	// dataType is inherited
	public Declaration declaration = null; // The Declaration object

	/**
	 * Initialize a VariableExp from its associated Declaration object.
	 * 
	 * @param decl The Declaration for this variable instance
	 */
	public VariableExp(Declaration decl) {
	    dataType = decl.dataType;
	    declaration = decl;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print("?");
	    stream.print(declaration.name);
	}
    }

    /**
     * 
     * OclNode.IfExp stands for an if-then-else-endif construct. It is ensured that
     * the condition part is of type BOOLEAN and that both decision parts are of
     * compatible types, least abstract supertype of which will be the type of the
     * IfExp.
     * 
     */
    public static class IfExp extends OclNode {

	// Constituents + inherited datatype
	public OclNode condition;
	public OclNode ifExpression;
	public OclNode elseExpression;

	/**
	 * Initialize an IfExp from its constituents.
	 * 
	 * @param type The common type of then and else
	 * @param c    Condition expression, must be BOOLEAN
	 * @param i    If-Expression
	 * @param e    Else-Expression
	 */
	IfExp(DataType type, OclNode c, OclNode i, OclNode e) {
	    dataType = type;
	    condition = c;
	    ifExpression = i;
	    elseExpression = e;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print("if");
	    condition.debugPrint(stream);
	    stream.print("then");
	    ifExpression.debugPrint(stream);
	    stream.print("else ");
	    elseExpression.debugPrint(stream);
	    stream.print("endif");
	}

	/**
	 * 
	 * Find out whether this OclNode represents a multiple entity, such as a Set or
	 * other Collection type.
	 * 
	 * 
	 * @return Flag - this represents a multiple entity
	 */
	public boolean isMultiple() {
	    return ifExpression.isMultiple() || elseExpression.isMultiple();
	}
    }

    /**
     * 
     * OclNode.LetExp represents a let-construct. It is ensured that all contained
     * declarations exhibit a type and a value.
     * 
     */
    public static class LetExp extends OclNode {

	// Expression constituents + inherited datatype
	public OclNode.Declaration[] declarations;
	public OclNode body;

	/**
	 * Initialize an IfExp from its constituents.
	 * 
	 * @param dcls Declarations
	 * @param body Body expression
	 */
	LetExp(OclNode.Declaration[] dcls, OclNode body) {
	    dataType = body.dataType;
	    declarations = dcls;
	    this.body = body;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    stream.print("let");
	    int i = 0;
	    for (Declaration d : declarations) {
		d.debugPrint(stream);
		if (i++ > 0)
		    stream.print(",");
	    }
	    stream.print("|");
	    body.debugPrint(stream);
	}
    }

    /** Property selector categories. */
    public enum PropertyCategory {
	UMLPROPERTY, // Attribute or role in UML model
	UMLOPERATION, // Operation in UML model
	UMLADDOPER, // Additional OCL implied operation on a UML class
	BASEOPER, // OCL Operation on a base type
	SETOPER, // OCL Operation on a collection type
	ITEROPER, // Iterative OCL Operation on a collection type
	CLASSOPER, // OCL Operation on a class
	INVALID // Invalid category
    }

    /**
     * 
     * Objects of class OclNode.PropertySelector stand for selecting and
     * characterizing properties on UML classes, or OCL supplied built-in objects.
     */
    public static class PropertySelector {

	// Property category
	public PropertyCategory category = PropertyCategory.INVALID;
	// Name of property
	public String name = null;
	// If UML attribute/role or operation, reference to PropertyInfo or
	// OperationInfo object
	public Info modelProperty = null;

	/**
	 * Initialize PropertySelector as UML attribute or role
	 * 
	 * @param pi PropertyInfo from model
	 */
	PropertySelector(PropertyInfo pi) {
	    category = PropertyCategory.UMLPROPERTY;
	    modelProperty = pi;
	    name = pi.name();
	}

	/**
	 * Initialize PropertySelector as UML Operation
	 * 
	 * @param oi OperationInfo from model
	 */
	PropertySelector(OperationInfo oi) {
	    category = PropertyCategory.UMLOPERATION;
	    modelProperty = oi;
	    name = oi.name();
	}

	/**
	 * This Ctor initializes any form of non-UML PropertySelector. The Category and
	 * name have to be explicitly specified.
	 * 
	 * @param pc   Property category. Must not be one of the UML-categories.
	 * @param name Name of the property.
	 */
	PropertySelector(PropertyCategory pc, String name) {
	    category = pc;
	    this.name = name;
	}
    }

    /** Mapping characteristic of the applied selector */
    public enum MultiplicityMapping {
	ONE2ONE, // Mapping from one instance to at most one
	ONE2MANY, // Mapping from one instance to zero or more
	MANY2ONE, // Mapping from any number to one instance
	MANY2MANY // Mapping from any number to any number
    }

    /**
     * 
     * The abstract class OclNode.CallExp stands for a selector step, which is
     * applied to some object or set of objects. Syntactically, in OCL this is the
     * construct <i>object.selector</i> or <i>object-&gt;selector</i>, optionally
     * followed by an argument list. Note that the object part can also be implicit,
     * due to a left out <i>self</i> or iterator binding variable.
     * 
     */
    public abstract static class PropertyCallExp extends OclNode {

	// Constituents + inherited dataType
	// Object on which the PropertyCallExp is applied
	public OclNode object;
	// Selector
	public PropertySelector selector;
	// Multiplicity mapping characteristic of the selector
	public MultiplicityMapping multMapping;
	// Implicitly generated CallExp ?
	public boolean isImplicit = false;

	/**
	 * 
	 * Find out whether this OclNode represents a multiple entity, such as a Set or
	 * other Collection type.
	 * 
	 * 
	 * @return Flag - this represents a multiple entity
	 */
	public boolean isMultiple() {
	    return multMapping == MultiplicityMapping.MANY2MANY || multMapping == MultiplicityMapping.ONE2MANY
		    || multMapping == MultiplicityMapping.ONE2ONE && object.isMultiple();
	}
    }

    /**
     * 
     * OclNode.AttributeCallExp stands for an OCL construct of the form
     * <i>object.selector</i>, where the <i>object</i> can be any object or built-in
     * type valued construct and <i>selector</i> selects some specific slot from
     * that resource, typically an attribute name or a role name.
     */
    public static class AttributeCallExp extends PropertyCallExp {

	// Additional constituents (none)

	/**
	 * Initialize AttributeCallExp as UML property category.
	 * 
	 * @param o  Object
	 * @param pi PropertyInfo from the model
	 * @param im true if this in an implicit generation
	 */
	AttributeCallExp(OclNode o, PropertyInfo pi, boolean im) {
	    object = o;
	    de.interactive_instruments.shapechange.core.Type t = pi.typeInfo();
	    ClassInfo ci = pi.model().classById(t.id);
	    ClassInfo metadataType = pi.propertyMetadataType();
	    dataType = ci != null ? new DataType(ci, metadataType) : new DataType(t.name, metadataType);
	    selector = new PropertySelector(pi);
	    isImplicit = im;
	    if (pi.cardinality().maxOccurs <= 1)
		multMapping = MultiplicityMapping.ONE2ONE;
	    else
		multMapping = MultiplicityMapping.ONE2MANY;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    object.debugPrint(stream);
	    stream.print(".");
	    stream.print(selector.name);
	}
    }

    /**
     * 
     * OclNode.OperationCallExp stands for an OCL construct of the form
     * <i>object.selector(arg1,...)</i>, where the <i>object</i> can be any object
     * or built-in type valued construct, <i>selector</i> selects some specific
     * operation from that resource and <i>(arg1,...)</i> stands for a list of zero
     * or more argument expressions.
     */
    public static class OperationCallExp extends PropertyCallExp {

	// Additional constituents
	public OclNode[] arguments = null;

	/**
	 * Initialize OperationCallExp as UML operation category. Note that the
	 * multiplicity mapping cannot be truthfully determined because this is
	 * currently not supported by the model interface.
	 * 
	 * @param o    Object
	 * @param oi   OperationInfo from the model
	 * @param args Array of argument expressions
	 * @param im   true if this in an implicit generation
	 */
	OperationCallExp(OclNode o, OperationInfo oi, OclNode[] args, boolean im) {
	    object = o;
	    SortedMap<Integer, String> pnames = oi.parameterNames();
	    SortedMap<Integer, String> ptypes = oi.parameterTypes();
	    String tname = null;
	    for (int i = 1; i <= pnames.size(); i++) {
		if (pnames.get(i).equals("__RETURN__"))
		    tname = ptypes.get(i);
	    }
	    ClassInfo ci = null;
	    if (tname != null)
		ci = oi.model().classByName(tname);
	    else
		tname = "Void";
	    dataType = ci != null ? new DataType(ci, null) : new DataType(tname, null);
	    selector = new PropertySelector(oi);
	    isImplicit = im;
	    multMapping = MultiplicityMapping.ONE2ONE;
	    arguments = args;
	}

	/**
	 * Initialize OperationCallExp as built-in OCL operation.
	 * 
	 * @param o        Object
	 * @param setLevel Operation is a set operation
	 * @param name     Name of the operation
	 * @param args     The arguments
	 * @param type     Basic DataType of the result
	 * @param mm       Multiplicity mapping characteristic of the operation
	 * @param imp      true if this in an implicit generation
	 */
	OperationCallExp(OclNode o, boolean setLevel, String name, OclNode[] args, DataType type,
		MultiplicityMapping mm, boolean imp) {
	    object = o;
	    dataType = type;
	    if (o instanceof OclNode.ClassLiteralExp)
		selector = new PropertySelector(PropertyCategory.CLASSOPER, name);
	    else if (setLevel) {
		selector = new PropertySelector(PropertyCategory.SETOPER, name);
	    } else if (o.isBuiltInType())
		selector = new PropertySelector(PropertyCategory.BASEOPER, name);
	    else
		selector = new PropertySelector(PropertyCategory.UMLADDOPER, name);
	    multMapping = mm;
	    isImplicit = imp;
	    arguments = args;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    object.debugPrint(stream);
	    if (selector.category == PropertyCategory.SETOPER)
		stream.print("->");
	    else
		stream.print(".");
	    stream.print(selector.name);
	    stream.print("(");
	    int i = 0;
	    for (OclNode n : arguments) {
		n.debugPrint(stream);
		if (i++ > 0)
		    stream.print(",");
	    }
	    stream.print(")");
	}
    }

    /**
     * 
     * OclNode.IterationCallExp stands for an OCL construct of the form
     * <i>object.selector(var1,...|arg1,...)</i>, where the <i>object</i> can be any
     * object or built-in type valued construct, <i>selector</i> selects some
     * specific iterative operation from that resource and
     * <i>(var1,...|arg1,...)</i> stands for a list of up to two variable
     * declarations and zero or more argument expressions.
     */
    public static class IterationCallExp extends OperationCallExp {

	// Additional constituents
	public Declaration[] declarations = null;

	/**
	 * Initialize IterationCallExp as built-in OCL operation.
	 * 
	 * @param o     Object
	 * @param name  Name of the operation
	 * @param decls The declarations
	 * @param args  The arguments
	 * @param type  Basic DataType of the result
	 * @param mm    Multiplicity mapping characteristic of the operation
	 */
	IterationCallExp(OclNode o, String name, Declaration[] decls, OclNode[] args, DataType type,
		MultiplicityMapping mm) {
	    super(o, true, name, args, type, mm, false);
	    selector.category = PropertyCategory.ITEROPER;
	    declarations = decls;
	}

	/**
	 * 
	 * This is for generating debug output out of OclNodes.
	 * 
	 * @param stream Writer onto which the debug output is to be directed.
	 */
	public void debugPrintContent(PrintWriter stream) {
	    object.debugPrint(stream);
	    stream.print("->");
	    stream.print(selector.name);
	    stream.print("(");
	    int i = 0;
	    for (Declaration d : declarations) {
		d.debugPrint(stream);
		if (i++ > 0)
		    stream.print(",");
	    }
	    i = 0;
	    for (OclNode n : arguments) {
		n.debugPrint(stream);
		if (i++ > 0)
		    stream.print(",");
	    }
	    stream.print(")");
	}
    }
}
