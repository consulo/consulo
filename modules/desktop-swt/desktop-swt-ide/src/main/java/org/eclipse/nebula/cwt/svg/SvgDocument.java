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
import org.eclipse.swt.graphics.Rectangle;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The SvgDocument is the base for all svg graphics.  It is used to
 * load an svg document from an inputstream or directly from a String.</p>
 * <p>An svg document may contain one or more svg fragments, each of which
 * can be accessed individually.</p>
 * <p></p>
 * <p>See also:
 * <a href="http://www.w3.org/TR/SVG">http://www.w3.org/TR/SVG</a></p>
 */
public class SvgDocument extends SvgContainer {

	/**
	 * Create a new SvgDocument from the contents of the given <code>InputStream</code>.
	 * @param in an <code>InputStream</code> containing the svg source.
	 * @return a newly created SvgDocument
	 */
	public static SvgDocument load(InputStream in) {
		return SvgLoader.load(in);
	}

	/**
	 * Create a new SvgDocument from the contents of the given <code>String</code>.
	 * @param src an <code>String</code> containing the svg source.
	 * @return a newly created SvgDocument
	 */
	public static SvgDocument load(String src) {
		return SvgLoader.load(src);
	}

	private Map<String, SvgFragment> fragmentMap;

	SvgDocument() {
		super(null, null);
		fragmentMap = new HashMap<String, SvgFragment>(3);
	}

	@Override
	void add(SvgElement element) {
		if(element instanceof SvgFragment) {
			elements.add(element);
			fragmentMap.put(element.getId(), (SvgFragment) element);
		}
	}

	/**
	 * Apply this svg document to the given graphics context, scaled to fit within
	 * the given bounds.  This method will recursive call the apply methods of all
	 * contained svg elements, thereby painting the entire document to the given
	 * graphics context.
	 * @param gc the graphics context
	 * @param bounds the bounds to which this document will be scaled
	 */
	public void apply(GC gc, Rectangle bounds) {
		for(SvgElement element : elements) {
			((SvgFragment) element).apply(gc, bounds);
		}
	}

	@Override
	public String getDescription() {
		return elements.isEmpty() ? null : ((SvgFragment) elements.get(0)).getDescription();
	}
	
	public SvgFragment getFragment() {
		return elements.isEmpty() ? null : (SvgFragment) elements.get(0);
	}

	/**
	 * Returns the SvgFragment element within this document that corresponds to the given id.
	 * @param id
	 * @return an SvgFragment with the given id, or null if one does not exist
	 */
	public SvgFragment getFragment(String id) {
		return fragmentMap.get(id);
	}

	/**
	 * Returns an array of all the SvgFragment elements contained by this document.  This is
	 * a new array - modification to it will not affect the underlying collection.
	 * @return an array of SvgFragments
	 */
	public SvgFragment[] getFragments() {
		return elements.toArray(new SvgFragment[elements.size()]);
	}

	@Override
	public String getTitle() {
		return elements.isEmpty() ? null : ((SvgFragment) elements.get(0)).getTitle();
	}

	/**
	 * Returns true if this document contains an SvgFragment with the given id.
	 * @param id the id of the fragment
	 * @return true if the fragment exists, false otherwise
	 */
	public boolean hasFragment(String id) {
		return fragmentMap.containsKey(id);
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

}
