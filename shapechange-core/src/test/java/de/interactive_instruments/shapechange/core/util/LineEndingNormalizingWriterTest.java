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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LineEndingNormalizingWriterTest {

    @TempDir
    Path tempDir;

    private static String normalize(String input, String sep) throws IOException {
	StringWriter sw = new StringWriter();
	try (Writer w = new LineEndingNormalizingWriter(sw, sep)) {
	    w.write(input);
	}
	return sw.toString();
    }

    @Test
    void normalizesAllBreakStylesToLf() throws IOException {
	assertEquals("a\nb\nc\n", normalize("a\nb\nc\n", "\n"));
	assertEquals("a\nb\nc\n", normalize("a\r\nb\r\nc\r\n", "\n"));
	assertEquals("a\nb\nc\n", normalize("a\rb\rc\r", "\n"));
	assertEquals("a\nb\nc\n", normalize("a\r\nb\nc\r", "\n"));
    }

    @Test
    void normalizesAllBreakStylesToCrlf() throws IOException {
	assertEquals("a\r\nb\r\n", normalize("a\nb\n", "\r\n"));
	// no doubling: a CR+LF input must not become CR+CR+LF
	assertEquals("a\r\nb\r\n", normalize("a\r\nb\r\n", "\r\n"));
	assertEquals("a\r\nb\r\n", normalize("a\rb\r", "\r\n"));
    }

    @Test
    void consecutiveCarriageReturnsAreSeparateBreaks() throws IOException {
	assertEquals("a\n\nb", normalize("a\r\rb", "\n"));
	// LF immediately followed by CR are two separate breaks
	assertEquals("a\n\nb", normalize("a\n\rb", "\n"));
    }

    @Test
    void trailingCarriageReturnResolvedOnClose() throws IOException {
	assertEquals("a\n", normalize("a\r", "\n"));
    }

    @Test
    void carriageReturnLineFeedSplitAcrossWritesCollapsesToOneBreak() throws IOException {
	StringWriter sw = new StringWriter();
	try (Writer w = new LineEndingNormalizingWriter(sw, "\n")) {
	    w.write("a\r");
	    w.write("\nb");
	}
	assertEquals("a\nb", sw.toString());
    }

    @Test
    void singleCharAndCharArrayWritesAreNormalized() throws IOException {
	StringWriter sw = new StringWriter();
	try (Writer w = new LineEndingNormalizingWriter(sw, "\n")) {
	    for (char c : "x\r\ny".toCharArray()) {
		w.write(c); // exercises write(int)
	    }
	    w.write("\r\nz".toCharArray()); // exercises write(char[])
	}
	assertEquals("x\ny\nz", sw.toString());
    }

    @Test
    void factoryWritesUtf8FileWithNormalizedEndings() throws IOException {
	Path file = tempDir.resolve("out.txt");
	try (Writer w = LineEndingNormalizingWriter.newFileWriter(file, "\n")) {
	    w.write("\u00e4\r\n\u00fc\r"); // non-ASCII to confirm UTF-8 encoding
	}
	assertArrayEquals("\u00e4\n\u00fc\n".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));
    }
}
