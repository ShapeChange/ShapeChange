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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.Optional;

import de.interactive_instruments.shapechange.core.model.ClassInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpPropertyEncodingContext {

    protected boolean isInFragment = false;
    protected ClassInfo type = null;
    protected LdpPropertyEncodingContext parentContext = null;

    /**
     * @return the isInFragment
     */
    public boolean isInFragment() {
	return isInFragment;
    }

    /**
     * @param isInFragment the isInFragment to set
     */
    public void setInFragment(boolean isInFragment) {
	this.isInFragment = isInFragment;
    }

    /**
     * @return the type
     */
    public ClassInfo getType() {
	return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(ClassInfo type) {
	this.type = type;
    }

    public void setParentContext(LdpPropertyEncodingContext context) {
	this.parentContext = context;
    }

    public Optional<LdpPropertyEncodingContext> getParentContext() {
	return this.parentContext == null ? Optional.empty() : Optional.of(this.parentContext);
    }

    public LdpPropertyEncodingContext getTopParentContext() {
	if (this.parentContext == null) {
	    return this;
	} else {
	    return this.parentContext.getTopParentContext();
	}
    }
}
