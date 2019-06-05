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

package de.interactive_instruments.ShapeChange.Model.EA;

import java.util.List;
import java.util.TreeMap;

import org.sparx.Collection;
import org.sparx.Method;
import org.sparx.MethodTag;
import org.sparx.Parameter;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OperationInfo;
import de.interactive_instruments.ShapeChange.Model.OperationInfoImpl;
import de.interactive_instruments.ShapeChange.Model.StereotypeNormalizer;

public class OperationInfoEA extends OperationInfoImpl
		implements OperationInfo {

	/**
	 * Flag used to prevent duplicate retrieval/computation of the alias of this
	 * property.
	 */
	protected boolean aliasAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the
	 * globalIdentifier of this class.
	 */
	protected boolean globalIdentifierAccessed = false;

	/** Access to the document object */
	protected EADocument document = null;

	/** Class the operation belongs to */
	protected ClassInfoEA classInfo = null;

	/**
	 * Model-unique id. This is the ID from EA Methods, prefixed by the class
	 * ID.
	 */
	protected String eaOperationId = null;

	/** Name of the operation */
	protected String eaName = null;

	/** Type information */
	protected Type typeInfo = new Type();
	protected ClassInfoEA typeClassInfo = null;

	/** EA method object */
	protected Method eaMethod = null;

	/** Cache set for stereotypes */
	// this map is already defined in InfoImpl

	/** Cache map for tagged values */
	// this map is already defined in InfoImpl

	/** Cache map for Parameters */
	protected Collection<Parameter> eaParametersCache = null;

	/** Create an OperationInfo object given an EA Method. */
	public OperationInfoEA(EADocument doc, ClassInfoEA ci, Method meth) {

		// Record references ...
		document = doc;
		classInfo = ci;
		eaMethod = meth;

		// The Id
		eaOperationId = ci.id();
		eaOperationId += "_M";
		eaOperationId += String.valueOf(eaMethod.GetMethodID());

		// Property name
		eaName = eaMethod.GetName();
		if (eaName != null)
			eaName = eaName.trim();
	} // OperationInfoEA()

	// Validate parameter cache of the operation.
	private void validateParametersCache() {
		if (eaParametersCache == null) {
			eaParametersCache = eaMethod.GetParameters();
		}
	} // validateParametersCache()

	/** Return the total number of parameters including __RETURN__ */
	public int parameterCount() {
		validateParametersCache();
		if (eaParametersCache == null)
			return 0;
		else
			return eaParametersCache.GetCount() + 1;
	} // parameterCount()

	/**
	 * Obtain the names all parameters of the operation. They will appear
	 * ordered as in the method definition. The return value (if any) appears in
	 * the last position and the receives the name __RETURN__.
	 */
	public TreeMap<Integer, String> parameterNames() {
		validateParametersCache();
		int count = 0;
		TreeMap<Integer, String> parms = new TreeMap<Integer, String>();
		if (eaParametersCache != null) {
			for (Parameter p : eaParametersCache) {
				String name = p.GetName();
				// "return" not observed ...
				// String kind = p.GetKind();
				// if(kind.equals("return"))
				// name = "__RETURN__";
				parms.put(++count, name);
			}
			parms.put(++count, "__RETURN__");
		}
		return parms;
	}

	/**
	 * Obtain the types of all parameters of the operation. Types will appear
	 * ordered as in the method definition. The type of the return value (if
	 * any) appears in the last position.
	 */
	public TreeMap<Integer, String> parameterTypes() {
		validateParametersCache();
		int count = 0;
		TreeMap<Integer, String> parms = new TreeMap<Integer, String>();
		if (eaParametersCache != null) {
			for (Parameter p : eaParametersCache) {
				String type = p.GetType();
				parms.put(++count, type);
			}
			String ret = eaMethod.GetReturnType();
			parms.put(++count, ret);
		}
		return parms;
	}

	/** Return model-unique id of operation. */
	public String id() {
		return eaOperationId;
	} // id()

	/** Return EA model object. */
	public Model model() {
		return document;
	} // model()

	/** Obtain the name of the property. */
	public String name() {
		// Get the name obtained from the model
		return eaName;
	} // name()

	// @Override
	// public Descriptors aliasNameAll() {
	//
	// // Retrieve/compute the alias only once
	// // Cache the result for subsequent use
	// if (!aliasAccessed) {
	//
	// aliasAccessed = true;
	//
	// // Obtain alias name from default implementation
	// Descriptors ls = super.aliasNameAll();
	//
	// // If not present, obtain from EA model directly
	// if (ls.isEmpty()) {
	//
	// String a = eaMethod.GetStyle();
	//
	// if (a != null && !a.isEmpty()) {
	//
	// super.aliasName = new Descriptors(
	// new LangString(options().internalize(a)));
	// } else {
	// super.aliasName = new Descriptors();
	// }
	// }
	// }
	// return super.aliasName;
	// }

	/** Return options and configuration object. */
	public Options options() {
		return document.options;
	} // options()

	/** Return result object for error reporting. */
	public ShapeChangeResult result() {
		return document.result;
	} // result()

	// Validate stereotypes cache of the property. The stereotypes found are 1.
	// restricted to those defined within ShapeChange and 2. deprecated ones
	// are normalized to the lastest definitions.
	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			// Fetch stereotypes 'collection' ...
			String sts;
			sts = eaMethod.GetStereotypeEx();
			String[] stereotypes = sts.split("\\,");
			// Allocate cache
			stereotypesCache = StereotypeNormalizer
					.normalizeAndMapToWellKnownStereotype(stereotypes, this);
		}
	} // validateStereotypesCache()

	// Validate tagged values cache, filtering on tagged values defined within
	// ShapeChange ...
	public void validateTaggedValuesCache() {
		if (taggedValuesCache == null) {
			// Fetch tagged values collection
			Collection<MethodTag> tvs = eaMethod.GetTaggedValues();

			// ensure that there are tagged values
			if (tvs != null) {

				// Allocate cache
				int ntvs = tvs.GetCount();
				taggedValuesCache = options().taggedValueFactory(ntvs);
				// Copy tag-value-pairs, leave out non-ShapeChange stuff and
				// normalize deprecated tags.
				for (MethodTag tv : tvs) {
					String t = tv.GetName();
					t = options().taggedValueNormalizer()
							.normalizeTaggedValue(t);
					if (t != null) {
						String v = tv.GetValue();
						if (v.equals("<memo>"))
							v = tv.GetNotes();
						taggedValuesCache.add(t, v);
					}
				}
			} else {
				taggedValuesCache = options().taggedValueFactory(0);
			}
		}
	} // validateTaggedValuesCache()

	// @Override
	// public Descriptors globalIdentifierAll() {
	//
	// // Obtain global identifier from default implementation
	// Descriptors ls = super.globalIdentifierAll();
	//
	// // If not present, obtain from EA model directly
	// if (ls.isEmpty()
	// && descriptorSource(Descriptor.GLOBALIDENTIFIER)
	// .equals("ea:guidtoxml")) {
	//
	// String gi = document.repository.GetProjectInterface()
	// .GUIDtoXML(eaMethod.GetMethodGUID());
	//
	// super.globalIdentifier = new Descriptors(
	// new LangString(options().internalize(gi)));
	// }
	// return super.globalIdentifier;
	// }

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!globalIdentifierAccessed
					&& descriptor == Descriptor.GLOBALIDENTIFIER) {

				globalIdentifierAccessed = true;

				// obtain from EA model directly
				if (model().descriptorSource(Descriptor.GLOBALIDENTIFIER)
						.equals("ea:guidtoxml")) {

					String gi = document.repository.GetProjectInterface()
							.GUIDtoXML(eaMethod.GetMethodGUID());

					if (gi != null && !gi.isEmpty()) {
						ls.add(new LangString(options().internalize(gi)));
						this.descriptors().put(descriptor, ls);
					}
				}

			} else if (!aliasAccessed && descriptor == Descriptor.ALIAS) {

				aliasAccessed = true;

				/*
				 * obtain from EA model directly if ea:alias is identified as
				 * the source
				 */
				if (model().descriptorSource(Descriptor.ALIAS)
						.equals("ea:alias")) {

					String a = eaMethod.GetStyle();

					if (a != null && !a.isEmpty()) {
						ls.add(new LangString(options().internalize(a)));
						this.descriptors().put(descriptor, ls);
					}
				}
			}

		}

		return ls;
	}
}
