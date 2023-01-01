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

/**
 * An SvgUse is an svg graphical element that uses another, previously defined
 * graphical element to paint to the graphics context with its own set of styles
 * and transforms.
 * <p>See also:
 * <a href="http://www.w3.org/TR/SVG/struct.html#UseElement">http://www.w3.org/TR/SVG/struct.html#UseElement</a></p>
 */
public class SvgUse extends SvgGraphic {

	String linkId;
	float x;
	float y;
	Float w;
	Float h;
	
	SvgUse(SvgContainer container, String id) {
		super(container, id);
	}

	public void apply(GC gc) {
		SvgGraphic graphic = getGraphic();
		if(graphic != null) {
			// TODO: proxy container?
			SvgContainer c = graphic.getContainer();
			graphic.setContainer(getContainer());
			graphic.apply(gc);
			graphic.setContainer(c);
		}
	}

	SvgFill getFill() {
		SvgGraphic graphic = getGraphic();
		if(graphic != null) {
			return graphic.getFill();
		}
		return null;
	}
	
	private SvgGraphic getGraphic() {
		Object def = getFragment().getElement(linkId);
		if(def instanceof SvgGraphic) {
			return (SvgGraphic) def;
		}
		return null;
	}

	SvgStroke getStroke() {
		SvgGraphic graphic = getGraphic();
		if(graphic != null) {
			return graphic.getStroke();
		}
		return null;
	}

}
