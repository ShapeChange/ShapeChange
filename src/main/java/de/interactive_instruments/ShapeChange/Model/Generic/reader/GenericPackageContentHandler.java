package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Model.Descriptor;

public class GenericPackageContentHandler
		extends AbstractGenericInfoContentHandler {

	private static final Set<String> SIMPLE_PACKAGE_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "targetNamespace", "xmlns",
					"xsdDocument", "gmlProfileSchema", "version", "isAppSchema",
					"isSchema" }));
	// , "schemaId", "ownerId", "rootPackageId"

	private boolean isInPackages = false;

	private GenericPackageInfo genPi;

	// private String ownerId = null;
	// private String rootPackageId = null;

	private List<GenericPackageContentHandler> childPackageContentHandlers = new ArrayList<GenericPackageContentHandler>();
	private List<GenericClassContentHandler> classContentHandlers = new ArrayList<GenericClassContentHandler>();

	public GenericPackageContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);

		this.genPi = new GenericPackageInfo();
		this.genPi.setResult(result);
		this.genPi.setOptions(options);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (GenericModelReaderConstants.SIMPLE_INFO_FIELDS
				.contains(localName)) {

			sb = new StringBuffer();

		} else if (localName.equals("descriptors")) {

			DescriptorsContentHandler handler = new DescriptorsContentHandler(
					result, options, reader, this);

			super.descriptorsHandler = handler;
			reader.setContentHandler(handler);

		} else if (localName.equals("taggedValues")) {

			reader.setContentHandler(new TaggedValuesContentHandler(result,
					options, reader, this));

		} else if (localName.equals("stereotypes")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("diagrams")) {

			reader.setContentHandler(
					new DiagramsContentHandler(result, options, reader, this));

		} else if (SIMPLE_PACKAGE_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else if (localName.equals("supplierIds")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("packages")) {

			isInPackages = true;

		} else if (localName.equals("Package")) {

			if (isInPackages) {
				GenericPackageContentHandler handler = new GenericPackageContentHandler(
						result, options, reader, this);
				this.childPackageContentHandlers.add(handler);
				reader.setContentHandler(handler);
			}

		} else if (localName.equals("classes")) {

			// ignore

		} else if (localName.equals("Class")) {

			GenericClassContentHandler handler = new GenericClassContentHandler(
					result, options, reader, this);
			this.classContentHandlers.add(handler);
			reader.setContentHandler(handler);

		} else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "GenericPackageContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("id")) {

			this.genPi.setId(sb.toString());
			
//			String id = sb.toString();
//			// strip "_PKG" prefix added by ModelExport
//			id = id.substring(4);
//			this.genPi.setId(id);

		} else if (localName.equals("name")) {

			this.genPi.setName(sb.toString());

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = options.stereotypesFactory();
			for (String stereotype : this.stringList) {
				stereotypesCache.add(stereotype);
			}
			this.genPi.setStereotypes(stereotypesCache);

		} else if (localName.equals("descriptors")) {

			/*
			 * ignore - we have a reference to the DescriptorsContentHandler
			 */

		} else if (localName.equals("taggedValues")) {

			/*
			 * ignore - TaggedValuesContentHandler calls
			 * this.setTaggedValues(...)
			 */

		} else if (localName.equals("diagrams")) {

			/*
			 * ignore - DiagramsContentHandler calls this.setDiagrams(...)
			 */

		} else if (localName.equals("targetNamespace")) {

			this.genPi.setTargetNamespace(sb.toString());

		} else if (localName.equals("xmlns")) {

			this.genPi.setXmlns(sb.toString());

		} else if (localName.equals("xsdDocument")) {

			this.genPi.setXsdDocument(sb.toString());

		} else if (localName.equals("gmlProfileSchema")) {

			this.genPi.setGmlProfileSchema(sb.toString());

		} else if (localName.equals("version")) {

			this.genPi.setVersion(sb.toString());

		}
		// else if (localName.equals("schemaId")) {
		//
		// this.genPi.setSchemaId(sb.toString());
		//
		// }
		else if (localName.equals("isAppSchema")) {

			this.genPi.setIsAppSchema(toBooleanValue(sb));

		} else if (localName.equals("isSchema")) {

			this.genPi.setIsSchema(toBooleanValue(sb));

		}
		// else if (localName.equals("ownerId")) {
		//
		// this.ownerId = sb.toString();
		//
		// } else if (localName.equals("rootPackageId")) {
		//
		// this.rootPackageId = sb.toString();
		//
		// }
		else if (localName.equals("supplierIds")) {

			this.genPi.setSupplierIds(new TreeSet<String>(this.stringList));

		} else if (localName.equals("packages")) {

			isInPackages = false;

		} else if (localName.equals("classes")) {

			// ignore

		} else if (localName.equals("Class")) {

			// ignore

		} else if (localName.equals("Package")) {

			if (!isInPackages) {

				// set descriptors in genPi
				this.genPi.setDescriptors(descriptorsHandler.getDescriptors());
//				for (Entry<Descriptor, Descriptors> entry : descriptors
//						.getDescriptors().entrySet()) {
//
//					if (entry.getKey() == Descriptor.ALIAS) {
//						this.genPi.setAliasNameAll(entry.getValue());
//					} else if (entry.getKey() == Descriptor.PRIMARYCODE) {
//						this.genPi.setPrimaryCodeAll(entry.getValue());
//					} else if (entry.getKey() == Descriptor.GLOBALIDENTIFIER) {
//						this.genPi.setGlobalIdentifierAll(entry.getValue());
//					}
//					// else if(entry.getKey() == Descriptor.DOCUMENTATION) {
//					// this.genPi.setDocumentationAll(entry.getValue());
//					// }
//					else if (entry.getKey() == Descriptor.DEFINITION) {
//						this.genPi.setDefinitionAll(entry.getValue());
//					} else if (entry.getKey() == Descriptor.DESCRIPTION) {
//						this.genPi.setDescriptionAll(entry.getValue());
//					} else if (entry.getKey() == Descriptor.LEGALBASIS) {
//						this.genPi.setLegalBasisAll(entry.getValue());
//					} else if (entry.getKey() == Descriptor.LANGUAGE) {
//						this.genPi.setLanguageAll(entry.getValue());
//					} else if (entry.getKey() == Descriptor.EXAMPLE) {
//						this.genPi.setExamplesAll(entry.getValue());
//					} else if (entry
//							.getKey() == Descriptor.DATACAPTURESTATEMENT) {
//						this.genPi
//								.setDataCaptureStatementsAll(entry.getValue());
//					}
//				}

				// set contained packages
				SortedSet<GenericPackageInfo> children = new TreeSet<GenericPackageInfo>();
				for (GenericPackageContentHandler gpch : this.childPackageContentHandlers) {
					children.add(gpch.getGenericPackage());
				}
				this.genPi.setContainedPackages(children);

				// set contained classes
				SortedSet<GenericClassInfo> classes = new TreeSet<GenericClassInfo>();
				for (GenericClassContentHandler gcch : this.classContentHandlers) {
					classes.add(gcch.getGenericClass());
				}
				this.genPi.setClasses(classes);

				// let parent know that we reached the end of the Package entry
				// (so that for example depth can properly be tracked)
				parent.endElement(uri, localName, qName);

				// Switch handler back to parent
				reader.setContentHandler(parent);
			}

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "GenericPackageContentHandler",
					localName);
		}
	}

	/**
	 * @return the genPi
	 */
	public GenericPackageInfo getGenericPackage() {
		return genPi;
	}

	@Override
	public void setTaggedValues(TaggedValues taggedValues) {

		this.genPi.setTaggedValues(taggedValues, false);
	}

	@Override
	public void setDiagrams(List<ImageMetadata> diagrams) {

		this.genPi.setDiagrams(diagrams);
	}

	// /**
	// * @return the ownerId
	// */
	// public String getOwnerId() {
	// return ownerId;
	// }
	//
	// /**
	// * @return the rootPackageId
	// */
	// public String getRootPackageId() {
	// return rootPackageId;
	// }

	/**
	 * @return the content handlers for the child packages; can be empty but not
	 *         <code>null</code>
	 */
	public List<GenericPackageContentHandler> getChildPackageContentHandlers() {
		return childPackageContentHandlers;
	}

	/**
	 * @return the classContentHandlers
	 */
	public List<GenericClassContentHandler> getClassContentHandlers() {
		return classContentHandlers;
	}

	public void visitPackageContentHandlers(
			Map<String, GenericPackageContentHandler> resultMap) {

		resultMap.put(this.genPi.id(), this);

		for (GenericPackageContentHandler gpch : this.childPackageContentHandlers) {

			gpch.visitPackageContentHandlers(resultMap);
		}
	}
}
