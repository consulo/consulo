/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options;

/**
 * @author Denis Zhdanov
 * @since 4/25/11 4:23 PM
 * @deprecated
 * @use consulo.ide.impl.idea.application.options.EditorFontsConstants
 */
public class OptionsConstants {
  @Deprecated
  public static final int MIN_EDITOR_FONT_SIZE     = 4;
  @Deprecated
  public static final int MAX_EDITOR_FONT_SIZE     = 40;
  @Deprecated
  public static final int DEFAULT_EDITOR_FONT_SIZE = 12;
  @Deprecated
  public static final float MIN_EDITOR_LINE_SPACING =   .6F;
  @Deprecated
  public static final float MAX_EDITOR_LINE_SPACING =     3;
  @Deprecated
  public static final float DEFAULT_EDITOR_LINE_SPACING = 1;

  private OptionsConstants() {
  }
}
