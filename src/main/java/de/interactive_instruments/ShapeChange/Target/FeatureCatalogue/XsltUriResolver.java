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

package de.interactive_instruments.ShapeChange.Target.FeatureCatalogue;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 * 
 */
public class XsltUriResolver implements URIResolver {

	private Map<String, URI> hrefMappings = null;

	public XsltUriResolver(Map<String, URI> hrefMappings) {
		this.hrefMappings = hrefMappings;
	}

	public Source resolve(String href, String base) throws TransformerException {

		InputStream inputStream = null;
		StreamSource source = null;

		try {

			if (hrefMappings.containsKey(href)) {
				// handle case where we have a mapping
				inputStream = hrefMappings.get(href).toURL().openStream();
				source = new StreamSource(inputStream);
				source.setSystemId(hrefMappings.get(href).toString());

			} else if (base == null || base.trim().length() == 0) {
				/*
				 * Usually all hrefs belonging to an xsl:include or xsl:import
				 * should have a base. This case is most likely a call from the
				 * document() function.
				 */
				URI resourceUri = new URI(href);
				if (resourceUri.isAbsolute()) {
					inputStream = resourceUri.toURL().openStream();
					source = new StreamSource(inputStream);
					source.setSystemId(resourceUri.toString());
				} else {
					// use of relative URIs in XSLT parameters is discouraged
					throw new TransformerException(
							"Relative URI encountered by XsltUriResolver: "
									+ href);
				}

			} else {
				URI resolvedUri = new URI(base).resolve(href);
				inputStream = resolvedUri.toURL().openStream();
				source = new StreamSource(inputStream);
				source.setSystemId(resolvedUri.toString());
			}

			return source;

		} catch (Exception ex) {

			ex.printStackTrace();
			return null;

		} finally {
			// do not close the input stream here
		}
	}

}
