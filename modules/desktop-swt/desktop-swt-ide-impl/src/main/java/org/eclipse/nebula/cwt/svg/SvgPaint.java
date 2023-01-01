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

abstract class SvgPaint {

	enum PaintType { None, Current, Color, Link }
	
	SvgGraphic parent;
	GC gc;
	SvgGradient paintServer;
	PaintType type = null;
	String linkId = null;
	Integer color = null;
	Float opacity = null;

	SvgPaint(SvgGraphic parent) {
		this.parent = parent;
	}
	
	abstract void apply();
	
	public void create(GC gc) {
		if(parent instanceof SvgShape) {
			this.gc = gc;
			if(linkId != null) {
				SvgElement def = parent.getElement(linkId);
				if(def instanceof SvgGradient) {
					SvgGradient gradient = (SvgGradient) def;
					SvgShape shape = (SvgShape) parent;
					paintServer = gradient;
					paintServer.create(shape, gc);
				}
			}
		} else {
			throw new UnsupportedOperationException("only shapes can be painted..."); //$NON-NLS-1$
		}
	}
	
	public boolean dispose() {
		if(paintServer != null) {
			paintServer.dispose();
			return true;
		}
		return false;
	}

	public boolean isPaintable() {
		return type != PaintType.None;
	}
	
}
