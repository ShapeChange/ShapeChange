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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Model;

import java.io.File;

public class ImageMetadata {

	private String id;
	private String name;
	private String documentation;
	private File file;
	private String relPathToFile;
	private int width;
	private int height;

	public ImageMetadata(String id, String name, String documentation, File file,
			String relPathToFile, int width, int height) {
		super();
		this.id = id;
		this.name = name;
		this.documentation = documentation;
		this.file = file;
		this.relPathToFile = relPathToFile;
		this.width = width;
		this.height = height;
	}

	/**
	 * @return the imgId
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the imgId to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the imgName
	 */
	public String getName() {
		return name;
	}
	
	

	/**
	 * @param name
	 *            the imgName to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the relPathToFile
	 */
	public String getRelPathToFile() {
		return relPathToFile;
	}

	/**
	 * @param relPathToFile
	 *            the relPathToFile to set
	 */
	public void setRelPathToFile(String relPathToFile) {
		this.relPathToFile = relPathToFile;
	}

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @param width
	 *            the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @param height
	 *            the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("img id: ");
		sb.append(id);
		sb.append("; img name: ");
		sb.append(name);
		sb.append("; width: ");
		sb.append(width);
		sb.append("; height: ");
		sb.append(height);
		sb.append("; relPathToFile: ");
		sb.append(relPathToFile);
		sb.append("; img documentation: ");
		sb.append(documentation);

		return sb.toString();
	}

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * @return the documentation
	 */
	public String getDocumentation() {
	    return documentation;
	}

	/**
	 * @param documentation the documentation to set; can be <code>null</code>
	 */
	public void setDocumentation(String documentation) {
	    this.documentation = documentation;
	}
}
