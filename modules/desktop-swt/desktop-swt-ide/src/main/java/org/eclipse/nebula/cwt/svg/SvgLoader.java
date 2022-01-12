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
 *    Edward Francis <edward.k.francis@gmail.com> - Handle Bézier relative commands correctly
 *    Edward Frnacis <edward.k.francis@gmail.com> - parsePathData handle multiple commands.
 *****************************************************************************/
package org.eclipse.nebula.cwt.svg;

import org.eclipse.nebula.cwt.svg.SvgPaint.PaintType;
import org.eclipse.nebula.cwt.svg.SvgTransform.Type;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

class SvgLoader {

	private static final char[] ATTR_CLASS = new char[] { 'c', 'l', 'a', 's', 's' };
	private static final char[] ATTR_CX = new char[] { 'c', 'x' };
	private static final char[] ATTR_CY = new char[] { 'c', 'y' };
	private static final char[] ATTR_D = new char[] { 'd' };
	private static final char[] ATTR_FILL = new char[] { 'f', 'i', 'l', 'l' };
	private static final char[] ATTR_FILL_OPACITY = new char[] { 'f', 'i', 'l', 'l', '-', 'o', 'p', 'a', 'c', 'i', 't', 'y' };
	private static final char[] ATTR_FILL_RULE = new char[] { 'f', 'i', 'l', 'l', '-', 'r', 'u', 'l', 'e' };
	private static final char[] ATTR_FX = new char[] { 'f', 'x' };
	private static final char[] ATTR_FY = new char[] { 'f', 'y' };
	private static final char[] ATTR_GRADIENT_TRANSFORM = new char[] { 'g', 'r', 'a', 'd', 'i', 'e', 'n', 't', 'T', 'r', 'a', 'n', 's',
			'f', 'o', 'r', 'm' };
	private static final char[] ATTR_GRADIENT_UNITS = new char[] { 'g', 'r', 'a', 'd', 'i', 'e', 'n', 't', 'U', 'n', 'i', 't', 's' };
	private static final char[] ATTR_HEIGHT = new char[] { 'h', 'e', 'i', 'g', 'h', 't' };
	private static final char[] ATTR_ID = new char[] { 'i', 'd' };
	private static final char[] ATTR_OFFSET = new char[] { 'o', 'f', 'f', 's', 'e', 't' };
	private static final char[] ATTR_POINTS = new char[] { 'p', 'o', 'i', 'n', 't', 's' };
	private static final char[] ATTR_R = new char[] { 'r' };
	private static final char[] ATTR_RX = new char[] { 'r', 'x' };
	private static final char[] ATTR_RY = new char[] { 'r', 'y' };
	private static final char[] ATTR_SPREAD_METHOD = new char[] { 's', 'p', 'r', 'e', 'a', 'd', 'M', 'e', 't', 'h', 'o', 'd' };
	private static final char[] ATTR_STOP = new char[] { 's', 't', 'o', 'p' };
	private static final char[] ATTR_STOP_COLOR = new char[] { 's', 't', 'o', 'p', '-', 'c', 'o', 'l', 'o', 'r' };
	private static final char[] ATTR_STOP_OPACITY = new char[] { 's', 't', 'o', 'p', '-', 'o', 'p', 'a', 'c', 'i', 't', 'y' };
	private static final char[] ATTR_STROKE = new char[] { 's', 't', 'r', 'o', 'k', 'e' };
	private static final char[] ATTR_STROKE_OPACITY = new char[] { 's', 't', 'r', 'o', 'k', 'e', '-', 'o', 'p', 'a', 'c', 'i', 't', 'y' };
	private static final char[] ATTR_STROKE_WIDTH = new char[] { 's', 't', 'r', 'o', 'k', 'e', '-', 'w', 'i', 'd', 't', 'h' };
	private static final char[] ATTR_STROKE_CAP = new char[] { 's', 't', 'r', 'o', 'k', 'e', '-', 'l', 'i', 'n', 'e', 'c', 'a', 'p' };
	private static final char[] ATTR_STROKE_JOIN = new char[] { 's', 't', 'r', 'o', 'k', 'e', '-', 'l', 'i', 'n', 'e', 'j', 'o', 'i', 'n' };
	private static final char[] ATTR_STYLE = new char[] { 's', 't', 'y', 'l', 'e' };
	private static final char[] ATTR_TRANSFORM = new char[] { 't', 'r', 'a', 'n', 's', 'f', 'o', 'r', 'm' };
	private static final char[] ATTR_VIEWBOX = new char[] { 'v', 'i', 'e', 'w', 'B', 'o', 'x' };
	private static final char[] ATTR_WIDTH = new char[] { 'w', 'i', 'd', 't', 'h' };
	private static final char[] ATTR_X = new char[] { 'x' };
	private static final char[] ATTR_X1 = new char[] { 'x', '1' };
	private static final char[] ATTR_X2 = new char[] { 'x', '2' };
	private static final char[] ATTR_XLINK_HREF = new char[] { 'x', 'l', 'i', 'n', 'k', ':', 'h', 'r', 'e', 'f' };
	private static final char[] ATTR_Y = new char[] { 'y' };
	private static final char[] ATTR_Y1 = new char[] { 'y', '1' };
	private static final char[] ATTR_Y2 = new char[] { 'y', '2' };
	private static final char[] ELEMENT_CDATA = new char[] { '!', '[', 'C', 'D', 'A', 'T', 'A', '[' };
	private static final char[] ELEMENT_CDATA_END = new char[] { ']', ']', '>' };
	private static final char[] ELEMENT_CIRCLE = new char[] { 'c', 'i', 'r', 'c', 'l', 'e' };
	private static final char[] ELEMENT_COMMENT = new char[] { '!', '-', '-' };
	private static final char[] ELEMENT_COMMENT_END = new char[] { '-', '-', '>' };
	private static final char[] ELEMENT_DESCRIPTION = new char[] { 'd', 'e', 's', 'c' };
	private static final char[] ELEMENT_DEFS = new char[] { 'd', 'e', 'f', 's' };
	private static final char[] ELEMENT_DOCTYPE = new char[] { '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E' };
	private static final char[] ELEMENT_ELLIPSE = new char[] { 'e', 'l', 'l', 'i', 'p', 's', 'e' };
	private static final char[] ELEMENT_GROUP = new char[] { 'g' };
	private static final char[] ELEMENT_LINEAR_GRADIENT = new char[] { 'l', 'i', 'n', 'e', 'a', 'r', 'G', 'r', 'a', 'd', 'i', 'e', 'n', 't' };
	private static final char[] ELEMENT_LINE = new char[] { 'l', 'i', 'n', 'e' };
	private static final char[] ELEMENT_PATH = new char[] { 'p', 'a', 't', 'h' };
	private static final char[] ELEMENT_POLYGON = new char[] { 'p', 'o', 'l', 'y', 'g', 'o', 'n' };
	private static final char[] ELEMENT_POLYLINE = new char[] { 'p', 'o', 'l', 'y', 'l', 'i', 'n', 'e' };
	private static final char[] ELEMENT_RADIAL_GRADIENT = new char[] { 'r', 'a', 'd', 'i', 'a', 'l', 'G', 'r', 'a', 'd', 'i', 'e', 'n', 't' };
	private static final char[] ELEMENT_RECT = new char[] { 'r', 'e', 'c', 't' };
	private static final char[] ELEMENT_SVG = new char[] { 's', 'v', 'g' };
	private static final char[] ELEMENT_STYLE = new char[] { 's', 't', 'y', 'l', 'e' };
	private static final char[] ELEMENT_TITLE = new char[] { 't', 'i', 't', 'l', 'e' };
	private static final char[] ELEMENT_USE = new char[] { 'u', 's', 'e' };
	private static final char[] ELEMENT_XML = new char[] { '?', 'x', 'm', 'l' };

	//	private static final String paramRegex = "[^\\d^\\.^-]+"; //$NON-NLS-1$
	private static final String paramRegex = "[ ,]+"; //$NON-NLS-1$
	private static final Matcher urlMatcher = Pattern.compile(" *url\\( *#(\\w+) *\\) *").matcher(""); //$NON-NLS-1$  //$NON-NLS-2$

	
	private static void addArc(String[] sa, int ix, List<Byte> types, List<Float> points, boolean relative) {
		float x1 = points.get(points.size() - 2);
		float y1 = points.get(points.size() - 1);
		float rx = abs(Float.parseFloat(sa[ix++]));
		float ry = abs(Float.parseFloat(sa[ix++]));
		float phi = clampAngle(Float.parseFloat(sa[ix++]));
		boolean largeArc = (!sa[ix++].equals("0")); //$NON-NLS-1$
		boolean sweep = (!sa[ix++].equals("0")); //$NON-NLS-1$
		float x2 = Float.parseFloat(sa[ix++]);
		float y2 = Float.parseFloat(sa[ix++]);
		if(relative) {
			x2 += x1;
			y2 += y1;
		}

		if(x1 == x2 && y1 == y2) {
			return;
		}
		if(rx == 0 || ry == 0) {
			types.add((byte) SWT.PATH_LINE_TO);
			points.add(x2);
			points.add(y2);
			return;
		}

		double radPhi = toRadians(phi);

		double x0 = (cos(radPhi) * ((x1 - x2) / 2)) + (sin(radPhi) * ((y1 - y2) / 2));
		double y0 = (-sin(radPhi) * ((x1 - x2) / 2)) + (cos(radPhi) * ((y1 - y2) / 2));
		double lambda = ((x0 * x0) / (rx * rx)) + ((y0 * y0) / (ry * ry));
		double radicand;
		if(lambda > 1) {
			rx *= sqrt(lambda);
			ry *= sqrt(lambda);
			radicand = 0;
		} else {
			radicand = ((rx * rx * ry * ry) - (rx * rx * y0 * y0) - (ry * ry * x0 * x0)) / ((rx * rx * y0 * y0) + (ry * ry * x0 * x0));
		}
		if(radicand < 0) {
			rx *= sqrt(lambda);
			ry *= sqrt(lambda);
			radicand = 0;
		}
		int sign = (largeArc != sweep) ? 1 : -1;
		double cx0 = sign * sqrt(radicand) * rx * y0 / ry;
		double cy0 = sign * sqrt(radicand) * -ry * x0 / rx;
		double cx = (cos(radPhi) * cx0) - (sin(radPhi) * cy0) + ((x1 + x2) / 2);
		double cy = (sin(radPhi) * cx0) + (cos(radPhi) * cy0) + ((y1 + y2) / 2);

		double theta1 = getAngle(1, 0, (x0 - cx0) / rx, (y0 - cy0) / ry);
		double dTheta = getAngle((x0 - cx0) / rx, (y0 - cy0) / ry, (-x0 - cx0) / rx, (-y0 - cy0) / ry);
		double theta2 = theta1 + dTheta;
		theta1 = clampAngle(theta1);
		dTheta = clampAngle(dTheta);
		theta2 = clampAngle(theta2);

		if(!sweep) {
			dTheta = 360 - dTheta;
		}

		int increment = 5;
		int lines = round((float) dTheta) / increment;
		double theta = theta1;
		for(int i = 0; i < lines; i++) {
			sign = (sweep) ? 1 : -1;
			theta = clampAngle(theta + (sign * increment));

			double radTheta = toRadians(theta);
			double x = cos(radPhi) * rx * cos(radTheta) - sin(radPhi) * ry * sin(radTheta) + cx;
			double y = sin(radPhi) * rx * cos(radTheta) + cos(radPhi) * ry * sin(radTheta) + cy;

			types.add((byte) SWT.PATH_LINE_TO);
			if(i == lines - 1) {
				points.add(x2);
				points.add(y2);
			} else {
				points.add((float) x);
				points.add((float) y);
			}
		}
	}
	
	private static void addPoint(List<Float> points, String s, boolean relative) {
		addPoint(points, s, relative, 2);
	}
	
	private static void addPoint(List<Float> points, String s, boolean relative, int relativeOffset) {
		if(relative) {
			points.add(points.get(points.size() - relativeOffset) + Float.parseFloat(s));
		} else {
			points.add(new Float(s));
		}
	}
	
	private static double clampAngle(double deg) {
		if(deg < 0) {
			deg += 360;
		} else if(deg > 360) {
			deg -= 360;
		}
		return deg;
	}
	
	private static float clampAngle(float deg) {
		if(deg < 0) {
			deg += 360;
		} else if(deg > 360) {
			deg -= 360;
		}
		return deg;
	}
	
	private static int closer(char[] ca, int start, int end) {
		if(start >= 0) {
			char opener = ca[start];
			char closer = closerChar(opener);
			int count = 1;
			for(int i = start+1; i < ca.length && i <= end; i++) {
				if(ca[i] == opener && ca[i] != closer) {
					count++;
				} else if(ca[i] == closer) {
					if(closer != '"' || ca[i-1] != '\\') { // check for escape char
						count--;
						if(count == 0) {
							return i;
						}
					}
				} else if(ca[i] == '"') {
					i = closer(ca, i, end); // just entered a string - get out of it
				}
			}
		}
		return -1;
	}

	private static char closerChar(char c) {
		switch(c) {
			case '<': return '>';
			case '(': return ')';
			case '{': return '}';
			case '[': return ']';
			case '"': return '"';
			case '\'': return '\'';
		}
		return 0;
	}

	private static int findAll(char[] ca, int from, int to, char...cs) {
		for(int i = from; i >= 0 && i < ca.length && i <= to; i++) {
			if(ca[i] == cs[0]) {
				if(cs.length == 1) {
					return i;
				}
				for(int j = 1; j < cs.length && (i+j) <= to; j++) {
					if((i+j) == ca.length) {
						return -1;
					}
					if(ca[i+j] != cs[j]) {
						break;
					}
					if(j == cs.length-1) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	private static int findAny(char[] ca, int from, int to, char...cs) {
		for(int i = from; i >= 0 && i < ca.length && i <= to; i++) {
			for(char c : cs) {
				if(ca[i] == c) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * find the closer for the XML tag which begins with the given start position
	 * ('<' should be the first char)
	 * @param ca
	 * @param start
	 * @return
	 */
	private static int findClosingTag(char[] ca, int start, int end) {
		if(start >= 0 && start < ca.length && start < end) {
			int s1 = findAny(ca, start, end, ' ', '>');
			if(s1 != -1) {
				char[] opener = new char[s1-start];
				opener[0] = '<';
				char[] closer = new char[s1-start+2];
				closer[0] = '<';
				closer[1] = '/';
				closer[closer.length-1] = '>';
				int i = start+1;
				for( ; i < s1; i++) {
					opener[i-start] = ca[i];
					closer[i-start+1] = ca[i];
				}
				
				int count1 = 1;
				int count2 = 1;
				for( ; i < ca.length; i++) {
					if(ca[i] == '<') {
						count1++;
						if(isNext(ca, i, opener)) {
							count2++;
						} else if(isNext(ca, i, closer)) {
							count2--;
							if(count2 == 0) {
								return i;
							}
						} else if(isNext(ca, i, ELEMENT_CDATA)) {
							i = findAll(ca, i+ELEMENT_CDATA.length, end, ELEMENT_CDATA_END);
						}
					} else if(ca[i] == '>') {
						if(ca[i-1] == '/') {
							count1--;
						}
						if(count1 == 0) {
							return i;
						}
					} else if(ca[i] == '"') {
						i = closer(ca, i, end); // just entered a string - get out of it
					}
				}
			}
		}
		return -1;
	}
	
	private static int findNextTag(char[] ca, int start, int end) {
		int s1 = findAll(ca, start, end, '<');
		if(s1 != -1 && s1 < ca.length-1) {
			if(ca[s1+1] != '/') {
				return s1;
			} else {
				return findNextTag(ca, s1+1, end);
			}
		}
		return -1;
	}
	
	private static int forward(char[] ca, int from) {
		for(int i = from; i >= 0 && i < ca.length; i++) {
			if(!Character.isWhitespace(ca[i])) {
				return i;
			}
		}
		return -1;
	}
	
	private static double getAngle(double ux, double uy, double vx, double vy) {
		double dot = ux * vx + uy * vy;
		double au = hypot(ux, uy);
		double av = hypot(vx, vy);
		double alpha = dot / (au * av);
		if(alpha > 1) {
			alpha = 1;
		} else if(alpha < -1) {
			alpha = -1;
		}
		double theta = 180 * acos(alpha) / PI;
		if((ux * vy - uy * vx) < 0) {
			theta *= -1;
		}
		return theta;
	}

	private static String getAttrValue(char[] ca, int start, int end, char... name) {
		char[] search = new char[name.length + 2];
		System.arraycopy(name, 0, search, 1, name.length);
		search[0] = ' ';
		search[search.length - 1] = '=';
		int s1 = findAll(ca, start, end, search);
		if(s1 != -1) {
			s1 = findAll(ca, s1, end, '"');
			if(s1 != -1) {
				int s2 = closer(ca, s1, end);
				if(s1 != -1) {
					return new String(ca, s1 + 1, s2 - s1 - 1);
				}
			}
		}
		return null;
	}

	private static int[] getAttrValueRange(char[] ca, int start, int end, char... name) {
		char[] search = new char[name.length + 2];
		System.arraycopy(name, 0, search, 1, name.length);
		search[0] = ' ';
		search[search.length - 1] = '=';
		int s1 = findAll(ca, start, end, search);
		if(s1 != -1) {
			s1 = findAll(ca, s1, end, '"');
			if(s1 != -1) {
				int s2 = closer(ca, s1, end);
				if(s1 != -1) {
					return new int[] { s1 + 1, s2 - 1 };
				}
			}
		}
		return new int[] { -1, -1 };
	}

	private static Map<String, String> getClassStyles(SvgElement element, char[] ca, int start, int end) {
		String s = getAttrValue(ca, start, end, ATTR_CLASS);
		if(s != null) {
			Map<String, String> styles = new HashMap<String, String>();
			String[] classes = s.trim().split(" +"); //$NON-NLS-1$
			for(String c : classes) {
				Map<String, String> pairs = element.getFragment().getStyles("." + c); //$NON-NLS-1$
				if(pairs != null) {
					styles.putAll(pairs);
				}
			}
			return styles;
		}
		return new HashMap<String, String>(0);
	}

	private static Integer getColorAsInt(String color) {
		if(color != null) {
			if(SvgColors.contains(color)) {
				return SvgColors.get(color);
			} else if('#' == color.charAt(0)) {
				if(color.length() == 4) {
					char[] ca = new char[6];
					ca[0] = color.charAt(1);
					ca[1] = color.charAt(1);
					ca[2] = color.charAt(2);
					ca[3] = color.charAt(2);
					ca[4] = color.charAt(3);
					ca[5] = color.charAt(3);
					return Integer.parseInt(new String(ca), 16);
				} else if(color.length() == 7) {
					return Integer.parseInt(color.substring(1), 16);
				}
			}
		}
		return null;
	}

	private static Map<String, String> getIdStyles(SvgElement element, char[] ca, int start, int end) {
		String s = element.getId();
		if(s != null) {
			Map<String, String> styles = new HashMap<String, String>();
			Map<String, String> pairs = element.getFragment().getStyles("#" + s); //$NON-NLS-1$
			if(pairs != null) {
				styles.putAll(pairs);
			}
			return styles;
		}
		return new HashMap<String, String>(0);
	}

	private static String getLink(String link) {
		urlMatcher.reset(link);
		if(urlMatcher.matches()) {
			return urlMatcher.group(1);
		}
		return null;
	}

	/**
	 * Types:
	 * <ul>
	 * <li>matrix(<a> <b> <c> <d> <e> <f>)</li>
	 * <li>translate(<tx> [<ty>])</li>
	 * <li>scale(<sx> [<sy>])</li>
	 * <li>rotate(<rotate-angle> [<cx> <cy>])</li>
	 * <li>skewX(<skew-angle>)</li>
	 * <li>skewY(<skew-angle>)</li>
	 * </ul>
	 * 
	 * @param str
	 * @return
	 */
	private static SvgTransform getTransform(char[] ca, int[] range) {
		int s1 = range[0];

		SvgTransform first = null;
		SvgTransform transform = null;
		while(s1 != -1 && s1 < range[1]) {
			int s2 = findAll(ca, s1, range[1], '(');
			int s3 = findAll(ca, s2, range[1], ')');
			if(s1 != -1 && s2 != -1 && s3 != -1) {
				if(transform == null) {
					first = transform = new SvgTransform();
				} else {
					transform.next = new SvgTransform();
					transform = transform.next;
				}
				if(isEqual(ca, s1, s2 - 1, "matrix".toCharArray())) { //$NON-NLS-1$
					transform.setData(Type.Matrix, new String(ca, s2 + 1, s3 - s2 - 1).split(paramRegex));
				} else if(isEqual(ca, s1, s2 - 1, "translate".toCharArray())) { //$NON-NLS-1$
					transform.setData(Type.Translate, new String(ca, s2 + 1, s3 - s2 - 1).split(paramRegex));
				} else if(isEqual(ca, s1, s2 - 1, "scale".toCharArray())) { //$NON-NLS-1$
					transform.setData(Type.Scale, new String(ca, s2 + 1, s3 - s2 - 1).split(paramRegex));
				} else if(isEqual(ca, s1, s2 - 1, "rotate".toCharArray())) { //$NON-NLS-1$
					transform.setData(Type.Rotate, new String(ca, s2 + 1, s3 - s2 - 1).split(paramRegex));
				} else if(isEqual(ca, s1, s2 - 1, "skewx".toCharArray())) { //$NON-NLS-1$
					transform.setData(Type.SkewX, new String(ca, s2 + 1, s3 - s2 - 1).split(paramRegex));
				} else if(isEqual(ca, s1, s2 - 1, "skewy".toCharArray())) { //$NON-NLS-1$
					transform.setData(Type.SkewY, new String(ca, s2 + 1, s3 - s2 - 1).split(paramRegex));
				}
			}
			s1 = forward(ca, s3 + 1);
		}

		if(first != null) {
			return first;
		} else {
			return new SvgTransform();
		}
	}

	private static String getValue(String name, Map<String, String> idStyles, Map<String, String> classStyles,
			Map<String, String> attrStyles, String attrValue) {
		return getValue(name, idStyles, classStyles, attrStyles, attrValue, null);
	}

	private static String getValue(String name, Map<String, String> idStyles, Map<String, String> classStyles,
			Map<String, String> attrStyles, String attrValue, String defaultValue) {
		if(attrValue != null) {
			return attrValue;
		}
		if(attrStyles.containsKey(name)) {
			return attrStyles.get(name);
		}
		if(classStyles.containsKey(name)) {
			return classStyles.get(name);
		}
		if(idStyles.containsKey(name)) {
			return idStyles.get(name);
		}
		return defaultValue;
	}

	private static boolean isEqual(char[] ca, int start, int end, char...test) {
		if(test.length != (end-start+1)) {
			return false;
		}
		for(int i = start, j = 0; i < end && j < test.length; i++, j++) {
			if(ca[i] != test[j]) {
				return false;
			}
		}
		return true;
	}

	private static boolean isNext(char[] ca, int start, char...test) {
		for(int i = start, j = 0; j < test.length; i++, j++) {
			if(ca[i] != test[j]) {
				return false;
			}
		}
		return true;
	}

	private static boolean isTag(char[] ca, int start, char[] tagName) {
		if(start >= 0 && start < ca.length && ca[start] == '<' ) {
			int i = start + 1;
			for(int j = 0; i < ca.length && j < tagName.length; i++, j++) {
				if(ca[i] != tagName[j]) {
					return false;
				}
			}
			if(i < ca.length) {
				return (ca[i] == ' ') || (ca[i] == '>');
			}
		}
		return false;
	}

	static SvgDocument load(InputStream in) {
		StringBuilder sb = new StringBuilder();
		BufferedInputStream bis = new BufferedInputStream(in);
		int i;
		try {
			while((i = bis.read()) != -1) {
				char c = (char) i;
				if(Character.isWhitespace(c)) { // replace all whitespace chars with a space char
					if(' ' != sb.charAt(sb.length() - 1)) { // no point in having multiple spaces
						sb.append(' ');
					}
				} else {
					sb.append(c);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		char[] ca = new char[sb.length()];
		sb.getChars(0, sb.length(), ca, 0);
		SvgDocument doc = new SvgDocument();
		parse(doc, ca, 0, ca.length - 1);
		return doc;
	}

	static SvgDocument load(String src) {
		SvgDocument doc = new SvgDocument();
		parse(doc, src.toCharArray(), 0, src.length() - 1);
		return doc;
	}

	private static void parse(SvgContainer container, char[] ca, int start, int end) {
		int s1 = start;
		while(s1 != -1 && s1 < end) {
			s1 = findNextTag(ca, s1, end);
			if(isTag(ca, s1, ELEMENT_GROUP)) {
				s1 = parseGroup(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_TITLE)) {
				s1 = parseTitle(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_DESCRIPTION)) {
				s1 = parseDescription(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_SVG)) {
				s1 = parseSvg(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_STYLE)) {
				s1 = parseStyle(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_USE)) {
				s1 = parseUse(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_PATH)) {
				s1 = parsePath(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_CIRCLE)) {
				s1 = parseCircle(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_ELLIPSE)) {
				s1 = parseEllipse(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_LINE)) {
				s1 = parseLine(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_POLYGON)) {
				s1 = parsePolygon(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_POLYLINE)) {
				s1 = parsePolyline(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_RECT)) {
				s1 = parseRectangle(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_LINEAR_GRADIENT)) {
				s1 = parseLinearGradient(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_RADIAL_GRADIENT)) {
				s1 = parseRadialGradient(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_DEFS)) {
				s1 = parseDefs(container, ca, s1, end);
			} else if(isTag(ca, s1, ELEMENT_XML) || isTag(ca, s1, ELEMENT_DOCTYPE)) {
				s1 = findAll(ca, s1, end, '>');
			} else if(isTag(ca, s1, ELEMENT_COMMENT)) {
				s1 = findAll(ca, s1, end, ELEMENT_COMMENT_END);
			} else {
				if(s1 != -1) {
					int s2 = findAny(ca, s1, end, ' ', '>');
					System.out.println("dunno: " + new String(ca, s1 + 1, s2 - s1 - 1)); //$NON-NLS-1$
				}
				s1 = findClosingTag(ca, s1, end);
			}
		}
	}

	private static int parseCircle(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			element.pathData = new PathData();
			element.pathData.points = new float[4];
			element.pathData.points[0] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_CX), 0);
			element.pathData.points[1] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_CY), 0);
			element.pathData.points[2] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_R));
			element.pathData.points[3] = element.pathData.points[2];
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	private static void parseCss(SvgStyle element, char[] ca, int start, int end) {
		Map<String, Map<String, String>> styles = new HashMap<String, Map<String, String>>();

		int s1 = forward(ca, start);
		int s = findAll(ca, s1, end, '{');
		int s2 = reverse(ca, s - 1);
		String names;

		while(s1 != -1 && s2 != -1) {
			names = new String(ca, s1, s2 - s1 + 1);
			s2 = closer(ca, s, end);
			if(s2 != -1) {
				Map<String, String> pairs = parseStyles(ca, s + 1, s2 - 1);
				for(String name : names.split(" *, *")) { //$NON-NLS-1$
					Map<String, String> existing = styles.get(name);
					if(existing != null) {
						Map<String, String> m = new HashMap<String, String>();
						m.putAll(existing);
						m.putAll(pairs);
						styles.put(name, m);
					} else {
						styles.put(name, pairs);
					}
				}
				s1 = forward(ca, s2 + 1);
				s = findAll(ca, s1, end, '{');
				s2 = reverse(ca, s - 1);
			}
		}

		element.styles = styles;
	}

	private static int parseDefs(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgContainer element = new SvgContainer(container, "defs"); //$NON-NLS-1$
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
			parse(element, ca, endAttrs, end);
		}
		return end;
	}

	private static int parseDescription(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			start = closer(ca, start, end);
			if(start != -1) {
				container.description = new String(ca, start + 1, end - start - 1);
			}
		}
		return end;
	}

	private static int parseEllipse(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			element.pathData = new PathData();
			element.pathData.points = new float[4];
			element.pathData.points[0] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_CX), 0);
			element.pathData.points[1] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_CY), 0);
			element.pathData.points[2] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_RX));
			element.pathData.points[3] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_RY));
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	private static void parseFill(SvgGraphic element, char[] ca, int start, int end) {
		Map<String, String> idStyles = getIdStyles(element, ca, start, end);
		Map<String, String> classStyles = getClassStyles(element, ca, start, end);
		Map<String, String> attrStyles = parseStyles(getAttrValue(ca, start, end, ATTR_STYLE));

		String s = getValue("fill", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_FILL)); //$NON-NLS-1$
		parsePaint(element.fill, s);

		s = getValue("fill-opacity", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_FILL_OPACITY)); //$NON-NLS-1$
		element.fill.opacity = parseFloat(s);

		s = getValue("fill-rule", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_FILL_RULE)); //$NON-NLS-1$
		element.fill.rule = parseRule(s);
	}

	private static Float parseFloat(String s) {
		return parseFloat(s, null);
	}

	private static float parseFloat(String s, float defaultValue) {
		if(s == null) {
			return defaultValue;
		} else {
			return Float.parseFloat(s);
		}
	}

	private static Float parseFloat(String s, Float defaultValue) {
		if(s == null) {
			return defaultValue;
		} else {
			if (s.endsWith("px")) {
				s = s.substring(0, s.length() - 2);
			}
			return Float.parseFloat(s);
		}
	}

	private static int parseGradientStop(SvgGradient gradient, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgGradientStop stop = new SvgGradientStop(gradient, getAttrValue(ca, start, endAttrs, ATTR_ID));

			Map<String, String> idStyles = getIdStyles(stop, ca, start, endAttrs);
			Map<String, String> classStyles = getClassStyles(stop, ca, start, endAttrs);
			Map<String, String> attrStyles = parseStyles(getAttrValue(ca, start, endAttrs, ATTR_STYLE));

			String s = getValue("offset", idStyles, classStyles, attrStyles, getAttrValue(ca, start, endAttrs, ATTR_OFFSET)); //$NON-NLS-1$
			stop.offset = parsePercentage(s, 0f, true);

			s = getValue("stop-color", idStyles, classStyles, attrStyles, getAttrValue(ca, start, endAttrs, ATTR_STOP_COLOR)); //$NON-NLS-1$
			stop.color = getColorAsInt(s);

			s = getValue("stop-opacity", idStyles, classStyles, attrStyles, getAttrValue(ca, start, endAttrs, ATTR_STOP_OPACITY), "1"); //$NON-NLS-1$  //$NON-NLS-2$
			stop.opacity = parseFloat(s);

			gradient.stops.add(stop);
		}
		return end;
	}

	private static int parseGroup(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgContainer element = new SvgContainer(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
			parse(element, ca, endAttrs, end);
		}
		return end;
	}

	// cm, em, ex, in, mm, pc, pt, px 
	private static float parseLength(String s, String defaultString) {
		if(s == null) {
			s = defaultString;
		}
		if(s.endsWith("%")) { //$NON-NLS-1$
			throw new UnsupportedOperationException("TODO parseLength: %"); //$NON-NLS-1$
		} else if(s.endsWith("cm")) { //$NON-NLS-1$
			final Point dpi = new Point(0, 0);
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					dpi.x = Display.getDefault().getDPI().x;
				}
			});
			return Float.parseFloat(s.substring(0, s.length() - 2)) * dpi.x * 0.393700787f;
		} else if(s.endsWith("em")) { //$NON-NLS-1$
			throw new UnsupportedOperationException("TODO parseLength: em"); //$NON-NLS-1$
		} else if(s.endsWith("ex")) { //$NON-NLS-1$
			throw new UnsupportedOperationException("TODO parseLength: ex"); //$NON-NLS-1$
		} else if(s.endsWith("in")) { //$NON-NLS-1$
			final Point dpi = new Point(0, 0);
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					dpi.x = Display.getDefault().getDPI().x;
				}
			});
			return Float.parseFloat(s.substring(0, s.length() - 2)) * dpi.x;
		} else if(s.endsWith("mm")) { //$NON-NLS-1$
			final Point dpi = new Point(0, 0);
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					dpi.x = Display.getDefault().getDPI().x;
				}
			});
			return Float.parseFloat(s.substring(0, s.length() - 2)) * dpi.x * 0.0393700787f;
		} else if(s.endsWith("pc")) { //$NON-NLS-1$
			throw new UnsupportedOperationException("TODO parseLength: pc"); //$NON-NLS-1$
		} else if(s.endsWith("pt")) { //$NON-NLS-1$
			throw new UnsupportedOperationException("TODO parseLength: pt"); //$NON-NLS-1$
		} else if(s.endsWith("px")) { //$NON-NLS-1$
			return Float.parseFloat(s.substring(0, s.length() - 2));
		} else {
			return Float.parseFloat(s);
		}
	}

	private static int parseLine(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			element.pathData = new PathData();
			element.pathData.types = new byte[2];
			element.pathData.points = new float[4];
			element.pathData.types[0] = (byte)SWT.PATH_MOVE_TO;
			element.pathData.points[0] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_X1), 0);
			element.pathData.points[1] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_Y1), 0);
			element.pathData.types[1] = (byte)SWT.PATH_LINE_TO;
			element.pathData.points[2] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_X2), 0);
			element.pathData.points[3] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_Y2), 0);
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
		}
		return end;
	}

	private static int parseLinearGradient(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgGradient gradient = new SvgGradient(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			gradient.data = new float[4];
			gradient.data[0] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_X1), 0f, false);
			gradient.data[1] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_Y1), 0f, false);
			gradient.data[2] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_X2), 1f, false);
			gradient.data[3] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_Y2), 0f, false);
			gradient.setLinkId(getAttrValue(ca, start, endAttrs, ATTR_XLINK_HREF));
			gradient.setSpreadMethod(getAttrValue(ca, start, endAttrs, ATTR_SPREAD_METHOD));
			gradient.setUnits(getAttrValue(ca, start, endAttrs, ATTR_GRADIENT_UNITS));
			gradient.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_GRADIENT_TRANSFORM));
			int s1 = endAttrs;
			while(s1 != -1 && s1 < end) {
				s1 = findNextTag(ca, s1 + 1, end);
				if(isNext(ca, s1 + 1, ATTR_STOP)) {
					s1 = parseGradientStop(gradient, ca, s1, end);
				}
			}
		}
		return end;
	}

	private static String parseLinkId(String id) {
		if(id != null && id.length() > 2 && '#' == id.charAt(0)) {
			return id.substring(1);
		}
		return null;
	}

	private static void parsePaint(SvgPaint paint, String s) {
		if(s != null) {
			if("none".equals(s)) { //$NON-NLS-1$
				paint.type = PaintType.None;
			} else if("currentColor".equals(s)) { //$NON-NLS-1$
				paint.type = PaintType.Current;
			} else if(s.startsWith("url")) { //$NON-NLS-1$
				paint.type = PaintType.Link;
				paint.linkId = getLink(s);
			} else {
				Integer i = getColorAsInt(s);
				if(i != null) {
					paint.type = PaintType.Color;
					paint.color = i;
				} else {
					paint.type = PaintType.None;
					System.out.println("dunno fill " + paint); //$NON-NLS-1$
				}
			}
		}
	}

	private static int parsePath(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			parsePathData(element, getAttrValue(ca, start, endAttrs, ATTR_D));
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	public static void parsePathData(SvgShape path, String data) {
		String[] sa = parsePathDataStrings(data);
		boolean relative;
		List<Byte> types = new ArrayList<Byte>();
		List<Float> points = new ArrayList<Float>();
		int i = -1;
		String lastCommand = ""; //$NON-NLS-1$
		boolean useLastCommand = false;
		while(i < sa.length - 1) {
			String command;
			if(!useLastCommand) {
				i++;
				command = sa[i];
			} else {
				i--;
				command = lastCommand;
				useLastCommand = false;
			}
			switch(command.charAt(0)) {
			case 'M':
			case 'm':
				types.add((byte) SWT.PATH_MOVE_TO);
				relative = ('m' == command.charAt(0));
				addPoint(points, sa[++i], relative);
				addPoint(points, sa[++i], relative);
				break;
			case 'L':
			case 'l':
				types.add((byte) SWT.PATH_LINE_TO);
				relative = ('l' == command.charAt(0));
				addPoint(points, sa[++i], relative);
				addPoint(points, sa[++i], relative);
				break;
			case 'H':
			case 'h':
				types.add((byte) SWT.PATH_LINE_TO);
				relative = ('h' == command.charAt(0));
				addPoint(points, sa[++i], relative);
				points.add(points.get(points.size() - 2));
				break;
			case 'V':
			case 'v':
				types.add((byte) SWT.PATH_LINE_TO);
				relative = ('v' == command.charAt(0));
				points.add(points.get(points.size() - 2));
				addPoint(points, sa[++i], relative);
				break;
			case 'C':
			case 'c':
				types.add((byte) SWT.PATH_CUBIC_TO);
				relative = ('c' == command.charAt(0));
				addPoint(points, sa[++i], relative);
				addPoint(points, sa[++i], relative);
				addPoint(points, sa[++i], relative, 4);
				addPoint(points, sa[++i], relative, 4);
				addPoint(points, sa[++i], relative, 6);
				addPoint(points, sa[++i], relative, 6);
				break;
			case 'S':
			case 's':
				types.add((byte) SWT.PATH_CUBIC_TO);
				relative = ('s' == command.charAt(0));
				if(SWT.PATH_CUBIC_TO == types.get(types.size() - 2)) {
					float x2 = points.get(points.size() - 4);
					float y2 = points.get(points.size() - 3);
					float x = points.get(points.size() - 2);
					float y = points.get(points.size() - 1);
					float x1 = 2 * x - x2;
					float y1 = 2 * y - y2;
					points.add(x1);
					points.add(y1);
				} else {
					points.add(points.get(points.size() - 2));
					points.add(points.get(points.size() - 2));
				}
				addPoint(points, sa[++i], relative, 4);
				addPoint(points, sa[++i], relative, 4);
				addPoint(points, sa[++i], relative, 6);
				addPoint(points, sa[++i], relative, 6);
				break;
			case 'Q':
			case 'q':
				types.add((byte) SWT.PATH_QUAD_TO);
				relative = ('q' == command.charAt(0));
				addPoint(points, sa[++i], relative);
				addPoint(points, sa[++i], relative);
				addPoint(points, sa[++i], relative, 4);
				addPoint(points, sa[++i], relative, 4);
				break;
			case 'T':
			case 't':
				types.add((byte) SWT.PATH_QUAD_TO);
				relative = ('t' == command.charAt(0));
				if(SWT.PATH_QUAD_TO == types.get(types.size() - 2)) {
					float x2 = points.get(points.size() - 4);
					float y2 = points.get(points.size() - 3);
					float x = points.get(points.size() - 2);
					float y = points.get(points.size() - 1);
					float x1 = 2 * x - x2;
					float y1 = 2 * y - y2;
					points.add(x1);
					points.add(y1);
				} else {
					points.add(points.get(points.size() - 2));
					points.add(points.get(points.size() - 2));
				}
				addPoint(points, sa[++i], relative, 4);
				addPoint(points, sa[++i], relative, 4);
				break;
			case 'Z':
			case 'z':
				types.add((byte) SWT.PATH_CLOSE);
				break;
			case 'A':
			case 'a':
				relative = ('a' == command.charAt(0));
				addArc(sa, ++i, types, points, relative);
				i += 6;
				break;
			default:
				char com = lastCommand.charAt(0);
				if(com != 'Z' && com != 'z') {
					useLastCommand = true;
					if(com == 'M') {
						lastCommand = "L"; //$NON-NLS-1$
					}
					if(com == 'm') {
						lastCommand = "l"; //$NON-NLS-1$
					}
				}
			}
			if(!useLastCommand) {
				lastCommand = command;
			}
		}

		path.pathData = new PathData();
		path.pathData.types = new byte[types.size()];
		for(i = 0; i < types.size(); i++) {
			path.pathData.types[i] = types.get(i).byteValue();
		}
		path.pathData.points = new float[points.size()];
		for(i = 0; i < points.size(); i++) {
			path.pathData.points[i] = points.get(i).floatValue();
		}
	}

	private static String[] parsePathDataStrings(String data) {
		List<String> strs = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		char[] ca = data.toCharArray();
		for(int i = 0; i < ca.length; i++) {
			char c = ca[i];
			if('e' == c) {
				sb.append(c);
				if(i < ca.length - 1 && ca[i + 1] == '-') {
					i++;
					sb.append(ca[i]);
				}
			} else if(Character.isLetter(c)) {
				if(sb != null && sb.length() > 0) {
					strs.add(sb.toString());
				}
				strs.add(Character.toString(c));
				sb = new StringBuilder();
			} else if('.' == c || Character.isDigit(c)) {
				sb.append(c);
			} else {
				if(sb != null && sb.length() > 0) {
					strs.add(sb.toString());
				}
				sb = new StringBuilder();
				if('-' == c) {
					sb.append(c);
				}
			}
		}
		if(sb != null && sb.length() > 0) {
			strs.add(sb.toString());
		}
		return strs.toArray(new String[strs.size()]);
	}

	private static Float parsePercentage(String s, Float defaultValue, boolean clamp) {
		if(s != null) {
			Float offset;
			if('%' == s.charAt(s.length() - 1)) {
				offset = Float.parseFloat(s.substring(0, s.length() - 1)) / 100;
			} else {
				offset = Float.parseFloat(s);
			}
			if(clamp) {
				if(offset > 1) {
					offset = new Float(1);
				} else if(offset < 0) {
					offset = new Float(0);
				}
			}
			return offset;
		}
		return defaultValue;
	}

	private static float[] parsePoints(String s) {
		if(s != null) {
			String[] sa = s.trim().split("[ ,]"); //$NON-NLS-1$
			float[] points = new float[sa.length];
			for(int i = 0; i < sa.length; i++) {
				points[i] = parseFloat(sa[i]);
			}
			return points;
		}
		return new float[0];
	}

	private static int parsePolygon(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			float[] linePoints = parsePoints(getAttrValue(ca, start, endAttrs, ATTR_POINTS));
			element.pathData = new PathData();
			element.pathData.types = new byte[1+(linePoints.length/2)];
			element.pathData.points = new float[linePoints.length];
			element.pathData.types[0] = (byte)SWT.PATH_MOVE_TO;
			element.pathData.points[0] = linePoints[0];
			element.pathData.points[1] = linePoints[1];
			int i = 2, j = 1;
			while(i < linePoints.length-1) {
				element.pathData.types[j++] = (byte)SWT.PATH_LINE_TO;
				element.pathData.points[i] = linePoints[i++];
				element.pathData.points[i] = linePoints[i++];
			}
			element.pathData.types[element.pathData.types.length-1] = (byte)SWT.PATH_CLOSE;
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	private static int parsePolyline(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			float[] linePoints = parsePoints(getAttrValue(ca, start, endAttrs, ATTR_POINTS));
			element.pathData = new PathData();
			element.pathData.types = new byte[linePoints.length/2];
			element.pathData.points = new float[linePoints.length];
			element.pathData.types[0] = (byte)SWT.PATH_MOVE_TO;
			element.pathData.points[0] = linePoints[0];
			element.pathData.points[1] = linePoints[1];
			int i = 2, j = 1;
			while(i < linePoints.length-1) {
				element.pathData.types[j++] = (byte)SWT.PATH_LINE_TO;
				element.pathData.points[i] = linePoints[i++];
				element.pathData.points[i] = linePoints[i++];
			}
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	private static int parseRadialGradient(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgGradient gradient = new SvgGradient(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			gradient.data = new float[5];
			gradient.data[0] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_CX), null, false);
			gradient.data[1] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_CY), null, false);
			gradient.data[2] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_FX), gradient.data[0], false);
			gradient.data[3] = parsePercentage(getAttrValue(ca, start, endAttrs, ATTR_FY), gradient.data[1], false);
			gradient.data[4] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_R));
			gradient.setLinkId(getAttrValue(ca, start, endAttrs, ATTR_XLINK_HREF));
			gradient.setSpreadMethod(getAttrValue(ca, start, endAttrs, ATTR_SPREAD_METHOD));
			gradient.setUnits(getAttrValue(ca, start, endAttrs, ATTR_GRADIENT_UNITS));
			gradient.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_GRADIENT_TRANSFORM));
			int s1 = endAttrs;
			while(s1 != -1 && s1 < end) {
				s1 = findNextTag(ca, s1 + 1, end);
				if(isNext(ca, s1 + 1, ATTR_STOP)) {
					s1 = parseGradientStop(gradient, ca, s1, end);
				}
			}
		}
		return end;
	}

	private static int parseRectangle(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgShape element = new SvgShape(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			element.pathData = new PathData();
			element.pathData.points = new float[6];
			element.pathData.points[0] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_X));
			element.pathData.points[1] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_Y));
			element.pathData.points[2] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_WIDTH));
			element.pathData.points[3] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_HEIGHT));
			element.pathData.points[4] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_RX), 0);
			element.pathData.points[5] = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_RY), 0);
			parseFill(element, ca, start, endAttrs);
			parseStroke(element, ca, start, endAttrs);
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	private static Integer parseRule(String s) {
		if(s != null) {
			if("evenodd".equals(s)) { //$NON-NLS-1$
				return SWT.FILL_EVEN_ODD;
			} else if("winding".equals(s)) { //$NON-NLS-1$
				return SWT.FILL_WINDING;
			}
		}
		return null;
	}

	private static void parseStroke(SvgGraphic element, char[] ca, int start, int end) {
		Map<String, String> idStyles = getIdStyles(element, ca, start, end);
		Map<String, String> classStyles = getClassStyles(element, ca, start, end);
		Map<String, String> attrStyles = parseStyles(getAttrValue(ca, start, end, ATTR_STYLE));

		String s = getValue("stroke", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_STROKE)); //$NON-NLS-1$
		parsePaint(element.stroke, s);

		s = getValue("stroke-opacity", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_STROKE_OPACITY)); //$NON-NLS-1$
		element.stroke.opacity = parseFloat(s);

		s = getValue("stroke-width", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_STROKE_WIDTH)); //$NON-NLS-1$
		element.stroke.width = parseStrokeWidth(s);

		s = getValue("stroke-linecap", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_STROKE_CAP)); //$NON-NLS-1$
		element.stroke.lineCap = parseStrokeLineCap(s);

		s = getValue("stroke-linejoin", idStyles, classStyles, attrStyles, getAttrValue(ca, start, end, ATTR_STROKE_JOIN)); //$NON-NLS-1$
		element.stroke.lineJoin = parseStrokeLineJoin(s);
	}

	private static Integer parseStrokeLineCap(String s) {
		if(s != null) {
			if("butt".equals(s)) { //$NON-NLS-1$
				return SWT.CAP_FLAT;
			} else if("round".equals(s)) { //$NON-NLS-1$
				return SWT.CAP_ROUND;
			} else if("square".equals(s)) { //$NON-NLS-1$
				return SWT.CAP_SQUARE;
			}
		}
		return null;
	}

	private static Integer parseStrokeLineJoin(String s) {
		if(s != null) {
			if("bevel".equals(s)) { //$NON-NLS-1$
				return SWT.JOIN_BEVEL;
			} else if("miter".equals(s)) { //$NON-NLS-1$
				return SWT.JOIN_MITER;
			} else if("round".equals(s)) { //$NON-NLS-1$
				return SWT.JOIN_ROUND;
			}
		}
		return null;
	}

	private static Float parseStrokeWidth(String s) {
		if(s != null) {
			if(s.endsWith("px")) { //$NON-NLS-1$
				return new Float(s.substring(0, s.length() - 2));
			} else {
				return new Float(s);
			}
		}
		return null;
	}

	private static int parseStyle(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endData = closer(ca, start, end);
			SvgStyle element = new SvgStyle(container);
			int cd1 = findAll(ca, start, end, ELEMENT_CDATA);
			if(cd1 != -1) {
				start = cd1 + ELEMENT_CDATA.length;
				endData = findAll(ca, start, end, ELEMENT_CDATA_END);
			} else {
				start = endData + 1;
				endData = end;
			}
			parseCss(element, ca, start, endData);
		}
		return end;
	}

	private static Map<String, String> parseStyles(char[] ca, int start, int end) {
		Map<String, String> styles = new HashMap<String, String>();
		int len = end - start + 1;
		if(len > 0 && start + len <= ca.length) {
			String[] sa = new String(ca, start, end - start + 1).trim().split(" *; *"); //$NON-NLS-1$
			for(String s : sa) {
				String[] sa2 = s.split(" *: *"); //$NON-NLS-1$
				if(sa2.length == 2) {
					styles.put(sa2[0], sa2[1]);
				}
			}
		}
		return styles;
	}

	private static Map<String, String> parseStyles(String styles) {
		if(styles != null) {
			return parseStyles(styles.toCharArray(), 0, styles.length() - 1);
		}
		return new HashMap<String, String>(0);
	}

	private static int parseSvg(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgFragment element = new SvgFragment(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			if(container != null) {
				// x and y have no effect on outermost svg fragments
				element.x = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_X));
				element.y = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_Y));
			}
			element.width = parseLength(getAttrValue(ca, start, endAttrs, ATTR_WIDTH), "10px"); //$NON-NLS-1$
			element.height = parseLength(getAttrValue(ca, start, endAttrs, ATTR_HEIGHT), "10px"); //$NON-NLS-1$
			element.viewBox = parseViewBox(getAttrValue(ca, start, endAttrs, ATTR_VIEWBOX));
			//			TODO element.preserveAspectRatio = 
			parse(element, ca, endAttrs, end);
		}
		return end;
	}

	private static int parseTitle(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			start = closer(ca, start, end);
			if(start != -1) {
				container.title = new String(ca, start + 1, end - start - 1);
			}
		}
		return end;
	}

	private static int parseUse(SvgContainer container, char[] ca, int start, int end) {
		end = findClosingTag(ca, start, end);
		if(end != -1) {
			int endAttrs = closer(ca, start, end);
			SvgUse element = new SvgUse(container, getAttrValue(ca, start, endAttrs, ATTR_ID));
			element.x = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_X), 0);
			element.y = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_Y), 0);
			element.w = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_WIDTH));
			element.h = parseFloat(getAttrValue(ca, start, endAttrs, ATTR_HEIGHT));
			element.linkId = parseLinkId(getAttrValue(ca, start, endAttrs, ATTR_XLINK_HREF));
			element.transform = getTransform(ca, getAttrValueRange(ca, start, endAttrs, ATTR_TRANSFORM));
		}
		return end;
	}

	private static float[] parseViewBox(String s) {
		if(s != null) {
			float[] vb = new float[4];
			String[] sa = s.split(paramRegex);
			if(sa.length == 4) {
				vb[0] = Float.parseFloat(sa[0]);
				vb[1] = Float.parseFloat(sa[1]);
				vb[2] = Float.parseFloat(sa[2]);
				vb[3] = Float.parseFloat(sa[3]);
				return vb;
			}
		}
		return null;
	}

	private static int reverse(char[] ca, int from) {
		for(int i = from; i >= 0 && i < ca.length; i--) {
			if(!Character.isWhitespace(ca[i])) {
				return i;
			}
		}
		return -1;
	}

	private SvgLoader() {
		// class should never be instantiated
	}

}
