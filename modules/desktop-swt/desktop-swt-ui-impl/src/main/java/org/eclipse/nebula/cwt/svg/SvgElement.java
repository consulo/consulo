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

import java.util.ArrayList;
import java.util.List;

/**
 * An svg document is an xml document.  As such, all contained xml elements
 * are also svg elements.  SvgElement is the base abstract class for all
 * svg elements in an SvgDocument.
 */
public abstract class SvgElement {

	static List<SvgElement> getAncestry(SvgElement element) {
		List<SvgElement> l = new ArrayList<SvgElement>();
		l.add(element);
		SvgElement parent = element.getContainer();
		while(parent != null) {
			l.add(0, parent);
			parent = parent.getContainer();
		}
		return l;
	}

	private SvgContainer container;
	private String id;

	SvgElement(SvgContainer container, String id) {
		this.container = container;
		this.id = id;
		if(container != null) {
			container.add(this);
			if(!(this instanceof SvgFragment)) {
				container.getFragment().put(this);
			}
		}
	}

	final SvgContainer getContainer() {
		return container;
	}

	SvgElement getElement(String id) {
		return getFragment().getElement(id);
	}
	
	SvgFragment getFragment() {
		if(container != null) {
			return container.getFragment();
		}
		return null;
	}

	/**
	 * Return the id of this SvgElement, if it exists.
	 * @return the id of the element if it exists, null otherwise.
	 */
	public final String getId() {
		return id;
	}

	float[] getViewport() {
		return container.getViewport();
	}

	void setContainer(SvgContainer container) {
		this.container = container;
	}
	
}
