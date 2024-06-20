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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.OpenApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;
import jakarta.json.JsonObject;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class OpenApiConfigItems implements MessageSource {

    protected List<ConformanceClass> conformanceClasses = new ArrayList<>();
    protected List<QueryParameter> queryParameters = new ArrayList<>();

    public OpenApiConfigItems(ShapeChangeResult result, Element apcs) throws Exception {

	boolean ioExceptionOccurred = false;

	// identify ConformanceClass elements
	NodeList ccNl = apcs.getElementsByTagName("ConformanceClass");
	List<Element> ccEs = XMLUtil.getElementNodes(ccNl);

	for (int i = 0; i < ccEs.size(); i++) {

	    String indexForMsg = "" + (i + 1);

	    Element ccE = ccEs.get(i);

	    ConformanceClass cc = new ConformanceClass();

	    // parse uri
	    String uri = ccE.getAttribute("uri").trim();
	    if (uri.length() == 0) {
		result.addError(this, 11, "uri", indexForMsg);
		continue;
	    }
	    cc.uri = uri;

	    // parse overlay
	    String overlay = ccE.getAttribute("overlay").trim();
	    if (overlay.length() == 0) {

		if (uri.equalsIgnoreCase(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_CORE)) {
		    overlay = "https://shapechange.net/resources/openapi/overlays/features-1-10-core.json";
		} else if (uri.equalsIgnoreCase(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_GEOJSON)) {
		    overlay = "https://shapechange.net/resources/openapi/overlays/features-1-10-geojson.json";
		} else if (uri.equalsIgnoreCase(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_HTML)) {
		    overlay = "https://shapechange.net/resources/openapi/overlays/features-1-10-html.json";
		} else if (uri.equalsIgnoreCase(OpenApiConstants.CC_OGCAPI_FEATURES_2_1_CRS)) {
		    overlay = "https://shapechange.net/resources/openapi/overlays/features-2-10-crs.json";
		} else {
		    result.addError(this, 10, "overlay", indexForMsg);
		    continue;
		}
	    }
	    try {
		JsonObject overlayJson = OpenApiDefinition.loadJson(overlay);
		cc.overlay = overlayJson;
	    } catch (IOException e) {
		ioExceptionOccurred = true;
		result.addError(this, 14, indexForMsg, cc.uri, overlay, e.getMessage());
		continue;
	    }

	    // parse param
	    String param = ccE.getAttribute("param").trim();
	    if (param.length() == 0) {
		cc.param = Optional.empty();
	    } else {
		cc.param = Optional.of(param);
	    }

	    this.conformanceClasses.add(cc);
	}

	// identify QueryParameter elements
	NodeList qpNl = apcs.getElementsByTagName("QueryParameter");
	List<Element> qpEs = XMLUtil.getElementNodes(qpNl);

	for (int i = 0; i < qpEs.size(); i++) {

	    String indexForMsg = "" + (i + 1);

	    Element qpE = qpEs.get(i);

	    QueryParameter qp = new QueryParameter();

	    // parse name
	    String name = qpE.getAttribute("name").trim();
	    if (name.length() == 0) {
		result.addError(this, 12, "name", indexForMsg);
		continue;
	    }
	    qp.name = name;

	    // parse overlay
	    String overlay = qpE.getAttribute("overlay").trim();
	    if (overlay.length() == 0) {
		result.addError(this, 12, "overlay", indexForMsg);
		continue;
	    }
	    try {
		JsonObject overlayJson = OpenApiDefinition.loadJson(overlay);
		qp.overlay = overlayJson;
	    } catch (IOException e) {
		ioExceptionOccurred = true;
		result.addError(this, 16, indexForMsg, qp.name, overlay, e.getMessage());
		continue;
	    }

	    // parse appliesToPhase
	    String param = qpE.getAttribute("appliesToPhase").trim();
	    if (param.length() != 0) {
		switch (param) {
		case "pre-feature-identification":
		    qp.phase = OpenApiDefinitionProcessingPhase.PRE_FEATURE_IDENTIFICATION;
		    break;
		default:
		    qp.phase = OpenApiDefinitionProcessingPhase.FINALIZATION;
		    break;
		}
	    }

	    this.queryParameters.add(qp);
	}

	if (ioExceptionOccurred) {
	    throw new Exception("An IOException occurred. Consult the log for further details");
	}
    }

    /**
     * @return the conformanceClasses; can be empty but not <code>null</code>
     */
    public List<ConformanceClass> getConformanceClasses() {
	return conformanceClasses;
    }

    /**
     * @return the queryParameters; can be empty but not <code>null</code>
     */
    public List<QueryParameter> getQueryParameters() {
	return queryParameters;
    }

    /**
     * @param conformanceClasses the conformanceClasses to set
     */
    public void setConformanceClasses(List<ConformanceClass> conformanceClasses) {
	if (conformanceClasses != null)
	    this.conformanceClasses = conformanceClasses;
    }

    /**
     * @param queryParameters the queryParameters to set
     */
    public void setQueryParameters(List<QueryParameter> queryParameters) {
	if (queryParameters != null)
	    this.queryParameters = queryParameters;
    }

    public Optional<ConformanceClass> conformanceClass(String conformanceClassUri) {
	return conformanceClasses.stream().filter(cc -> cc.uri.equalsIgnoreCase(conformanceClassUri)).findFirst();
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 10:
	    return "'$1$' attribute of $2$ ConformanceClass element is empty, with no default being defined for the conformance class. ConformanceClass will be ignored.";
	case 11:
	    return "'$1$' attribute of $2$ ConformanceClass element is empty which is not allowed. ConformanceClass will be ignored.";
	case 12:
	    return "'$1$' attribute of $2$ QueryParameter element is empty which is not allowed. QueryParameter will be ignored.";
	case 14:
	    return "Could not load JSON overlay for $1$ ConformanceClass with uri '$2$' from: '$3$'. ConformanceClass will be ignored. Exception message is: $4$";
	case 16:
	    return "Could not load JSON overlay for $1$ QueryParameter with name '$2$' from: '$3$'. QueryParameter will be ignored. Exception message is: $4$";

	default:
	    return "(OpenApiConfigItems.java) Unknown message with number: " + mnr;
	}
    }
}
