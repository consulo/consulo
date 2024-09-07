/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.icons;

import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.SystemInfo;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * "close" icon for windows (frames and dialogs).
 *
 * @uiDefault TitlePane.closeHoverBackground			Color
 * @uiDefault TitlePane.closePressedBackground			Color
 * @uiDefault TitlePane.closeHoverForeground			Color
 * @uiDefault TitlePane.closePressedForeground			Color
 *
 * @author Karl Tauber
 */
public class FlatWindowCloseIcon
	extends FlatWindowAbstractIcon
{
	private final Color hoverForeground;
	private final Color pressedForeground;

	public FlatWindowCloseIcon() {
		this( null );
	}

	/** @since 3.2 */
	public FlatWindowCloseIcon( String windowStyle ) {
		super( FlatUIUtils.getSubUIDimension( "TitlePane.buttonSize", windowStyle ),
			FlatUIUtils.getSubUIInt( "TitlePane.buttonSymbolHeight", windowStyle, 10 ),
			FlatUIUtils.getSubUIColor( "TitlePane.closeHoverBackground", windowStyle ),
			FlatUIUtils.getSubUIColor( "TitlePane.closePressedBackground", windowStyle ) );

		hoverForeground = FlatUIUtils.getSubUIColor( "TitlePane.closeHoverForeground", windowStyle );
		pressedForeground = FlatUIUtils.getSubUIColor( "TitlePane.closePressedForeground", windowStyle );
	}

	@Override
	protected void paintIconAt1x( Graphics2D g, int x, int y, int width, int height, double scaleFactor ) {
		int iwh = (int) (getSymbolHeight() * scaleFactor);
		int ix = x + ((width - iwh) / 2);
		int iy = y + ((height - iwh) / 2);
		int ix2 = ix + iwh - 1;
		int iy2 = iy + iwh - 1;
		float thickness = SystemInfo.isWindows_11_orLater ? (float) scaleFactor : (int) scaleFactor;

		Path2D path = new Path2D.Float( Path2D.WIND_EVEN_ODD, 4 );
		path.moveTo( ix, iy );
		path.lineTo( ix2, iy2 );
		path.moveTo( ix, iy2 );
		path.lineTo( ix2, iy );
		g.setStroke( new BasicStroke( thickness ) );
		g.draw( path );
	}

	@Override
	protected Color getForeground( Component c ) {
		return FlatButtonUI.buttonStateColor( c, c.getForeground(), null, null, hoverForeground, pressedForeground );
	}
}
