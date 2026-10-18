/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log;


import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.util.LightDarkColorValue;

// TODO migrated color scheme
public class VcsLogStandardColors {
  public static class Refs {
    public static final ColorValue TIP = new LightDarkColorValue(RGBColor.fromRGBValue(0xffe137), RGBColor.fromRGBValue(0xe1c731));
    public static final ColorValue LEAF = new LightDarkColorValue(RGBColor.fromRGBValue(0x8a2d6b), RGBColor.fromRGBValue(0xc31e8c));
    public static final ColorValue BRANCH = new LightDarkColorValue(RGBColor.fromRGBValue(0x3cb45c), RGBColor.fromRGBValue(0x3cb45c));
    public static final ColorValue BRANCH_REF = new LightDarkColorValue(RGBColor.fromRGBValue(0x9f79b5), RGBColor.fromRGBValue(0x9f79b5));
    public static final ColorValue TAG = new LightDarkColorValue(RGBColor.fromRGBValue(0x7a7a7a), RGBColor.fromRGBValue(0x999999));
  }
}
