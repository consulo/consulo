/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.util.awt;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.util.awt.laf.PreJava9UIUtil;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-11-19
 */
public enum AntialiasingType {
  SUBPIXEL("Subpixel", RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB, true),
  GREYSCALE("Greyscale", RenderingHints.VALUE_TEXT_ANTIALIAS_ON, true),
  OFF("No antialiasing", RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, false);

  private final String myName;
  private final Object myHint;
  private final boolean isEnabled;

  AntialiasingType(String name, Object hint, boolean enabled) {
    myName = name;
    myHint = hint;
    isEnabled = enabled;
  }

  public Object getHint() {
    return myHint;
  }

  public Object getTextInfo() {
    if(SystemInfo.IS_AT_LEAST_JAVA9) {
      throw new IllegalArgumentException("must be never called at java 9+");
    }
    return isEnabled ? PreJava9UIUtil.newAATextInfo(myHint, UIUtil.getLcdContrastValue()) : null;
  }

  @Override
  public String toString() {
    return myName;
  }}
