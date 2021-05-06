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

import javax.swing.text.html.HTMLEditorKit;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class HTMLEditorKitHacking {
  private static final Logger LOG = Logger.getInstance(HTMLEditorKitHacking.class);

  public static Object DEFAULT_STYLES_KEY() {
    try {
      Field keyField = HTMLEditorKit.class.getDeclaredField("DEFAULT_STYLES_KEY");
      keyField.setAccessible(true);
      return keyField.get(null);
    }
    catch (Throwable e) {
      LOG.warn(e);
      return null;
    }
  }
}
