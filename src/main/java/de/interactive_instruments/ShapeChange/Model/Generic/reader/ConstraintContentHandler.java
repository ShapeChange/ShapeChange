package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Constraint.ModelElmtContextType;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericFolConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericOclConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericTextConstraint;

public class ConstraintContentHandler extends AbstractContentHandler {

	private static final Set<String> CONSTRAINT_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "name", "status", "text", "type",
					"sourceType", "contextModelElementId",
					"contextModelElementType" }));

	private String name = null;
	private String status = null;
	private String text = null;
	private String type = null;
	private String sourceType = null;
	private String contextModelElementId = null;
	private String contextModelElementType = null;

	private Constraint constraint = null;

	public ConstraintContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractGenericInfoContentHandler parent) {
		super(result, options, reader, parent);
		this.parent = parent;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (CONSTRAINT_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "ConstraintContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("name")) {

			this.name = sb.toString();

		} else if (localName.equals("status")) {

			this.status = sb.toString();

		} else if (localName.equals("text")) {

			this.text = sb.toString();

		} else if (localName.equals("type")) {

			this.type = sb.toString();

		} else if (localName.equals("sourceType")) {

			this.sourceType = sb.toString();

		} else if (localName.equals("contextModelElementId")) {

			this.contextModelElementId = sb.toString();

		} else if (localName.equals("contextModelElementType")) {

			this.contextModelElementType = sb.toString();

		} else if (localName.equals("FolConstraint")) {

			GenericFolConstraint con = new GenericFolConstraint();
			this.constraint = con;
			
			con.setName(name);
			con.setStatus(status);
			con.setText(text);
			con.setSourceType(sourceType);
			if (this.contextModelElementType != null
					&& this.contextModelElementType
							.equalsIgnoreCase("ATTRIBUTE")) {
				con.setContextModelElmtType(ModelElmtContextType.ATTRIBUTE);
			} else {
				con.setContextModelElmtType(ModelElmtContextType.CLASS);
			}

			returnToParent(uri,localName,qName);

		} else if (localName.equals("OclConstraint")) {

			GenericOclConstraint con = new GenericOclConstraint();
			this.constraint = con;
			
			con.setName(name);
			con.setStatus(status);
			con.setText(text);			
			if (this.contextModelElementType != null
					&& this.contextModelElementType
							.equalsIgnoreCase("ATTRIBUTE")) {
				con.setContextModelElmtType(ModelElmtContextType.ATTRIBUTE);
			} else {
				con.setContextModelElmtType(ModelElmtContextType.CLASS);
			}

			returnToParent(uri,localName,qName);

		} else if (localName.equals("TextConstraint")) {

			GenericTextConstraint con = new GenericTextConstraint();
			this.constraint = con;
			
			con.setName(name);
			con.setStatus(status);
			con.setText(text);		
			con.setType(type);
			if (this.contextModelElementType != null
					&& this.contextModelElementType
							.equalsIgnoreCase("ATTRIBUTE")) {
				con.setContextModelElmtType(ModelElmtContextType.ATTRIBUTE);
			} else {
				con.setContextModelElmtType(ModelElmtContextType.CLASS);
			}

			returnToParent(uri,localName,qName);

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "ConstraintContentHandler",
					localName);
		}
	}

	private void returnToParent(String uri, String localName, String qName) throws SAXException {

		// let parent know that we reached the end of the constraint entry
		// (so that for example depth can properly be tracked)
		parent.endElement(uri, localName, qName);

		// Switch handler back to parent
		reader.setContentHandler(parent);
	}

	/**
	 * @return the contextModelElementId
	 */
	public String getContextModelElementId() {
		return contextModelElementId;
	}

	/**
	 * @return the constraint
	 */
	public Constraint getConstraint() {
		return constraint;
	}

}
