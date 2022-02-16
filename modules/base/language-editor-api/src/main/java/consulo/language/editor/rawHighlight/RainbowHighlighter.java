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
package consulo.language.editor.rawHighlight;

import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.colorScheme.EditorColorsManager;
import consulo.codeEditor.colorScheme.TextAttributesKey;
import consulo.codeEditor.colorScheme.TextAttributesScheme;
import consulo.codeEditor.markup.TextAttributes;
import consulo.language.Language;
import consulo.language.editor.DefaultLanguageHighlighterColors;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.util.ColorGenerator;
import consulo.language.psi.PsiElement;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.util.ColorValueUtil;
import consulo.ui.util.LightDarkColorValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RainbowHighlighter {
  private static final ColorValue[] RAINBOW_JB_COLORS_DEFAULT = {
          new LightDarkColorValue(new RGBColor(155, 59, 106), new RGBColor(82, 157, 82)),
          new LightDarkColorValue(new RGBColor(17, 77, 119), new RGBColor(190, 112, 112)),
          new LightDarkColorValue(new RGBColor(188, 134, 80), new RGBColor(61, 118, 118)),
          new LightDarkColorValue(new RGBColor(0, 89, 16), new RGBColor(190, 153, 112)),
          new LightDarkColorValue(new RGBColor(188, 81, 80), new RGBColor(157, 82, 124)),
  };
  public static final TextAttributesKey[] RAINBOW_COLOR_KEYS = new TextAttributesKey[RAINBOW_JB_COLORS_DEFAULT.length];
  private static final int RAINBOW_COLORS_BETWEEN = 4;
  private static final String UNIT_TEST_COLORS = "#000001,#000002,#000003,#000004"; // Do not modify!
  private static final String INHERITED = "inherited";

  static {
    for (int i = 0; i < RAINBOW_JB_COLORS_DEFAULT.length; ++i) {
      //noinspection deprecation
      RAINBOW_COLOR_KEYS[i] = TextAttributesKey.createTextAttributesKey("RAINBOW_COLOR" + i, createRainbowAttribute(RAINBOW_JB_COLORS_DEFAULT[i]));
    }
  }
  public final static String RAINBOW_TYPE = "rainbow";
  private final static String RAINBOW_TEMP_PREF = "RAINBOW_TEMP_";

  @SuppressWarnings("deprecation")
  public final static TextAttributesKey RAINBOW_ANCHOR = TextAttributesKey.createTextAttributesKey(RAINBOW_TYPE, new TextAttributes());
  @SuppressWarnings("deprecation")
  public final static TextAttributesKey RAINBOW_GRADIENT_DEMO = TextAttributesKey.createTextAttributesKey("rainbow_demo", new TextAttributes());
  public final static Boolean DEFAULT_RAINBOW_ON = Boolean.FALSE;

  @Nonnull
  private final TextAttributesScheme myColorsScheme;
  @Nonnull
  private final ColorValue[] myRainbowColors;

  public RainbowHighlighter(@Nullable TextAttributesScheme colorsScheme) {
    myColorsScheme = colorsScheme != null ? colorsScheme : EditorColorsManager.getInstance().getGlobalScheme();
    myRainbowColors = generateColorSequence(myColorsScheme);
  }

  public static final HighlightInfoType RAINBOW_ELEMENT = new HighlightInfoType.HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, DefaultLanguageHighlighterColors.CONSTANT);

  @Nullable
  @Contract("_, null -> !null")
  public static Boolean isRainbowEnabled(@Nullable TextAttributesScheme colorsScheme, @Nullable Language language) {
    Object value = colorsScheme == null ? null : colorsScheme.getMetaProperties().getOrDefault(getKey(language), INHERITED);
    if (String.valueOf(true).equals(value)) return Boolean.TRUE;
    if (String.valueOf(false).equals(value)) return Boolean.FALSE;
    return language == null ? DEFAULT_RAINBOW_ON : null;
  }

  public static boolean isRainbowEnabledWithInheritance(@Nullable TextAttributesScheme colorsScheme, @Nullable Language language) {
    Boolean rainbowEnabled = isRainbowEnabled(colorsScheme, language);
    return rainbowEnabled != null ? rainbowEnabled : isRainbowEnabled(colorsScheme, null);
  }

  public static void setRainbowEnabled(@Nonnull TextAttributesScheme colorsScheme, @Nullable Language language, @Nullable Boolean enabled) {
    Map<String, Object> properties = colorsScheme.getMetaProperties();
    String key = getKey(language);
    if (enabled == null || (language == null && enabled == DEFAULT_RAINBOW_ON)) {
      properties.remove(key);
    }
    else {
      properties.put(key, String.valueOf(enabled));
    }
  }

  @Nonnull
  private static String getKey(@Nullable Language language) {
    return RAINBOW_TYPE + " " + (language == null ? "Default language" : language.getID());
  }

  @Nonnull
  public static String generatePaletteExample() {
    int stopCount = RAINBOW_COLOR_KEYS.length;
    StringBuilder sb = new StringBuilder();
    String tagRainbow = RAINBOW_GRADIENT_DEMO.getExternalName();
    for (int i = 0; i < RAINBOW_TEMP_KEYS.length; ++i) {
      if (sb.length() != 0) {
        sb.append(" ");
      }
      sb.append("<").append(tagRainbow).append(">");
      sb.append((i % stopCount == 0) ? "Stop#" + String.valueOf(i / stopCount + 1) : "T");
      sb.append("</").append(tagRainbow).append(">");
    }
    return sb.toString();
  }

  @Nonnull
  @Contract(pure = true)
  private ColorValue calculateForeground(int colorIndex) {
    return myRainbowColors[colorIndex];
  }

  public int getColorsCount() {
    return myRainbowColors.length;
  }

  @Nonnull
  private static ColorValue[] generateColorSequence(@Nonnull TextAttributesScheme colorsScheme) {
    String colorDump = ApplicationManager.getApplication().isUnitTestMode()
                       ? UNIT_TEST_COLORS
                       : Registry.get("rainbow.highlighter.colors").asString();

    final List<String> registryColors = StringUtil.split(colorDump, ",");
    if (!registryColors.isEmpty()) {
      return registryColors.stream().map(s -> ColorValueUtil.fromHex(s.trim())).toArray(ColorValue[]::new);
    }

    List<ColorValue> stopColors = ContainerUtil.map(RAINBOW_COLOR_KEYS, key -> colorsScheme.getAttributes(key).getForegroundColor());
    List<ColorValue> colors = ColorGenerator.generateLinearColorSequence(stopColors, RAINBOW_COLORS_BETWEEN);
    return colors.toArray(new ColorValue[colors.size()]);
  }

  @Nonnull
  public TextAttributesKey[] getRainbowTempKeys() {
    TextAttributesKey[] keys = new TextAttributesKey[myRainbowColors.length];
    for (int i = 0; i < myRainbowColors.length; ++i) {
      //noinspection deprecation
      TextAttributesKey key = TextAttributesKey.createTextAttributesKey(RAINBOW_TEMP_PREF + i, new TextAttributes());
      key.getDefaultAttributes().setForegroundColor(myRainbowColors[i]);
      keys[i] = key;
    }
    return keys;
  }

  public static boolean isRainbowTempKey(TextAttributesKey key) {
    return key.getExternalName().startsWith(RAINBOW_TEMP_PREF);
  }

  public HighlightInfo getInfo(int colorIndex, @Nullable PsiElement id, @Nullable TextAttributesKey colorKey) {
    return id == null ? null : getInfoBuilder(colorIndex, colorKey).range(id).create();
  }

  public HighlightInfo getInfo(int colorIndex, int start, int end, @Nullable TextAttributesKey colorKey) {
    return getInfoBuilder(colorIndex, colorKey).range(start, end).create();
  }

  @Nonnull
  protected HighlightInfo.Builder getInfoBuilder(int colorIndex, @Nullable TextAttributesKey colorKey) {
    if (colorKey == null) {
      colorKey = DefaultLanguageHighlighterColors.LOCAL_VARIABLE;
    }
    return HighlightInfo
            .newHighlightInfo(RAINBOW_ELEMENT)
            .textAttributes(TextAttributes
                                    .fromFlyweight(myColorsScheme
                                                           .getAttributes(colorKey)
                                                           .getFlyweight()
                                                           .withForeground(calculateForeground(colorIndex))));
  }

  private static final TextAttributesKey[] RAINBOW_TEMP_KEYS = new RainbowHighlighter(null).getRainbowTempKeys();

  @Nonnull
  public static  TextAttributes createRainbowAttribute(@Nullable ColorValue color) {
    TextAttributes ret = new TextAttributes();
    ret.setForegroundColor(color);
    return ret;
  }

  public static Map<String, TextAttributesKey> createRainbowHLM() {
    Map<String, TextAttributesKey> hashMap = new HashMap<>();
    hashMap.put(RAINBOW_ANCHOR.getExternalName(), RAINBOW_ANCHOR);
    hashMap.put(RAINBOW_GRADIENT_DEMO.getExternalName(), RAINBOW_GRADIENT_DEMO);
    for (TextAttributesKey key : RAINBOW_TEMP_KEYS) {
      hashMap.put(key.getExternalName(), key);
    }
    return hashMap;
  }
}
