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

package de.interactive_instruments.ShapeChange.Util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 * 
 */
public class ZipHandler {

	/**
	 * Unzips the contents of a zip file to a given directory.
	 * 
	 * @param zipfile
	 * @param directory
	 * @throws Exception
	 */
	public void unzip(File zipfile, File directory) throws Exception {
		ZipFile zipFile = new ZipFile(zipfile);
		for (ZipEntry entry : Collections.list(zipFile.entries())) {
			extractEntry(zipFile, entry, directory);
		}
		zipFile.close();
	}

	/**
	 * Creates a zip file containing the files (actual files as well as sub
	 * directories) from a given directory, with relative file paths.
	 * 
	 * @param directoryToZip
	 *            Directory that shall be zipped.
	 * @param toFile
	 *            File to zip to.
	 * @throws IOException
	 */
	public void zip(File directoryToZip, File toFile) throws Exception {

		FileOutputStream fos = new FileOutputStream(toFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		addDirectory(directoryToZip, zos, "");

		zos.close();
		fos.close();
	}

	/**
	 * Creates a zip file containing the given file, with relative file paths.
	 * 
	 * @param fileToZip
	 *            File that shall be zipped.
	 * @param toFile
	 *            File to zip to.
	 * @throws IOException
	 */
	public static void zipFile(File fileToZip, File toFile) throws IOException {

		FileOutputStream fout = new FileOutputStream(toFile);
		ZipOutputStream zout = new ZipOutputStream(fout);

		byte[] tmpBuf = new byte[1024];

		// add an actual file to the zip
		BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(fileToZip));

		zout.putNextEntry(new ZipEntry("" + fileToZip.getName()));

		int len;
		while ((len = bis.read(tmpBuf)) != -1) {
			zout.write(tmpBuf, 0, len);
		}

		zout.closeEntry();
		bis.close();

		zout.close();
		fout.close();
	}

	/**
	 * Extracts an entry (directory or file) from a zip file to the destination
	 * directory.
	 * 
	 * @param zipFile
	 *            The file to unzip.
	 * @param entry
	 *            One specific component (directory or file) from the zip file.
	 * @param destDir
	 *            The destination directory to which the entry is unzipped to.
	 * @throws IOException
	 */
	private void extractEntry(ZipFile zipFile, ZipEntry entry, File destDir)
			throws IOException {

		byte[] buffer = new byte[0xFFFF];

		File file = new File(destDir, entry.getName());

		if (entry.isDirectory()) {
			file.mkdirs();
		} else {
			new File(file.getParent()).mkdirs();

			InputStream is = null;
			OutputStream os = null;

			try {
				is = zipFile.getInputStream(entry);
				os = new FileOutputStream(file);

				for (int len; (len = is.read(buffer)) != -1;) {
					os.write(buffer, 0, len);
				}
			} finally {

				if (os != null)
					os.close();
				if (is != null)
					is.close();

			}
		}
	}

	/**
	 * Adds the files found in a given directory as zip entries to a given zip
	 * output stream. Zip entry names are preceded by the given base path, which
	 * is automatically updated while the method recursively traverses all
	 * subdirectories of the given directory. Empty directories are also added
	 * as zip entries.
	 * 
	 * @param dir
	 *            Directory whose contents shall be added to the zip file
	 * @param zout
	 *            Stream to which new zip entries for the files contained in the
	 *            directory are added.
	 * @param basePathSoFar
	 *            Base path for zip entries, used to keep track of the relative
	 *            location of a file within the zipped directory structure. The
	 *            path is relative, without preceding drive or device letter, or
	 *            a leading slash. Unless the path is the empty string, it ends
	 *            in a forward slash "/". Path separators must be forward
	 *            slashes as opposed to backwards slashes "\" for compatibility
	 *            with Amiga and UNIX file systems etc.
	 * @throws IOException
	 */
	private void addDirectory(File dir, ZipOutputStream zout,
			String basePathSoFar) throws IOException {

		File[] files = dir.listFiles();
		// byte[] tmpBuf = new byte[1024];

		if (files != null) {

			for (File file : files) {

				if (file.isDirectory()) {

					if (file.list() == null || file.list().length == 0) {

						// an empty directory is encoded in a zip with a
						// trailing
						// "/"
						zout.putNextEntry(new ZipEntry(
								basePathSoFar + file.getName() + "/"));
						zout.closeEntry();

					} else {
						addDirectory(file, zout,
								basePathSoFar + file.getName() + "/");
					}

				} else {

					addFile(file, zout, basePathSoFar);
					// // add an actual file to the zip
					// BufferedInputStream bis = new BufferedInputStream(
					// new FileInputStream(file));
					//
					// zout.putNextEntry(
					// new ZipEntry(basePathSoFar + file.getName()));
					//
					// int len;
					// while ((len = bis.read(tmpBuf)) != -1) {
					// zout.write(tmpBuf, 0, len);
					// }
					//
					// zout.closeEntry();
					// bis.close();
				}
			}
		}
	}

	/**
	 * Add the given file to the zip output stream, creating a new entry with
	 * the given base path and name of the file.
	 * 
	 * @param fileToZip
	 * @param zout
	 * @param basePath
	 * @throws IOException
	 */
	private void addFile(File fileToZip, ZipOutputStream zout, String basePath)
			throws IOException {

		byte[] tmpBuf = new byte[1024];

		// add an actual file to the zip
		BufferedInputStream bis = new BufferedInputStream(
				new FileInputStream(fileToZip));

		zout.putNextEntry(new ZipEntry(basePath + fileToZip.getName()));

		int len;
		while ((len = bis.read(tmpBuf)) != -1) {
			zout.write(tmpBuf, 0, len);
		}

		zout.closeEntry();
		bis.close();
	}
}
