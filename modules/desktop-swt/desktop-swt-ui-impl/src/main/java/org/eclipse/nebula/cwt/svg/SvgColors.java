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

class SvgColors {
	
	private static final Map<String, Integer> colors = new HashMap<String, Integer>(147);
	static {
		colors.put("aliceblue", 0xf0f8ff); //$NON-NLS-1$
		colors.put("antiquewhite", 0xfaebd7); //$NON-NLS-1$
		colors.put("aqua", 0x00ffff); //$NON-NLS-1$
		colors.put("aquamarine", 0x7fffd4); //$NON-NLS-1$
		colors.put("azure", 0xf0ffff); //$NON-NLS-1$
		colors.put("beige", 0xf5f5dc); //$NON-NLS-1$
		colors.put("bisque", 0xffe4c4); //$NON-NLS-1$
		colors.put("black", 0x000000); //$NON-NLS-1$
		colors.put("blanchedalmond", 0xffebcd); //$NON-NLS-1$
		colors.put("blue", 0x0000ff); //$NON-NLS-1$
		colors.put("blueviolet", 0x8a2be2); //$NON-NLS-1$
		colors.put("brown", 0xa52a2a); //$NON-NLS-1$
		colors.put("burlywood", 0xdeb887); //$NON-NLS-1$
		colors.put("cadetblue", 0x5f9ea0); //$NON-NLS-1$
		colors.put("chartreuse", 0x7fff00); //$NON-NLS-1$
		colors.put("chocolate", 0xd2691e); //$NON-NLS-1$
		colors.put("coral", 0xff7f50); //$NON-NLS-1$
		colors.put("cornflowerblue", 0x6495ed); //$NON-NLS-1$
		colors.put("cornsilk", 0xfff8dc); //$NON-NLS-1$
		colors.put("crimson", 0xdc143c); //$NON-NLS-1$
		colors.put("cyan", 0x00ffff); //$NON-NLS-1$
		colors.put("darkblue", 0x00008b); //$NON-NLS-1$
		colors.put("darkcyan", 0x008b8b); //$NON-NLS-1$
		colors.put("darkgoldenrod", 0xb8860b); //$NON-NLS-1$
		colors.put("darkgray", 0xa9a9a9); //$NON-NLS-1$
		colors.put("darkgreen", 0x006400); //$NON-NLS-1$
		colors.put("darkgrey", 0xa9a9a9); //$NON-NLS-1$
		colors.put("darkkhaki", 0xbdb76b); //$NON-NLS-1$
		colors.put("darkmagenta", 0x8b008b); //$NON-NLS-1$
		colors.put("darkolivegreen", 0x556b2f); //$NON-NLS-1$
		colors.put("darkorange", 0xff8c00); //$NON-NLS-1$
		colors.put("darkorchid", 0x9932cc); //$NON-NLS-1$
		colors.put("darkred", 0x8b0000); //$NON-NLS-1$
		colors.put("darksalmon", 0xe9967a); //$NON-NLS-1$
		colors.put("darkseagreen", 0x8fbc8f); //$NON-NLS-1$
		colors.put("darkslateblue", 0x483d8b); //$NON-NLS-1$
		colors.put("darkslategray", 0x2f4f4f); //$NON-NLS-1$
		colors.put("darkslategrey", 0x2f4f4f); //$NON-NLS-1$
		colors.put("darkturquoise", 0x00ced1); //$NON-NLS-1$
		colors.put("darkviolet", 0x9400d3); //$NON-NLS-1$
		colors.put("deeppink", 0xff1493); //$NON-NLS-1$
		colors.put("deepskyblue", 0x00bfff); //$NON-NLS-1$
		colors.put("dimgray", 0x696969); //$NON-NLS-1$
		colors.put("dimgrey", 0x696969); //$NON-NLS-1$
		colors.put("dodgerblue", 0x1e90ff); //$NON-NLS-1$
		colors.put("firebrick", 0xb22222); //$NON-NLS-1$
		colors.put("floralwhite", 0xfffaf0); //$NON-NLS-1$
		colors.put("forestgreen", 0x228b22); //$NON-NLS-1$
		colors.put("fuchsia", 0xff00ff); //$NON-NLS-1$
		colors.put("gainsboro", 0xdcdcdc); //$NON-NLS-1$
		colors.put("ghostwhite", 0xf8f8ff); //$NON-NLS-1$
		colors.put("gold", 0xffd700); //$NON-NLS-1$
		colors.put("goldenrod", 0xdaa520); //$NON-NLS-1$
		colors.put("gray", 0x808080); //$NON-NLS-1$
		colors.put("grey", 0x808080); //$NON-NLS-1$
		colors.put("green", 0x008000); //$NON-NLS-1$
		colors.put("greenyellow", 0xadff2f); //$NON-NLS-1$
		colors.put("honeydew", 0xf0fff0); //$NON-NLS-1$
		colors.put("hotpink", 0xff69b4); //$NON-NLS-1$
		colors.put("indianred", 0xcd5c5c); //$NON-NLS-1$
		colors.put("indigo", 0x4b0082); //$NON-NLS-1$
		colors.put("ivory", 0xfffff0); //$NON-NLS-1$
		colors.put("khaki", 0xf0e68c); //$NON-NLS-1$
		colors.put("lavender", 0xe6e6fa); //$NON-NLS-1$
		colors.put("lavenderblush", 0xfff0f5); //$NON-NLS-1$
		colors.put("lawngreen", 0x7cfc00); //$NON-NLS-1$
		colors.put("lemonchiffon", 0xfffacd); //$NON-NLS-1$
		colors.put("lightblue", 0xadd8e6); //$NON-NLS-1$
		colors.put("lightcoral", 0xf08080); //$NON-NLS-1$
		colors.put("lightcyan", 0xe0ffff); //$NON-NLS-1$
		colors.put("lightgoldenrodyellow", 0xfafad2); //$NON-NLS-1$
		colors.put("lightgray", 0xd3d3d3); //$NON-NLS-1$
		colors.put("lightgreen", 0x90ee90); //$NON-NLS-1$
		colors.put("lightgrey", 0xd3d3d3); //$NON-NLS-1$
		colors.put("lightpink", 0xffb6c1); //$NON-NLS-1$
		colors.put("lightsalmon", 0xffa07a); //$NON-NLS-1$
		colors.put("lightseagreen", 0x20b2aa); //$NON-NLS-1$
		colors.put("lightskyblue", 0x87cefa); //$NON-NLS-1$
		colors.put("lightslategray", 0x778899); //$NON-NLS-1$
		colors.put("lightslategrey", 0x778899); //$NON-NLS-1$
		colors.put("lightsteelblue", 0xb0c4de); //$NON-NLS-1$
		colors.put("lightyellow", 0xffffe0); //$NON-NLS-1$
		colors.put("lime", 0x00ff00); //$NON-NLS-1$
		colors.put("limegreen", 0x32cd32); //$NON-NLS-1$
		colors.put("linen", 0xfaf0e6); //$NON-NLS-1$
		colors.put("magenta", 0xff00ff); //$NON-NLS-1$
		colors.put("maroon", 0x800000); //$NON-NLS-1$
		colors.put("mediumaquamarine", 0x66cdaa); //$NON-NLS-1$
		colors.put("mediumblue", 0x0000cd); //$NON-NLS-1$
		colors.put("mediumorchid", 0xba55d3); //$NON-NLS-1$
		colors.put("mediumpurple", 0x9370db); //$NON-NLS-1$
		colors.put("mediumseagreen", 0x3cb371); //$NON-NLS-1$
		colors.put("mediumslateblue", 0x7b68ee); //$NON-NLS-1$
		colors.put("mediumspringgreen", 0x00fa9a); //$NON-NLS-1$
		colors.put("mediumturquoise", 0x48d1cc); //$NON-NLS-1$
		colors.put("mediumvioletred", 0xc71585); //$NON-NLS-1$
		colors.put("midnightblue", 0x191970); //$NON-NLS-1$
		colors.put("mintcream", 0xf5fffa); //$NON-NLS-1$
		colors.put("mistyrose", 0xffe4e1); //$NON-NLS-1$
		colors.put("moccasin", 0xffe4b5); //$NON-NLS-1$
		colors.put("navajowhite", 0xffdead); //$NON-NLS-1$
		colors.put("navy", 0x000080); //$NON-NLS-1$
		colors.put("oldlace", 0xfdf5e6); //$NON-NLS-1$
		colors.put("olive", 0x808000); //$NON-NLS-1$
		colors.put("olivedrab", 0x6b8e23); //$NON-NLS-1$
		colors.put("orange", 0xffa500); //$NON-NLS-1$
		colors.put("orangered", 0xff4500); //$NON-NLS-1$
		colors.put("orchid", 0xda70d6); //$NON-NLS-1$
		colors.put("palegoldenrod", 0xeee8aa); //$NON-NLS-1$
		colors.put("palegreen", 0x98fb98); //$NON-NLS-1$
		colors.put("paleturquoise", 0xafeeee); //$NON-NLS-1$
		colors.put("palevioletred", 0xdb7093); //$NON-NLS-1$
		colors.put("papayawhip", 0xffefd5); //$NON-NLS-1$
		colors.put("peachpuff", 0xffdab9); //$NON-NLS-1$
		colors.put("peru", 0xcd853f); //$NON-NLS-1$
		colors.put("pink", 0xffc0cb); //$NON-NLS-1$
		colors.put("plum", 0xdda0dd); //$NON-NLS-1$
		colors.put("powderblue", 0xb0e0e6); //$NON-NLS-1$
		colors.put("purple", 0x800080); //$NON-NLS-1$
		colors.put("red", 0xff0000); //$NON-NLS-1$
		colors.put("rosybrown", 0xbc8f8f); //$NON-NLS-1$
		colors.put("royalblue", 0x4169e1); //$NON-NLS-1$
		colors.put("saddlebrown", 0x8b4513); //$NON-NLS-1$
		colors.put("salmon", 0xfa8072); //$NON-NLS-1$
		colors.put("sandybrown", 0xf4a460); //$NON-NLS-1$
		colors.put("seagreen", 0x2e8b57); //$NON-NLS-1$
		colors.put("seashell", 0xfff5ee); //$NON-NLS-1$
		colors.put("sienna", 0xa0522d); //$NON-NLS-1$
		colors.put("silver", 0xc0c0c0); //$NON-NLS-1$
		colors.put("skyblue", 0x87ceeb); //$NON-NLS-1$
		colors.put("slateblue", 0x6a5acd); //$NON-NLS-1$
		colors.put("slategray", 0x708090); //$NON-NLS-1$
		colors.put("slategrey", 0x708090); //$NON-NLS-1$
		colors.put("snow", 0xfffafa); //$NON-NLS-1$
		colors.put("springgreen", 0x00ff7f); //$NON-NLS-1$
		colors.put("steelblue", 0x4682b4); //$NON-NLS-1$
		colors.put("tan", 0xd2b48c); //$NON-NLS-1$
		colors.put("teal", 0x008080); //$NON-NLS-1$
		colors.put("thistle", 0xd8bfd8); //$NON-NLS-1$
		colors.put("tomato", 0xff6347); //$NON-NLS-1$
		colors.put("turquoise", 0x40e0d0); //$NON-NLS-1$
		colors.put("violet", 0xee82ee); //$NON-NLS-1$
		colors.put("wheat", 0xf5deb3); //$NON-NLS-1$
		colors.put("white", 0xffffff); //$NON-NLS-1$
		colors.put("whitesmoke", 0xf5f5f5); //$NON-NLS-1$
		colors.put("yellow", 0xffff00); //$NON-NLS-1$
		colors.put("yellowgreen", 0x9acd32); //$NON-NLS-1$
	}

	static boolean contains(String color) {
		return colors.containsKey(color);
	}

	static int get(String color) {
		return colors.get(color);
	}
	
	private SvgColors() {
		// class should never be instantiated
	}

}
