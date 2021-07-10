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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

/**
 * An svg document can contain one or more svg fragments, each denoted
 * by the "svg" tag. Each of these consists of all the information
 * necessary to paint a graphic to the screen, including definition
 * and css style child elements.
 * <p>
 * Of particular importance is that the svg fragment can also contain
 * a viewbox which can be used for scaling the image to a particular size.
 * Therefore this element is where the real-world rendered dimensions
 * interact with the svg dimenions.
 * </p>
 * <p>
 * See also:
 * <a href=
 * "http://www.w3.org/TR/SVG/struct.html#SVGElement">http://www.w3.org/TR/SVG/struct.html#SVGElement</a>
 * </p>
 */
public class SvgFragment extends SvgContainer {

	Float x;
	Float y;
	Float width;
	Float height;
	/**
	 * viewBox[0] == x
	 * viewBox[1] == y
	 * viewBox[2] == w
	 * viewBox[3] == h
	 */
	float[] viewBox;
	SvgTransform boundsTransform;
	boolean preserveAspectRatio;

	private Map<String, SvgElement> elementMap;

	SvgFragment(SvgContainer container, String id) {
		super(container, id);
		elementMap = new HashMap<String, SvgElement>();
		boundsTransform = new SvgTransform();
		boundsTransform.data = new float[] { 1, 0, 0, 1, 0, 0 };
	}

	/**
	 * Apply this svg fragment to the given graphics context, scaled to fit
	 * within
	 * the given bounds. This method will recursive call the apply methods of
	 * all
	 * contained svg elements, thereby painting the entire fragment to the given
	 * graphics context.
	 * 
	 * @param gc
	 *            the graphics context
	 * @param bounds
	 *            the bounds to which this fragment will be scaled
	 */
	public void apply(GC gc, Rectangle bounds) {
		boundsTransform.translate(bounds.x, bounds.y);

		if (viewBox != null) {
			float sx = bounds.width / viewBox[2];
			float sy = bounds.height / viewBox[3];
			boundsTransform.scale(sx, sy);
		} else if (width != null && height != null) {
			float sx = bounds.width / width;
			float sy = bounds.height / height;
			boundsTransform.scale(sx, sy);
		}

		super.apply(gc);

		boundsTransform.data = new float[] { 1, 0, 0, 1, 0, 0 };
	}

	public SvgElement getElement(String id) {
		return elementMap.get(id);
	}

	@Override
	public SvgFragment getFragment() {
		return this;
	}

	/**
	 * Return a map of css styles for the given class name, if it exists.
	 * <p>
	 * Each SvgFragment can contain a style element which consists of css
	 * styles.
	 * </p>
	 * 
	 * @param className
	 *            the name of the css class to return styles for.
	 * @return a map of css style for the given class name if it exists, null
	 *         otherwise.
	 */
	public Map<String, String> getStyles(String className) {
		SvgElement element = elementMap.get("style"); //$NON-NLS-1$
		if (element instanceof SvgStyle) {
			Map<String, Map<String, String>> classes = ((SvgStyle) element).styles;
			if (classes != null) {
				return classes.get(className);
			}
		}
		return null;
	}

	@Override
	public float[] getViewport() {
		if (x == null || y == null) {
			return new float[] { 0, 0, width, height };
		} else {
			return new float[] { x, y, width, height };
		}
	}

	/**
	 * Returns true if this fragment contains an SvgElement with the given id.
	 * 
	 * @param id
	 *            the id of the element
	 * @return true if the element exists, false otherwise
	 */
	public boolean hasElement(String id) {
		return elementMap.containsKey(id);
	}

	/**
	 * Returns true if this SvgFragment is at the outermost level, meaning it
	 * is a direct child of the SvgDocument. This is an important distinction
	 * because, as with all svg elements, fragments can be nested. Each svg
	 * fragment will establish a new coordinate system, but only the outer
	 * fragment will determine the scaling necessary to display at the requested
	 * size.
	 * 
	 * @return true if this fragment is at the outermost level, false otherwise.
	 */
	public boolean isOutermost() {
		return getContainer() == null;
	}

	void put(SvgElement element) {
		String id = element.getId();
		if (id != null) {
			elementMap.put(id, element);
		}
	}

	public float[] getViewBox() {
		return viewBox;
	}

	public Float getWidth() {
		return width;
	}

	public Float getHeight() {
		return height;
	}
}
