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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class InputConfiguration {

	/**
	 * Default defined in ShapeChangeConfiguration XSD
	 */
	private String id;
	/**
	 * key: name, value: value
	 */
	private Map<String, String> parametersByName;
	/**
	 * key: alias (lower case), value: wellknown (lower case)
	 */
	private Map<String, String> stereotypeAliasesByAlias;
	private Map<String, String> tagAliasesByAlias;
	private SortedMap<String, String> descriptorSources = new TreeMap<String, String>();

	/**
	 * key: packageName, value: package info
	 */
	private Map<String, PackageInfoConfiguration> packageInfosByName;

	/**
	 * @param id
	 * @param parametersByName
	 * @param stereotypeAliasesByAlias
	 *            map with stereotype alias (in lower case) as key, and with
	 *            wellknown stereotype (in lower case) as value
	 * @param descriptorSources, can be <code>null</code> or empty
	 * @param tagAliases
	 *            map with tag alias (in lower case) as key, and with wellknown
	 *            tag (in lower case) as value
	 * @param packageInfosByName
	 */
	public InputConfiguration(String id, Map<String, String> parametersByName,
			Map<String, String> stereotypeAliasesByAlias,
			Map<String, String> tagAliasesByAlias,
			Map<String, String> descriptorSources,
			Map<String, PackageInfoConfiguration> packageInfosByName) {
		super();
		this.id = id;
		this.parametersByName = parametersByName;
		this.stereotypeAliasesByAlias = stereotypeAliasesByAlias;
		this.tagAliasesByAlias = tagAliasesByAlias;
		if(descriptorSources != null) {
			this.descriptorSources.putAll(descriptorSources);
		}
		this.packageInfosByName = packageInfosByName;
	}

	public String getId() {
		return id;
	}

	public Map<String, String> getParameters() {
		return parametersByName;
	}

	public boolean hasParameter(String paramName) {
		if (parametersByName != null
				&& parametersByName.containsKey(paramName)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return map with key: alias (lower case), value: wellknown (lower case)
	 */
	public Map<String, String> getStereotypeAliases() {
		return stereotypeAliasesByAlias;
	}

	/**
	 * @return map with key: alias (lower case), value: wellknown (lower case)
	 */
	public Map<String, String> getTagAliases() {
		return tagAliasesByAlias;
	}

	/**
	 * @return map (can be empty but not <code>null</code>) with key: descriptor
	 *         (lower case), value: source (lower case)
	 */
	public SortedMap<String, String> getDescriptorSources() {
		return descriptorSources;
	}

	public Map<String, PackageInfoConfiguration> getPackageInfos() {
		return packageInfosByName;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("InputConfiguration:\r\n");

		sb.append("\tid: " + this.id + "\r\n");

		sb.append("\tpackage infos: ");
		if (this.packageInfosByName == null || packageInfosByName.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : packageInfosByName.keySet()) {
				PackageInfoConfiguration pi = packageInfosByName.get(key);
				sb.append("\t\tPackageInfo (" + key);
				if (pi.getNsabr() != null) {
					sb.append(pi.getNsabr() + "|");
				} else {
					sb.append("no nsabr|");
				}
				if (pi.getNs() != null) {
					sb.append(pi.getNs() + "|");
				} else {
					sb.append("no ns|");
				}
				if (pi.getXsdDocument() != null) {
					sb.append(pi.getXsdDocument() + "|");
				} else {
					sb.append("no xsdDocument|");
				}
				if (pi.getVersion() != null) {
					sb.append(pi.getVersion());
				} else {
					sb.append("no version");
				}
				sb.append(")\r\n");

			}
		}

		sb.append("\tparameters: ");
		if (this.parametersByName == null || parametersByName.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : parametersByName.keySet()) {
				sb.append("\t\t(" + key + " | " + parametersByName.get(key)
						+ ")\r\n");
			}
		}

		sb.append("\tstereotypealiases: ");
		if (this.stereotypeAliasesByAlias == null
				|| stereotypeAliasesByAlias.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : stereotypeAliasesByAlias.keySet()) {
				sb.append("\t\t(" + key + " | "
						+ stereotypeAliasesByAlias.get(key) + ")\r\n");
			}
		}

		sb.append("\ttagaliases: ");
		if (this.tagAliasesByAlias == null || tagAliasesByAlias.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : tagAliasesByAlias.keySet()) {
				sb.append("\t\t(" + key + " | " + tagAliasesByAlias.get(key)
						+ ")\r\n");
			}
		}

		sb.append("\tdescriptorsources: ");
		if (this.descriptorSources.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : descriptorSources.keySet()) {
				sb.append("\t\t(" + key + " | " + descriptorSources.get(key)
						+ ")\r\n");
			}
		}

		return sb.toString();
	}

}
