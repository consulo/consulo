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
package consulo.awt.hacking;

import consulo.logging.Logger;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class FontManagerFactoryHacking {
  private static final Logger LOG = Logger.getInstance(FontManagerFactoryHacking.class);

  public static void mapLogicFontsToPhysical(String[] logicalFontsToMap, BiConsumer<String, String> mapper) {
    try {
      Object fontManager = null;
      try {
        fontManager = Class.forName("sun.font.FontManagerFactory").getMethod("getInstance").invoke(null);
      }
      catch (ClassNotFoundException e) {
        // expected for JRE 1.6. FontManager.findFont2D method is static there, so leaving fontManager value as null will work
      }
      Method findFontMethod = Class.forName("sun.font.FontManager").getMethod("findFont2D", String.class, int.class, int.class);
      for (String logicalFont : logicalFontsToMap) {
        Object font2D = findFontMethod.invoke(fontManager, logicalFont, Font.PLAIN, 0);
        if (font2D == null) {
          continue;
        }
        String fontClassName = font2D.getClass().getName();
        String physicalFont = null;
        if ("sun.font.CompositeFont".equals(fontClassName)) { // Windows and Linux case
          Object physicalFontObject = Class.forName("sun.font.CompositeFont").getMethod("getSlotFont", int.class).invoke(font2D, 0);
          physicalFont = (String)Class.forName("sun.font.Font2D").getMethod("getFamilyName", Locale.class).invoke(physicalFontObject, Locale.getDefault());
        }
        else if ("sun.font.CFont".equals(fontClassName)) { // MacOS case
          Class<?> cFontClazz = Class.forName("sun.font.CFont");
          physicalFont = (String)cFontClazz.getDeclaredField("nativeFontName").get(font2D);
        }
        if (physicalFont != null) {
          mapper.accept(logicalFont, physicalFont);
        }
      }
    }
    catch (Throwable e) {
      LOG.warn("Failed to determine logical to physical font mappings", e);
    }
  }
}
