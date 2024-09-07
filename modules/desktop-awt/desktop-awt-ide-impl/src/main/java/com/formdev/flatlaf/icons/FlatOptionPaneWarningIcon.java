/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.icons;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import com.formdev.flatlaf.ui.FlatUIUtils;

/**
 * "Warning" icon for {@link javax.swing.JOptionPane}.
 *
 * @uiDefault OptionPane.icon.warningColor			Color	optional; defaults to Actions.Yellow
 * @uiDefault Actions.Yellow						Color
 *
 * @author Karl Tauber
 */
public class FlatOptionPaneWarningIcon
	extends FlatOptionPaneAbstractIcon
{
	public FlatOptionPaneWarningIcon() {
		super( "OptionPane.icon.warningColor", "Actions.Yellow" );
	}

	/*
		<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
		  <g fill="none" fill-rule="evenodd">
		    <path fill="#EDA200" d="M17.7364863,3.038851 L30.2901269,25.0077221 C30.8381469,25.966757 30.5049534,27.1884663 29.5459185,27.7364863 C29.2437231,27.9091694 28.9016945,28 28.5536406,28 L3.44635936,28 C2.34178986,28 1.44635936,27.1045695 1.44635936,26 C1.44635936,25.6519461 1.53718999,25.3099175 1.70987307,25.0077221 L14.2635137,3.038851 C14.8115337,2.0798161 16.033243,1.74662265 16.9922779,2.29464259 C17.3023404,2.47182119 17.5593077,2.72878844 17.7364863,3.038851 Z"/>
		    <rect width="4" height="11" x="14" y="8" fill="#FFF" rx="2"/>
		    <circle cx="16" cy="23" r="2" fill="#FFF"/>
		  </g>
		</svg>
	*/

	@Override
	protected Shape createOutside() {
		return FlatUIUtils.createRoundTrianglePath( 16,0, 32,28, 0,28, 4 );

	}

	@Override
	protected Shape createInside() {
		Path2D inside = new Path2D.Float( Path2D.WIND_EVEN_ODD );
		inside.append( new RoundRectangle2D.Float( 14, 8, 4, 11, 4, 4 ), false );
		inside.append( new Ellipse2D.Float( 14, 21, 4, 4 ), false );
		return inside;
	}
}
