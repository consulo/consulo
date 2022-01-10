/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.ui;

import consulo.logging.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * migration part
 */
public class DesktopAppUIUtil extends AppUIUtil {
  public static void registerBundledFonts() {
    registerFont("/fonts/Inconsolata.ttf");
    registerFont("/fonts/SourceCodePro-Regular.ttf");
    registerFont("/fonts/SourceCodePro-Bold.ttf");
    registerFont("/fonts/FiraCode-Bold.ttf");
    registerFont("/fonts/FiraCode-Light.ttf");
    registerFont("/fonts/FiraCode-Medium.ttf");
    registerFont("/fonts/FiraCode-Regular.ttf");
    registerFont("/fonts/FiraCode-Retina.ttf");
    registerFont("/fonts/FiraCode-SemiBold.ttf");
    registerFont("/fonts/JetBrainsMono-Bold.ttf");
    registerFont("/fonts/JetBrainsMono-BoldItalic.ttf");
    registerFont("/fonts/JetBrainsMono-ExtraBold.ttf");
    registerFont("/fonts/JetBrainsMono-ExtraBoldItalic.ttf");
    registerFont("/fonts/JetBrainsMono-ExtraLight.ttf");
    registerFont("/fonts/JetBrainsMono-ExtraLightItalic.ttf");
    registerFont("/fonts/JetBrainsMono-Italic.ttf");
    registerFont("/fonts/JetBrainsMono-Light.ttf");
    registerFont("/fonts/JetBrainsMono-LightItalic.ttf");
    registerFont("/fonts/JetBrainsMono-Medium.ttf");
    registerFont("/fonts/JetBrainsMono-MediumItalic.ttf");
    registerFont("/fonts/JetBrainsMono-Regular.ttf");
    registerFont("/fonts/JetBrainsMono-SemiBold.ttf");
    registerFont("/fonts/JetBrainsMono-SemiBoldItalic.ttf");
    registerFont("/fonts/JetBrainsMono-Thin.ttf");
    registerFont("/fonts/JetBrainsMono-ThinItalic.ttf");
  }

  private static void registerFont(String name) {
    try {
      URL url = AppUIUtil.class.getResource(name);
      if (url == null) {
        throw new IOException("Resource missing: " + name);
      }

      try (InputStream is = url.openStream()) {
        Font font = Font.createFont(Font.TRUETYPE_FONT, is);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
      }
    }
    catch (Exception e) {
      Logger.getInstance(AppUIUtil.class).error("Cannot register font: " + name, e);
    }
  }
}
