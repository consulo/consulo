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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

class SvgStroke extends SvgPaint {

	Float width = null;
	Integer lineCap = SWT.CAP_FLAT;
	Integer lineJoin = SWT.JOIN_MITER;

	SvgStroke(SvgGraphic parent) {
		super(parent);
	}
	
	public void apply() {
		if(paintServer != null) {
			paintServer.apply(true);
		} else {
			Color c = new Color(gc.getDevice(), color >> 16, (color & 0x00FF00) >> 8, color & 0x0000FF);
			gc.setForeground(c);
			c.dispose();
			gc.setLineWidth((int)Math.ceil(width));
			gc.setLineCap(lineCap);
			gc.setLineJoin(lineJoin);
			gc.setAlpha((int)(255 * opacity));
		}
	}

}
