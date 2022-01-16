/****************************************************************************
 * Copyright (c) 2008, 2009 Jeremy Dowdall
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Jeremy Dowdall <jeremyd@aspencloud.com> - initial API and implementation
 *****************************************************************************/
package org.eclipse.nebula.cwt.svg;

import org.eclipse.swt.graphics.GC;

import java.util.ArrayList;
import java.util.List;

/**
 * An SvgElement which is capable of containing other SvgElements.
 * The most commonly accessed container element types are the document, fragment, and group.
 */
public class SvgContainer extends SvgGraphic {

	List<SvgElement> elements;
	
	SvgContainer(SvgContainer container, String id) {
		super(container, id);
		elements = new ArrayList<SvgElement>();
	}

	void add(SvgElement element) {
		elements.add(element);
	}
	
	public void apply(GC gc) {
		for(SvgElement element : elements) {
			if(element instanceof SvgGraphic) {
				((SvgGraphic) element).apply(gc);
			}
		}
	}
	
	/**
	 * Returns an array of child elements contained by this container element.
	 * Modifying this array will not affect the underlying element list of this
	 * container element.
	 * @return an array of child elements contained by this element.
	 */
	public SvgElement[] getElements() {
		return elements.toArray(new SvgElement[elements.size()]);
	}

    /**
     * Returns true if this list contains no elements.
     * @return true if this list contains no elements.
     */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

}
