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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LineEndingNormalizingOutputStreamTest {

    @TempDir
    Path tempDir;

    private static byte[] bytes(String s) {
	return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] normalize(byte[] input, String sep) throws IOException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	try (OutputStream os = new LineEndingNormalizingOutputStream(bos, sep)) {
	    os.write(input);
	}
	return bos.toByteArray();
    }

    @Test
    void normalizesAllBreakStylesToLf() throws IOException {
	assertArrayEquals(bytes("a\nb\nc\n"), normalize(bytes("a\r\nb\rc\n"), "\n"));
    }

    @Test
    void normalizesToCrlfWithoutDoubling() throws IOException {
	assertArrayEquals(bytes("a\r\nb\r\n"), normalize(bytes("a\r\nb\n"), "\r\n"));
    }

    @Test
    void trailingCarriageReturnResolvedOnClose() throws IOException {
	assertArrayEquals(bytes("a\n"), normalize(bytes("a\r"), "\n"));
    }

    @Test
    void carriageReturnLineFeedSplitAcrossWritesCollapsesToOneBreak() throws IOException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	try (OutputStream os = new LineEndingNormalizingOutputStream(bos, "\n")) {
	    os.write(bytes("a\r"));
	    os.write(bytes("\nb"));
	}
	assertArrayEquals(bytes("a\nb"), bos.toByteArray());
    }

    @Test
    void multibyteUtf8BytesArePreserved() throws IOException {
	// ä = 0xC3 0xA4, ü = 0xC3 0xBC; none of these bytes is a newline byte
	assertArrayEquals(bytes("\u00e4\n\u00fc\n"), normalize(bytes("\u00e4\r\n\u00fc\r"), "\n"));
    }

    @Test
    void factoryWritesFileWithNormalizedEndings() throws IOException {
	Path file = tempDir.resolve("out.bin");
	try (OutputStream os = LineEndingNormalizingOutputStream.newFileOutputStream(file, "\r\n")) {
	    os.write(bytes("a\nb\n"));
	}
	assertArrayEquals(bytes("a\r\nb\r\n"), Files.readAllBytes(file));
    }
}
