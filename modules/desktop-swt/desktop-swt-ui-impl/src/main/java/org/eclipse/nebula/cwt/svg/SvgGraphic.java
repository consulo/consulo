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

import org.eclipse.nebula.cwt.svg.SvgPaint.PaintType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;

/**
 * A base abstract class for all types of svg elements which can be
 * applied (painted) to a graphics context.  These may be shapes which
 * can be painted directly, or containers which will paint their children.
 */
public abstract class SvgGraphic extends SvgElement {

	String title;
	String description;
	SvgFill fill;
	SvgStroke stroke;
	SvgTransform transform;
	
	SvgGraphic(SvgContainer container, String id) {
		super(container, id);
		fill = new SvgFill(this);
		stroke = new SvgStroke(this);
	}

	/**
	 * Apply this svg graphic to the given graphics context.
	 * <p>Note that to support the rather abstract structure of svg,
	 * each time this method is called all transformations and css properties 
	 * to be calculated and applied.  If this is a shape, it will be
	 * painted to the graphics context.  Containers will recursively
	 * make this call on their children.</p>
	 * @param gc the gc to use in all graphics operations
	 */
	public abstract void apply(GC gc);
	
	/**
	 * Returns the value of the <code>desc</code> element that is a child of this svg element.
	 * If there is no <code>desc</code> element that is a direct decendent of this element, null
	 * is returned.
	 * @return the <code>desc</code> of this svg element
	 */
	public String getDescription() {
		return (description == null) ? null : description;
	}
	
	SvgFill getFill() {
		SvgFill df = new SvgFill(this);
		for(SvgElement el : getAncestry(this)) {
			if(el instanceof SvgGraphic) {
				SvgFill tmp = ((SvgGraphic) el).fill;
				if(tmp.type != null) {
					df.type = tmp.type;
				}
				if(tmp.color != null) {
					df.color = tmp.color;
				}
				if(tmp.linkId != null) {
					df.linkId = tmp.linkId;
				}
				if(tmp.opacity != null) {
					df.opacity = tmp.opacity;
				}
				if(tmp.rule != null) {
					df.rule = tmp.rule;
				}
			}
		}
		if(df.type == null) {
			df.type = PaintType.Color;
		}
		if(df.color == null) {
			df.color = 0;
		}
		if(df.opacity == null) {
			df.opacity = 1f;
		}
		if(df.rule == null) {
			df.rule = SWT.FILL_EVEN_ODD;
		}
		return df;
	}

	SvgStroke getStroke() {
		SvgStroke ds = new SvgStroke(this);
		for(SvgElement el : getAncestry(this)) {
			if(el instanceof SvgGraphic) {
				SvgStroke tmp = ((SvgGraphic) el).stroke;
				if(tmp.type != null) {
					ds.type = tmp.type;
				}
				if(tmp.color != null) {
					ds.color = tmp.color;
				}
				if(tmp.linkId != null) {
					ds.linkId = tmp.linkId;
				}
				if(tmp.opacity != null) {
					ds.opacity = tmp.opacity;
				}
				if(tmp.width != null) {
					ds.width = tmp.width;
				}
				if(tmp.lineCap != null) {
					ds.lineCap = tmp.lineCap;
				}
				if(tmp.lineJoin != null) {
					ds.lineJoin = tmp.lineJoin;
				}
			}
		}
		if(ds.type == null) {
			ds.type = PaintType.None;
		}
		if(ds.type != PaintType.None) {
			if(ds.color == null) {
				ds.color = 0;
			}
			if(ds.opacity == null) {
				ds.opacity = 1f;
			}
			if(ds.width == null) {
				ds.width = 1f;
			}
			if(ds.lineCap == null) {
				ds.lineCap = SWT.CAP_FLAT;
			}
			if(ds.lineJoin == null) {
				ds.lineJoin = SWT.JOIN_MITER;
			}
		}
		return ds;
	}
	
	Transform getTransform(GC gc) {
		Transform t = new Transform(gc.getDevice());
		gc.getTransform(t);
		for(SvgElement el : getAncestry(this)) {
			if(el instanceof SvgFragment) {
				SvgTransform st = ((SvgFragment) el).boundsTransform;
				if(!st.isIdentity()) {
					Transform tmp = new Transform(gc.getDevice());
					tmp.setElements(st.data[0], st.data[1], st.data[2], st.data[3], st.data[4], st.data[5]);
					t.multiply(tmp);
					tmp.dispose();
				}
			} else if(el instanceof SvgGraphic) {
				SvgTransform st = ((SvgGraphic) el).transform;
				while(st != null) {
					if(!st.isIdentity()) {
						Transform tmp = new Transform(gc.getDevice());
						tmp.setElements(st.data[0], st.data[1], st.data[2], st.data[3], st.data[4], st.data[5]);
						t.multiply(tmp);
						tmp.dispose();
					}
					st = st.next;
				}
			}
		}
		return t;
	}
	
	/**
	 * Returns the value of the <code>title</code> element that is a child of this svg element.
	 * If there is no <code>title</code> element that is a direct decendent of this element, null
	 * is returned.
	 * @return the <code>title</code> of this svg element
	 */
	public String getTitle() {
		return (title == null) ? null : title;
	}

}
