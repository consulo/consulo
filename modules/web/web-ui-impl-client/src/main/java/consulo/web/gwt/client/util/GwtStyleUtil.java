/*
 * Copyright 2013-2016 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.web.gwt.client.util;

import consulo.annotation.DeprecationInfo;
import consulo.web.gwt.shared.transport.GwtColor;
import consulo.web.gwt.shared.ui.state.RGBColor;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20-May-16
 */
public class GwtStyleUtil {
  @Deprecated
  @DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
  public static String toString(GwtColor color) {
    return "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")";
  }

  @Nonnull
  public static String toString(RGBColor color) {
    return "rgb(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")";
  }
}
