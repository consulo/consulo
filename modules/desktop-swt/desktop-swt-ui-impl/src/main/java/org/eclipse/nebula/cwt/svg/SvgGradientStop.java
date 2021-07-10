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


class SvgGradientStop extends SvgElement {

	/**
	 * always a value between 0 and 1
	 */
	Float offset;
	Integer color;
	Float opacity;
	
	SvgGradientStop(SvgGradient gradient, String id) {
		super(gradient.getContainer(), id);
	}
	
	int alpha() {
		return (int) (255 * opacity);
	}
	
	int blue() {
		return color & 0x0000FF;
	}
	
	int green() {
		return (color & 0x00FF00) >> 8;
	}
	
	int red() {
		return color >> 16;
	}
	
}
