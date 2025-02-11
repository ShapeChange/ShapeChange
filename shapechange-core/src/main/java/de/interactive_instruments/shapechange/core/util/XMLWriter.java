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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;
import org.xml.sax.helpers.XMLFilterImpl;

import de.interactive_instruments.shapechange.core.modeldiff.DiffElement.Operation;

/**
 * This class was adapted from the XMLWriter and DataWriter classes written by
 * David Megginson, which are in the public domain.
 * <p>
 * Changes:
 * <ul>
 * <li>XML processing instruction at the start of the document now indicates the
 * encoding.</li>
 * <li>Used java generics where applicable</li>
 * <li>Using System.getProperty("line.separator") for creation of new
 * lines.</li>
 * <li>Applying general formatting now, with indentation and new line creation.
 * </li>
 * <li>Removed special treatment of "ch[i] &gt; '\u007f'" when writing characters.
 * Correct encoding should be handled by the writer.</li>
 * </ul>
 * <p>
 * The following documentation from the XMLWriter class still applies:
 * <p>
 * =====================
 * <p>
 * Filter to write an XML document from a SAX event stream.
 * 
 * <p>
 * This class can be used by itself or as part of a SAX event stream: it takes
 * as input a series of SAX2 ContentHandler events and uses the information in
 * those events to write an XML document. Since this class is a filter, it can
 * also pass the events on down a filter chain for further processing (you can
 * use the XMLWriter to take a snapshot of the current state at any point in a
 * filter chain), and it can be used directly as a ContentHandler for a SAX2
 * XMLReader.
 * </p>
 * 
 * <p>
 * The client creates a document by invoking the methods for standard SAX2
 * events, always beginning with the {@link #startDocument startDocument} method
 * and ending with the {@link #endDocument endDocument} method. There are
 * convenience methods provided so that clients do not have to create empty
 * attribute lists or provide empty strings as parameters; for example, the
 * method invocation
 * </p>
 * 
 * <pre>
 * w.startElement(&quot;foo&quot;);
 * </pre>
 * 
 * <p>
 * is equivalent to the regular SAX2 ContentHandler method
 * </p>
 * 
 * <pre>
 * w.startElement(&quot;&quot;, &quot;foo&quot;, &quot;&quot;, new AttributesImpl());
 * </pre>
 * 
 * <p>
 * Except that it is more efficient because it does not allocate a new empty
 * attribute list each time.
 * </p>
 * 
 * <h2>Namespace Support</h2>
 * 
 * <p>
 * The writer contains extensive support for XML Namespaces, so that a client
 * application does not have to keep track of prefixes and supply
 * <var>xmlns</var> attributes. By default, the XML writer will generate
 * Namespace declarations in the form _NS1, _NS2, etc., wherever they are
 * needed.
 * </p>
 * 
 * <p>
 * In many cases, document authors will prefer to choose their own prefixes
 * rather than using the (ugly) default names. The XML writer allows two methods
 * for selecting prefixes:
 * </p>
 * 
 * <ol>
 * <li>the qualified name</li>
 * <li>the {@link #setPrefix setPrefix} method.</li>
 * </ol>
 * 
 * <p>
 * Whenever the XML writer finds a new Namespace URI, it checks to see if a
 * qualified (prefixed) name is also available; if so it attempts to use the
 * name's prefix (as long as the prefix is not already in use for another
 * Namespace URI).
 * </p>
 * 
 * <p>
 * Before writing a document, the client can also pre-map a prefix to a
 * Namespace URI with the setPrefix method.
 * </p>
 * 
 * <p>
 * The default Namespace simply uses an empty string as the prefix.
 * </p>
 * 
 * <p>
 * By default, the XML writer will not declare a Namespace until it is actually
 * used. Sometimes, this approach will create a large number of Namespace
 * declarations, as in the following example:
 * </p>
 * 
 * <pre>
 * &lt;xml version="1.0" standalone="yes"?&gt;
 * 
 * &lt;rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"&gt;
 *  &lt;rdf:Description about="http://www.foo.com/ids/books/12345"&gt;
 *   &lt;dc:title xmlns:dc="http://www.purl.org/dc/"&gt;A Dark Night&lt;/dc:title&gt;
 *   &lt;dc:creator xmlns:dc="http://www.purl.org/dc/"&gt;Jane Smith&lt;/dc:title&gt;
 *   &lt;dc:date xmlns:dc="http://www.purl.org/dc/"&gt;2000-09-09&lt;/dc:title&gt;
 *  &lt;/rdf:Description&gt;
 * &lt;/rdf:RDF&gt;
 * </pre>
 * 
 * <p>
 * The "rdf" prefix is declared only once, because the RDF Namespace is used by
 * the root element and can be inherited by all of its descendants; the "dc"
 * prefix, on the other hand, is declared three times, because no higher element
 * uses the Namespace. To solve this problem, you can instruct the XML writer to
 * predeclare Namespaces on the root element even if they are not used there:
 * </p>
 * 
 * <pre>
 * w.forceNSDecl(&quot;http://www.purl.org/dc/&quot;);
 * </pre>
 * 
 * <p>
 * Now, the "dc" prefix will be declared on the root element even though it's
 * not needed there, and can be inherited by its descendants:
 * </p>
 * 
 * <pre>
 * &lt;xml version="1.0" standalone="yes"?&gt;
 * 
 * &lt;rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
 *             xmlns:dc="http://www.purl.org/dc/"&gt;
 *  &lt;rdf:Description about="http://www.foo.com/ids/books/12345"&gt;
 *   &lt;dc:title&gt;A Dark Night&lt;/dc:title&gt;
 *   &lt;dc:creator&gt;Jane Smith&lt;/dc:title&gt;
 *   &lt;dc:date&gt;2000-09-09&lt;/dc:title&gt;
 *  &lt;/rdf:Description&gt;
 * &lt;/rdf:RDF&gt;
 * </pre>
 * 
 * <p>
 * This approach is also useful for declaring Namespace prefixes that be used by
 * qualified names appearing in attribute values or character data.
 * </p>
 * 
 * @author Johannes Echterhoff
 * 
 * @see org.xml.sax.ContentHandler
 */
public class XMLWriter extends XMLFilterImpl {

    private enum State {
	SEEN_NOTHING, SEEN_DATA, SEEN_ELEMENT;
    }

    private State state = State.SEEN_NOTHING;
    private Stack<State> stateStack = new Stack<State>();
    private boolean lastElementWasStart = false;
    // private boolean wrapWithCDATA = false;

    private int indentStep = 2;
    private int depth = 0;

    private final Attributes EMPTY_ATTS = new AttributesImpl();

    private Hashtable<String, String> prefixTable;
    private Hashtable<String, Boolean> forcedDeclTable;
    private Hashtable<String, String> doneDeclTable;

    protected Writer output;
    private NamespaceSupport nsSupport;
    private int prefixCounter = 0;

    private String encoding;
    private String LINESEPARATOR = System.getProperty("line.separator");

    private StringBuilder sb;

    /**
     * Create a new XML writer.
     * 
     * <p>
     * Write to the writer provided. The writer must already be configured to use
     * the correct character encoding.
     * </p>
     * 
     * @param writer   The output destination, or null to use standard output.
     * @param encoding Content to be placed in the "encoding" attribute of the xml
     *                 processing instruction that will be written at the start of
     *                 the document. Defaults to "UTF-8".
     */
    public XMLWriter(Writer writer, String encoding) {
	super();
	init(writer, encoding);
    }

    /**
     * Create a new XML writer.
     * 
     * <p>
     * Use the specified XML reader as the parent, and write to the specified
     * writer. The writer must already be configured to use the correct character
     * encoding.
     * </p>
     * 
     * @param xmlreader The parent in the filter chain, or null for no parent.
     * @param writer    The output destination, or null to use standard output.
     * @param encoding  Content to be placed in the "encoding" attribute of the xml
     *                  processing instruction that will be written at the start of
     *                  the document. Defaults to "UTF-8".
     */
    public XMLWriter(XMLReader xmlreader, Writer writer, String encoding) {
	super(xmlreader);
	init(writer, encoding);
    }

    /**
     * Return the current indent step.
     * 
     * <p>
     * Return the current indent step: each start tag will be indented by this
     * number of spaces times the number of ancestors that the element has.
     * </p>
     * 
     * @return The number of spaces in each indentation step, or 0 or less for no
     *         indentation.
     * @see #setIndentStep
     */
    public int getIndentStep() {
	return indentStep;
    }

    /**
     * Set the current indent step.
     * 
     * @param indentStep The new indent step (0 or less for no indentation).
     * @see #getIndentStep
     */
    public void setIndentStep(int indentStep) {
	this.indentStep = indentStep;
    }

    /**
     * Internal initialization method.
     * 
     * <p>
     * All of the public constructors invoke this method.
     * 
     * @param writer   The output destination, or null to use standard output.
     * @param encoding Content to be placed in the "encoding" attribute of the xml
     *                 processing instruction that will be written at the start of
     *                 the document. Defaults to "UTF-8".
     */
    private void init(Writer writer, String encoding) {
	setOutput(writer);
	this.encoding = (encoding != null) ? encoding : "UTF-8";

	nsSupport = new NamespaceSupport();
	prefixTable = new Hashtable<String, String>();
	forcedDeclTable = new Hashtable<String, Boolean>();
	doneDeclTable = new Hashtable<String, String>();
    }

    /**
     * Reset the writer.
     * 
     * <p>
     * This method is especially useful if the writer throws an exception before it
     * is finished, and you want to reuse the writer for a new document. It is
     * usually a good idea to invoke {@link #flush flush} before resetting the
     * writer, to make sure that no output is lost.
     * </p>
     * 
     * <p>
     * This method is invoked automatically by the {@link #startDocument
     * startDocument} method before writing a new document.
     * </p>
     * 
     * <p>
     * <strong>Note:</strong> this method will <em>not</em> clear the prefix or URI
     * information in the writer or the selected output writer.
     * </p>
     * 
     * @see #flush
     */
    public void reset() {
	prefixCounter = 0;
	nsSupport.reset();
	depth = 0;
	state = State.SEEN_NOTHING;
	stateStack = new Stack<State>();
    }

    /**
     * Flush the output.
     * 
     * <p>
     * This method flushes the output stream. It is especially useful when you need
     * to make certain that the entire document has been written to output but do
     * not want to close the output stream.
     * </p>
     * 
     * <p>
     * This method is invoked automatically by the {@link #endDocument endDocument}
     * method after writing a document.
     * </p>
     * @throws IOException  tbd
     * 
     * @see #reset
     */
    public void flush() throws IOException {
	output.flush();
    }

    public void close() throws IOException {
	output.close();
    }

    /**
     * Set the output destination for the document.
     * 
     * @param writer The output destination, or null to use standard output.
     * @see #flush
     * 
     */
    protected void setOutput(Writer writer) {
	if (writer == null) {
	    output = new OutputStreamWriter(System.out);
	} else {
	    output = writer;
	}
    }

    /**
     * Specify a preferred prefix for a Namespace URI.
     * 
     * <p>
     * Note that this method does not actually force the Namespace to be declared;
     * to do that, use the {@link #forceNSDecl(java.lang.String) forceNSDecl} method
     * as well.
     * </p>
     * 
     * @param uri    The Namespace URI.
     * @param prefix The preferred prefix, or "" to select the default Namespace.
     * @see #getPrefix
     * @see #forceNSDecl(java.lang.String)
     * @see #forceNSDecl(java.lang.String,java.lang.String)
     */
    public void setPrefix(String uri, String prefix) {
	prefixTable.put(uri, prefix);
    }

    /**
     * Get the current or preferred prefix for a Namespace URI.
     * 
     * @param uri The Namespace URI.
     * @return The preferred prefix, or "" for the default Namespace.
     * @see #setPrefix
     */
    public String getPrefix(String uri) {
	return prefixTable.get(uri);
    }

    /**
     * Force a Namespace to be declared on the root element.
     * 
     * <p>
     * By default, the XMLWriter will declare only the Namespaces needed for an
     * element; as a result, a Namespace may be declared many places in a document
     * if it is not used on the root element.
     * </p>
     * 
     * <p>
     * This method forces a Namespace to be declared on the root element even if it
     * is not used there, and reduces the number of xmlns attributes in the
     * document.
     * </p>
     * 
     * @param uri The Namespace URI to declare.
     * @see #forceNSDecl(java.lang.String,java.lang.String)
     * @see #setPrefix
     */
    public void forceNSDecl(String uri) {
	forcedDeclTable.put(uri, Boolean.TRUE);
    }

    /**
     * Force a Namespace declaration with a preferred prefix.
     * <p>
     * This is a convenience method that invokes {@link #setPrefix setPrefix} then
     * {@link #forceNSDecl(java.lang.String) forceNSDecl}.
     * </p>
     * 
     * @param uri    The Namespace URI to declare on the root element.
     * @param prefix The preferred prefix for the Namespace, or "" for the default
     *               Namespace.
     * @see #setPrefix
     * @see #forceNSDecl(java.lang.String)
     */
    public void forceNSDecl(String uri, String prefix) {
	setPrefix(uri, prefix);
	forceNSDecl(uri);
    }

    /**
     * Writes the XML declaration at the beginning of the document.
     * <p>
     * Passes the event on down the filter chain for further processing.
     * 
     * @exception org.xml.sax.SAXException If there is an error writing the XML
     *                                     declaration, or if a handler further down
     *                                     the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#startDocument
     */
    public void startDocument() throws SAXException {

	reset();

	write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
	newLine();

	super.startDocument();
    }

    /**
     * Write a newline at the end of the document.
     * 
     * Pass the event on down the filter chain for further processing.
     * 
     * @exception org.xml.sax.SAXException If there is an error writing the newline,
     *                                     or if a handler further down the filter
     *                                     chain raises an exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void endDocument() throws SAXException {

	super.endDocument();

	try {
	    flush();
	} catch (IOException e) {
	    throw new SAXException(e);
	}
    }

    /**
     * Write a start tag.
     * 
     * Pass the event on down the filter chain for further processing.
     * 
     * @param uri       The Namespace URI, or the empty string if none is available.
     * @param localName The element's local (unprefixed) name (required).
     * @param qName     The element's qualified (prefixed) name, or the empty string
     *                  if none is available. This method will use the qName as a
     *                  template for generating a prefix if necessary, but it is not
     *                  guaranteed to use the same qName.
     * @param atts      The element's attribute list (must not be null).
     * @exception org.xml.sax.SAXException If there is an error writing the start
     *                                     tag, or if a handler further down the
     *                                     filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

	sb = new StringBuilder();

	stateStack.push(State.SEEN_ELEMENT);
	state = State.SEEN_NOTHING;
	lastElementWasStart = true;

	if (depth > 0) {
	    newLine();
	}
	doIndent();

	depth++;

	nsSupport.pushContext();

	write('<');
	writeName(uri, localName, qName, true);
	writeAttributes(atts);

	if (depth == 1) {
	    forceNSDecls();
	}

	writeNSDecls();
	write('>');

	super.startElement(uri, localName, qName, atts);
    }

    /**
     * Convenience method: start an element with only local name and a single
     * attribute.
     * 
     * @param localName tbd
     * @param attributeName tbd
     * @param attributeValue tbd
     * @throws SAXException tbd
     */
    public void startElement(String localName, String attributeName, String attributeValue) throws SAXException {

	startElement("", localName, attributeName, attributeValue);
    }

    /**
     * Convenience method: start an element with a single attribute.
     * 
     * @param uri tbd
     * @param localName tbd
     * @param attributeName tbd
     * @param attributeValue tbd
     * @throws SAXException tbd
     */
    public void startElement(String uri, String localName, String attributeName, String attributeValue)
	    throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	startElement(uri, localName, "", atts);
    }

    /**
     * Convenience method: start an element with only local name and a single
     * attribute. Optionally, add information about the operation.
     * 
     * @param localName tbd
     * @param attributeName tbd
     * @param attributeValue tbd
     * @param op tbd
     * @throws SAXException tbd
     */
    public void startElement(String localName, String attributeName, String attributeValue, Operation op)
	    throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	if (op != null) {
	    atts.addAttribute("", "mode", "", "CDATA", op.toString());
	}

	startElement("", localName, "", atts);
    }

    private void newLine() throws SAXException {

	char[] tmp = LINESEPARATOR.toCharArray();

	// writeEsc(tmp, 0, tmp.length, false);
	write(LINESEPARATOR);

	super.characters(tmp, 0, tmp.length);
    }

    /**
     * Writes and emits character content reported for the element. Then writes the
     * end tag.
     * <p>
     * Passes the endElement event on down the filter chain for further processing.
     * 
     * @param uri       The Namespace URI, or the empty string if none is available.
     * @param localName The element's local (unprefixed) name (required).
     * @param qName     The element's qualified (prefixed) name, or the empty string
     *                  is none is available. This method will use the qName as a
     *                  template for generating a prefix if necessary, but it is not
     *                  guaranteed to use the same qName.
     * @exception org.xml.sax.SAXException If there is an error writing the end tag,
     *                                     or if a handler further down the filter
     *                                     chain raises an exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {

	depth--;

	if (State.SEEN_ELEMENT.equals(state)) {

	    newLine();
	    doIndent();

	} else if (lastElementWasStart && State.SEEN_DATA.equals(state)) {

	    String tmp = sb.toString();
	    // String tmp2 = tmp.trim();
	    // char[] newChars = tmp2.toCharArray();
	    char[] newChars = tmp.toCharArray();

	    writeEsc(newChars, 0, newChars.length, false);

	    // if (wrapWithCDATA) {
	    // write("]]>");
	    // wrapWithCDATA = false;
	    // }

	    super.characters(newChars, 0, newChars.length);
	}

	write("</");
	writeName(uri, localName, qName, true);
	write('>');

	state = stateStack.pop();
	nsSupport.popContext();
	lastElementWasStart = false;

	super.endElement(uri, localName, qName);
    }

    /**
     * If we are inside an element, append the character data to an internal buffer.
     * <p>
     * Ignore character data that is between end and start elements (e.g. line
     * feeds).
     * <p>
     * Only character data of element content is ultimately passed on down the
     * filter chain for further processing (with leading and trailing whitespace
     * trimmed).
     * 
     * @param ch     The array of characters to write.
     * @param start  The starting position in the array.
     * @param len The number of characters to write.
     * @exception org.xml.sax.SAXException If there is an error writing the
     *                                     characters, or if a handler further down
     *                                     the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#characters
     */
    public void characters(char ch[], int start, int len) throws SAXException {

	if (lastElementWasStart) {
	    state = State.SEEN_DATA;

	    // we need to aggregate the whole string, as it may be reported in
	    // chunks
	    sb.append(ch, start, len);
	} else {
	    /*
	     * We ignore character data between end and start element (e.g. line feeds)
	     * because we format the XML ourselves.
	     */
	}

	/*
	 * StringBuilder content is written/emitted in endElement method.
	 */
    }

    /**
     * Print indentation for the current level.
     * <p>
     * Also passes this whitespaces on down the filter chain (via the
     * {@link ContentHandler#characters(char[], int, int)} method).
     * 
     * @exception org.xml.sax.SAXException If there is an error writing the
     *                                     indentation characters, or if a filter
     *                                     further down the chain raises an
     *                                     exception.
     */
    private void doIndent() throws SAXException {

	if (indentStep > 0 && depth > 0) {

	    int n = indentStep * depth;
	    char ch[] = new char[n];

	    for (int i = 0; i < n; i++) {
		ch[i] = ' ';
	    }

	    writeEsc(ch, 0, ch.length, false);

	    super.characters(ch, 0, ch.length);
	}
    }

    /**
     * Ignorable whitespace is NOT written.
     * <p>
     * However, the event is passed on down the filter chain for further processing.
     * 
     * @param ch     The array of characters to write.
     * @param start  The starting position in the array.
     * @param length The number of characters to write.
     * @exception org.xml.sax.SAXException If there is an error writing the
     *                                     whitespace, or if a handler further down
     *                                     the filter chain raises an exception.
     * @see org.xml.sax.ContentHandler#ignorableWhitespace
     */
    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {

	// writeEsc(ch, start, length, false);

	super.ignorableWhitespace(ch, start, length);
    }

    /**
     * Writes a processing instruction.
     * <p>
     * Passes the event on down the filter chain for further processing.
     * 
     * @param target The PI target.
     * @param data   The PI data.
     * @exception org.xml.sax.SAXException If there is an error writing the PI, or
     *                                     if a handler further down the filter
     *                                     chain raises an exception.
     * @see org.xml.sax.ContentHandler#processingInstruction
     */
    public void processingInstruction(String target, String data) throws SAXException {

	write("<?");
	write(target);
	write(' ');
	write(data);
	write("?>");

	if (depth < 1) {
	    newLine();
	}

	super.processingInstruction(target, data);
    }

    /**
     * Convenience mechanism to write a comment. The comment is not passed on (no
     * support for LexicalHandlers).
     * 
     * @param comment tbd
     * @throws SAXException tbd
     */
    public void comment(String comment) throws SAXException {

	if (depth > 0) {
	    newLine();
	}
	doIndent();

	write("<!--");
	write(comment);
	write("-->");

	newLine();
    }

    /**
     * Start a new element without a qname or attributes.
     * <p>
     * This method will provide a default empty attribute list and an empty string
     * for the qualified name. It invokes
     * {@link #startElement(String, String, String, Attributes)} directly.
     * </p>
     * 
     * @param uri       The element's Namespace URI.
     * @param localName The element's local name.
     * @exception org.xml.sax.SAXException If there is an error writing the start
     *                                     tag, or if a handler further down the
     *                                     filter chain raises an exception.
     * @see #startElement(String, String, String, Attributes)
     */
    public void startElement(String uri, String localName) throws SAXException {
	startElement(uri, localName, "", EMPTY_ATTS);
    }

    /**
     * Start a new element without a qname, attributes or a Namespace URI.
     * <p>
     * This method will provide an empty string for the Namespace URI, and empty
     * string for the qualified name, and a default empty attribute list. It invokes
     * #startElement(String, String, String, Attributes)} directly.
     * </p>
     * 
     * @param localName The element's local name.
     * @exception org.xml.sax.SAXException If there is an error writing the start
     *                                     tag, or if a handler further down the
     *                                     filter chain raises an exception.
     * @see #startElement(String, String, String, Attributes)
     */
    public void startElement(String localName) throws SAXException {
	startElement("", localName, "", EMPTY_ATTS);
    }
    
    public void startElement(String localName, Attributes atts) throws SAXException {
	startElement("", localName, "", atts);
    }

    /**
     * End an element without a qname.
     * <p>
     * This method will supply an empty string for the qName. It invokes
     * {@link #endElement(String, String, String)} directly.
     * </p>
     * 
     * @param uri       The element's Namespace URI.
     * @param localName The element's local name.
     * @exception org.xml.sax.SAXException If there is an error writing the end tag,
     *                                     or if a handler further down the filter
     *                                     chain raises an exception.
     * @see #endElement(String, String, String)
     */
    public void endElement(String uri, String localName) throws SAXException {
	endElement(uri, localName, "");
    }

    /**
     * End an element without a Namespace URI or qname.
     * <p>
     * This method will supply an empty string for the qName and an empty string for
     * the Namespace URI. It invokes {@link #endElement(String, String, String)}
     * directly.
     * </p>
     * 
     * @param localName The element's local name.
     * @exception org.xml.sax.SAXException If there is an error writing the end tag,
     *                                     or if a handler further down the filter
     *                                     chain raises an exception.
     * @see #endElement(String, String, String)
     */
    public void endElement(String localName) throws SAXException {
	endElement("", localName, "");
    }

    /**
     * Force all Namespaces to be declared.
     * <p>
     * This method is used on the root element to ensure that the predeclared
     * Namespaces all appear.
     */
    private void forceNSDecls() {

	Enumeration<String> prefixes = forcedDeclTable.keys();

	while (prefixes.hasMoreElements()) {
	    String prefix = prefixes.nextElement();
	    doPrefix(prefix, null, true);
	}
    }

    /**
     * Determine the prefix for an element or attribute name.
     * 
     * TODO: this method probably needs some cleanup.
     * 
     * @param uri       The Namespace URI.
     * @param qName     The qualified name (optional); this will be used to indicate
     *                  the preferred prefix if none is currently bound.
     * @param isElement true if this is an element name, false if it is an attribute
     *                  name (which cannot use the default Namespace).
     */
    private String doPrefix(String uri, String qName, boolean isElement) {

	String defaultNS = nsSupport.getURI("");

	if ("".equals(uri)) {
	    if (isElement && defaultNS != null)
		nsSupport.declarePrefix("", "");
	    return null;
	}

	String prefix;

	if (isElement && defaultNS != null && uri.equals(defaultNS)) {
	    prefix = "";
	} else {
	    prefix = nsSupport.getPrefix(uri);
	}

	if (prefix != null) {
	    return prefix;
	}

	prefix = doneDeclTable.get(uri);

	if (prefix != null
		&& ((!isElement || defaultNS != null) && "".equals(prefix) || nsSupport.getURI(prefix) != null)) {
	    prefix = null;
	}

	if (prefix == null) {

	    prefix = prefixTable.get(uri);

	    if (prefix != null
		    && ((!isElement || defaultNS != null) && "".equals(prefix) || nsSupport.getURI(prefix) != null)) {
		prefix = null;
	    }
	}

	if (prefix == null && qName != null && !"".equals(qName)) {

	    int i = qName.indexOf(':');

	    if (i == -1) {
		if (isElement && defaultNS == null) {
		    prefix = "";
		}
	    } else {
		prefix = qName.substring(0, i);
	    }
	}

	// TODO JE: what purpose does this have exactly - setting the prefix?
	for (; prefix == null || nsSupport.getURI(prefix) != null; prefix = "__NS" + ++prefixCounter)
	    ;
	nsSupport.declarePrefix(prefix, uri);
	doneDeclTable.put(uri, prefix);

	return prefix;
    }

    /**
     * Write a raw character.
     * 
     * @param c The character to write.
     * @exception org.xml.sax.SAXException If there is an error writing the
     *                                     character, this method will throw an
     *                                     IOException wrapped in a SAXException.
     */
    private void write(char c) throws SAXException {
	try {
	    output.write(c);
	} catch (IOException e) {
	    throw new SAXException(e);
	}
    }

    /**
     * Write a raw string.
     * 
     * @param s
     * @exception org.xml.sax.SAXException If there is an error writing the string,
     *                                     this method will throw an IOException
     *                                     wrapped in a SAXException
     */
    private void write(String s) throws SAXException {
	try {
	    output.write(s);
	} catch (IOException e) {
	    throw new SAXException(e);
	}
    }

    /**
     * Write out an attribute list, escaping values.
     * <p>
     * The names will have prefixes added to them.
     * 
     * @param atts The attribute list to write.
     * @exception SAXException If there is an error writing the attribute
     *                                 list, this method will throw an IOException
     *                                 wrapped in a SAXException.
     */
    private void writeAttributes(Attributes atts) throws SAXException {

	int len = atts.getLength();

	for (int i = 0; i < len; i++) {

	    char ch[] = atts.getValue(i).toCharArray();

	    write(' ');
	    writeName(atts.getURI(i), atts.getLocalName(i), atts.getQName(i), false);
	    write("=\"");
	    writeEsc(ch, 0, ch.length, true);
	    write('"');
	}
    }

    /**
     * Write an array of data characters with escaping.
     * 
     * @param ch       The array of characters.
     * @param start    The starting position.
     * @param length   The number of characters to use.
     * @param isAttVal true if this is an attribute value literal.
     * @exception SAXException If there is an error writing the characters,
     *                                 this method will throw an IOException wrapped
     *                                 in a SAXException.
     */
    private void writeEsc(char ch[], int start, int length, boolean isAttVal) throws SAXException {

	for (int i = start; i < start + length; i++) {
	    switch (ch[i]) {
	    case '&':
		write("&amp;");
		break;
	    case '<':
		write("&lt;");
		break;
	    case '>':
		write("&gt;");
		break;
	    case '\r':
		write("&#xD;");
		break;
	    case '\n':
		write("&#xA;");
		break;
	    case '\"':
		if (isAttVal) {
		    write("&quot;");
		} else {
		    write('\"');
		}
		break;
	    default:
		// if (ch[i] > '\u007f') { // write("&#"); // // //
		// write(Integer.toString(ch[i]));
		// // //
		// write(';');
		// } else {
		write(ch[i]);
		// }
	    }
	}
    }

    /**
     * Write out the list of Namespace declarations.
     * 
     * @exception org.xml.sax.SAXException This method will throw an IOException
     *                                     wrapped in a SAXException if there is an
     *                                     error writing the Namespace declarations.
     */
    private void writeNSDecls() throws SAXException {

	Enumeration<String> prefixes = nsSupport.getDeclaredPrefixes();

	while (prefixes.hasMoreElements()) {

	    String prefix = (String) prefixes.nextElement();
	    String uri = nsSupport.getURI(prefix);

	    if (uri == null) {
		uri = "";
	    }

	    char ch[] = uri.toCharArray();

	    write(' ');

	    if ("".equals(prefix)) {
		write("xmlns=\"");
	    } else {
		write("xmlns:");
		write(prefix);
		write("=\"");
	    }

	    writeEsc(ch, 0, ch.length, true);
	    write('\"');
	}
    }

    /**
     * Write an element or attribute name.
     * 
     * @param uri       The Namespace URI.
     * @param localName The local name.
     * @param qName     The prefixed name, if available, or the empty string.
     * @param isElement true if this is an element name, false if it is an attribute
     *                  name.
     * @exception org.xml.sax.SAXException This method will throw an IOException
     *                                     wrapped in a SAXException if there is an
     *                                     error writing the name.
     */
    private void writeName(String uri, String localName, String qName, boolean isElement) throws SAXException {

	String prefix = doPrefix(uri, qName, isElement);

	if (prefix != null && !"".equals(prefix)) {
	    write(prefix);
	    write(':');
	}
	write(localName);
    }

    /**
     * Write an element with character data content.
     *
     * <p>
     * This is a convenience method to write a complete element with character data
     * content, including the start tag and end tag.
     * </p>
     *
     * <p>
     * This method invokes
     * {@link #startElement(String, String, String, Attributes)}, followed by
     * {@link #characters(String)}, followed by
     * {@link #endElement(String, String, String)}.
     * </p>
     *
     * @param uri       The element's Namespace URI.
     * @param localName The element's local name.
     * @param qName     The element's default qualified name.
     * @param atts      The element's attributes.
     * @param content   The character data content.
     * @exception org.xml.sax.SAXException If there is an error writing the empty
     *                                     tag, or if a handler further down the
     *                                     filter chain raises an exception.
     * @see #startElement(String, String, String, Attributes)
     * @see #characters(String)
     * @see #endElement(String, String, String)
     */
    public void dataElement(String uri, String localName, String qName, Attributes atts, String content)
	    throws SAXException {
	startElement(uri, localName, qName, atts);
	if (content != null) {
	    characters(content);
	}
	endElement(uri, localName, qName);
    }

    public void emptyElement(String localName, Attributes atts) throws SAXException {
	emptyElement("", localName, "", atts);
    }

    public void emptyElement(String localName, String attributeName, String attributeValue) throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	emptyElement(localName, atts);
    }

    public void emptyElement(String localName, String attributeName, String attributeValue, Operation op)
	    throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	if (op != null) {
	    atts.addAttribute("", "mode", "", "CDATA", op.toString());
	}

	emptyElement(localName, atts);
    }

    public void emptyElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
	startElement(uri, localName, qName, atts);
	endElement(uri, localName, qName);
    }

    /**
     * Write an element with character data content but no attributes.
     *
     * <p>
     * This is a convenience method to write a complete element with character data
     * content, including the start tag and end tag. This method provides an empty
     * string for the qname and an empty attribute list.
     * </p>
     *
     * <p>
     * This method invokes
     * {@link #startElement(String, String, String, Attributes)}, followed by
     * {@link #characters(String)}, followed by
     * {@link #endElement(String, String, String)}.
     * </p>
     *
     * @param uri       The element's Namespace URI.
     * @param localName The element's local name.
     * @param content   The character data content.
     * @exception org.xml.sax.SAXException If there is an error writing the empty
     *                                     tag, or if a handler further down the
     *                                     filter chain raises an exception.
     * @see #startElement(String, String, String, Attributes)
     * @see #characters(String)
     * @see #endElement(String, String, String)
     */
    public void dataElement(String uri, String localName, String content) throws SAXException {
	dataElement(uri, localName, "", EMPTY_ATTS, content);
    }

    // public void cdataElement(String uri, String localName, String content)
    // throws SAXException {
    // cdataElement(uri, localName, "", EMPTY_ATTS, content);
    // }

    /**
     * Write an element with character data content but no attributes or Namespace
     * URI.
     *
     * <p>
     * This is a convenience method to write a complete element with character data
     * content, including the start tag and end tag. The method provides an empty
     * string for the Namespace URI, and empty string for the qualified name, and an
     * empty attribute list.
     * </p>
     *
     * <p>
     * This method invokes
     * {@link #startElement(String, String, String, Attributes)}, followed by
     * {@link #characters(String)}, followed by
     * {@link #endElement(String, String, String)}.
     * </p>
     *
     * @param localName The element's local name.
     * @param content   The character data content.
     * @exception org.xml.sax.SAXException If there is an error writing the empty
     *                                     tag, or if a handler further down the
     *                                     filter chain raises an exception.
     * @see #startElement(String, String, String, Attributes)
     * @see #characters(String)
     * @see #endElement(String, String, String)
     */
    public void dataElement(String localName, String content) throws SAXException {
	dataElement("", localName, "", EMPTY_ATTS, content);
    }

    // public void cdataElement(String localName, String content)
    // throws SAXException {
    // cdataElement("", localName, "", EMPTY_ATTS, content);
    // }

    public void dataElement(String localName, String content, Operation op) throws SAXException {
	if (op != null) {
	    dataElement(localName, content, "mode", op.toString());
	} else {
	    dataElement("", localName, "", EMPTY_ATTS, content);
	}
    }

    public void dataElement(String localName, String content, String attributeName, String attributeValue)
	    throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	dataElement("", localName, "", atts, content);
    }

    public void dataElement(String uri, String localName, String content, String attributeName, String attributeValue)
	    throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	dataElement(uri, localName, "", atts, content);
    }

    public void dataElement(String localName, String content, String attributeName, String attributeValue, Operation op)
	    throws SAXException {

	AttributesImpl atts = new AttributesImpl();
	atts.addAttribute("", attributeName, "", "CDATA", attributeValue);

	if (op != null) {
	    atts.addAttribute("", "mode", "", "CDATA", op.toString());
	}

	dataElement("", localName, "", atts, content);
    }

    /**
     * Write a string of character data, with XML escaping.
     *
     * <p>
     * This is a convenience method that takes an XML String, converts it to a
     * character array, then invokes {@link #characters(char[], int, int)}.
     * </p>
     *
     * @param data The character data.
     * @exception org.xml.sax.SAXException If there is an error writing the string,
     *                                     or if a handler further down the filter
     *                                     chain raises an exception.
     * @see #characters(char[], int, int)
     */
    public void characters(String data) throws SAXException {
	char ch[] = data.toCharArray();
	characters(ch, 0, ch.length);
    }

}
