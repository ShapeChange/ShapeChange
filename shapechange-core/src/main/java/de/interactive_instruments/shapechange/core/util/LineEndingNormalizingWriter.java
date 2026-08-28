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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.util;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link Writer} that normalizes all line endings in the character stream to
 * a configured line separator. Any CR+LF, a lone CR, or a lone LF is written as
 * the configured separator; existing separators are not doubled. A CR at the
 * end of the stream is resolved (written as the separator) on {@link #close()}.
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class LineEndingNormalizingWriter extends FilterWriter {

    private final String lineSeparator;

    /** A CR has been seen whose line break has not yet been emitted. */
    private boolean pendingCR = false;

    /**
     * @param out           the writer to normalize output for
     * @param lineSeparator the line separator to use (e.g. "\n" or "\r\n")
     */
    public LineEndingNormalizingWriter(Writer out, String lineSeparator) {
	super(out);
	this.lineSeparator = lineSeparator;
    }

    /**
     * @param path          file to write to; created or truncated, UTF-8 encoded
     * @param lineSeparator the line separator to use (e.g. "\n" or "\r\n")
     * @return a writer that writes UTF-8 to the file, normalizing line endings
     * @throws IOException if the file cannot be opened for writing
     */
    public static Writer newFileWriter(Path path, String lineSeparator) throws IOException {
	return new LineEndingNormalizingWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), lineSeparator);
    }

    private void normalize(char c) throws IOException {
	if (c == '\r') {
	    if (pendingCR) {
		// the previous CR was a lone CR line break
		out.write(lineSeparator);
	    }
	    pendingCR = true;
	} else if (c == '\n') {
	    // handles both a lone LF and the LF of a CR+LF pair
	    out.write(lineSeparator);
	    pendingCR = false;
	} else {
	    if (pendingCR) {
		out.write(lineSeparator);
		pendingCR = false;
	    }
	    out.write(c);
	}
    }

    @Override
    public void write(int c) throws IOException {
	normalize((char) c);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
	for (int i = 0; i < len; i++) {
	    normalize(cbuf[off + i]);
	}
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
	for (int i = 0; i < len; i++) {
	    normalize(str.charAt(off + i));
	}
    }

    @Override
    public void close() throws IOException {
	if (pendingCR) {
	    out.write(lineSeparator);
	    pendingCR = false;
	}
	super.close();
    }
}
