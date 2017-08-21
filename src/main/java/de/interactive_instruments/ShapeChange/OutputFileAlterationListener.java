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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class OutputFileAlterationListener implements FileAlterationListener {

	private List<File> newOutputFiles = new ArrayList<File>();

	@Override
	public void onStart(final FileAlterationObserver observer) {
		newOutputFiles = new ArrayList<File>();
	}

	@Override
	public void onDirectoryCreate(final File directory) {
		// ignore
	}

	@Override
	public void onDirectoryChange(final File directory) {
		// ignore
	}

	@Override
	public void onDirectoryDelete(final File directory) {
		// ignore
	}

	@Override
	public void onFileCreate(final File file) {
		this.newOutputFiles.add(file);
	}

	@Override
	public void onFileChange(final File file) {
		this.newOutputFiles.add(file);
	}

	@Override
	public void onFileDelete(final File file) {
		// ignore
	}

	@Override
	public void onStop(final FileAlterationObserver observer) {
		// ignore
	}

	/**
	 * @return list of files (not directories) that were detected as created or
	 *         changed; can be empty but not <code>null</code>
	 */
	public List<File> getNewOutputFiles() {
		return this.newOutputFiles;
	}
}