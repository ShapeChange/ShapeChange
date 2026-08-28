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

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An {@link OutputStream} that normalizes all line endings in the byte stream
 * to a configured line separator. Any CR+LF, a lone CR, or a lone LF byte
 * sequence is written as the configured separator; existing separators are not
 * doubled. A CR at the end of the stream is resolved (written as the separator)
 * on {@link #close()}.
 * <p>
 * This is the byte-level counterpart of {@link LineEndingNormalizingWriter}. It
 * is safe for UTF-8 and any ASCII-superset encoding, because the newline bytes
 * (0x0D / 0x0A) never occur inside a multi-byte character in such encodings. It
 * must not be used for encodings where that does not hold (e.g. UTF-16).
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class LineEndingNormalizingOutputStream extends FilterOutputStream {

    private final byte[] lineSeparator;

    /** A CR has been seen whose line break has not yet been emitted. */
    private boolean pendingCR = false;

    /**
     * @param out           the stream to normalize output for
     * @param lineSeparator the line separator to use (e.g. "\n" or "\r\n"); must be
     *                      ASCII
     */
    public LineEndingNormalizingOutputStream(OutputStream out, String lineSeparator) {
	this(out, lineSeparator.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * @param out           the stream to normalize output for
     * @param lineSeparator the line separator bytes to use
     */
    public LineEndingNormalizingOutputStream(OutputStream out, byte[] lineSeparator) {
	super(out);
	this.lineSeparator = lineSeparator.clone();
    }

    /**
     * @param path          file to write to; created or truncated
     * @param lineSeparator the line separator to use (e.g. "\n" or "\r\n")
     * @return a buffered stream that writes to the file, normalizing line endings
     * @throws IOException if the file cannot be opened for writing
     */
    public static OutputStream newFileOutputStream(Path path, String lineSeparator) throws IOException {
	return new LineEndingNormalizingOutputStream(new BufferedOutputStream(Files.newOutputStream(path)),
		lineSeparator);
    }

    @Override
    public void write(int b) throws IOException {
	if (b == '\r') {
	    if (pendingCR) {
		// the previous CR was a lone CR line break
		out.write(lineSeparator);
	    }
	    pendingCR = true;
	} else if (b == '\n') {
	    // handles both a lone LF and the LF of a CR+LF pair
	    out.write(lineSeparator);
	    pendingCR = false;
	} else {
	    if (pendingCR) {
		out.write(lineSeparator);
		pendingCR = false;
	    }
	    out.write(b);
	}
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
	for (int i = 0; i < len; i++) {
	    write(b[off + i]);
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
