// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.impl.style;

import consulo.logging.Logger;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class UIThemeBean {
    private static final Logger LOG = Logger.getInstance(UIThemeBean.class);

    private static final String OS_MACOS_KEY = "os.mac";
    private static final String OS_WINDOWS_KEY = "os.windows";
    private static final String OS_LINUX_KEY = "os.linux";
    private static final String OS_DEFAULT_KEY = "os.default";

    private static final String ourOsKey = resolveOsKey();

    public @Nullable String name;

    public @Nullable String nameKey;

    public @Nullable String parentTheme;

    public @Nullable String resourceBundle = "messages.IdeBundle";

    public @Nullable String author;

    /**
     * The path to editor scheme file.
     */
    public @Nullable String editorScheme;

    public @Nullable String iconLibraryId;

    public boolean dark = false;

    public @Nullable Map<String, Object> ui;

    public @Nullable Map<String, Object> icons;

    public @Nullable Map<String, Object> background;

    public @Nullable Map<String, Object> emptyFrameBackground;

    public ColorMap colorMap = new ColorMap();

    public ColorMap iconColorOnSelectionMap = new ColorMap();

    @Override
    public String toString() {
        return "UIThemeBean(name=" + name + ", parentTheme=" + parentTheme + ", dark=" + dark + ")";
    }

    static UIThemeBean readTheme(JsonParser parser) {
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("theme json must start with an object");
        }

        UIThemeBean bean = new UIThemeBean();
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }

            switch (token) {
                case START_OBJECT -> {
                    switch (parser.currentName()) {
                        case "icons" -> bean.icons = readMapFromJson(parser);
                        case "background" -> bean.background = readMapFromJson(parser);
                        case "emptyFrameBackground" -> bean.emptyFrameBackground = readMapFromJson(parser);
                        case "colors" -> bean.colorMap.rawMap = readColorMapFromJson(parser, new LinkedHashMap<>());
                        case "iconColorsOnSelection" ->
                            bean.iconColorOnSelectionMap.rawMap = readColorMapFromJson(parser, new LinkedHashMap<>());
                        case "ui" -> {
                            // ordered map is required (not clear why)
                            Map<String, Object> map = new LinkedHashMap<>(700);
                            readFlatMapFromJson(parser, map);
                            bean.ui = map;
                        }
                        case "UIDesigner" -> parser.skipChildren();
                        default -> LOG.warn("Unknown object: " + parser.currentName());
                    }
                }
                case END_OBJECT -> {
                    return bean;
                }
                case VALUE_STRING -> {
                    switch (parser.currentName()) {
                        case "id" -> LOG.warn("Do not set theme id in JSON (value=" + parser.getValueAsString() + ")");
                        case "name" -> bean.name = parser.getValueAsString();
                        case "nameKey" -> bean.nameKey = parser.getValueAsString();
                        case "parentTheme" -> bean.parentTheme = parser.getValueAsString();
                        case "resourceBundle" -> bean.resourceBundle = parser.getValueAsString();
                        case "author" -> bean.author = parser.getValueAsString();
                        case "editorScheme" -> bean.editorScheme = parser.getValueAsString();
                        case "iconLibraryId" -> bean.iconLibraryId = parser.getValueAsString();
                        default -> {
                        }
                    }
                }
                case VALUE_TRUE -> readTopLevelBoolean(parser, bean, true);
                case VALUE_FALSE -> readTopLevelBoolean(parser, bean, false);
                case PROPERTY_NAME -> {
                }
                default -> LOG.warn("Unknown field: " + parser.currentName());
            }
        }

        return bean;
    }

    private static void readTopLevelBoolean(JsonParser parser, UIThemeBean bean, boolean value) {
        if ("dark".equals(parser.currentName())) {
            bean.dark = value;
        }
    }

    /**
     * Flatten example: {@code "Editor": { "SearchField": { "borderInsets": "7,10,7,8" } }} is flattened to
     * {@code "Editor.SearchField.borderInsets": "7,10,7,8"} in internal representation.
     * <p>
     * Per-OS keys are also resolved as shown below:
     * <pre>
     *  "Menu.borderColor": {
     *    "os.default": "Grey12",
     *    "os.windows": "Blue12"
     *  }
     * </pre>
     * <p>
     * This is useful when we need to validate if a certain key was already set, and to uniformly override
     * parentTheme keys regardless of used format.
     * <p>
     * Note: we intentionally do not expand "*" patterns here.
     */
    private static void readFlatMapFromJson(JsonParser parser, Map<String, Object> result) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("expected an object");
        }

        Deque<String> prefix = new ArrayDeque<>();
        StringBuilder path = new StringBuilder();
        String currentFieldName = null;
        int level = 1;
        loop:
        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }

            switch (token) {
                case START_OBJECT -> {
                    level++;
                    prefix.addLast(currentFieldName);
                    currentFieldName = null;
                }
                case END_OBJECT -> {
                    level--;
                    prefix.pollLast();
                    currentFieldName = null;

                    if (level == 0) {
                        break loop;
                    }
                }
                case START_ARRAY -> {
                    String fieldName = parser.currentName();
                    while (true) {
                        JsonToken arrayToken = parser.nextToken();
                        if (arrayToken == JsonToken.END_ARRAY || arrayToken == null) {
                            break;
                        }

                        if (arrayToken == JsonToken.VALUE_STRING) {
                            if (!prefix.isEmpty()) {
                                path.append(String.join(".", prefix));
                                path.append('.');
                            }
                            path.append(fieldName);
                            result.put(path.toString(), parser.getString());
                            path.setLength(0);
                        }
                        else {
                            logError(parser);
                        }
                    }
                }
                case VALUE_STRING -> putEntry(prefix, result, parser, path, key -> UIThemeParser.parseStringValue(parser.getString(), key));
                case VALUE_NUMBER_INT -> putEntry(prefix, result, parser, path, key -> parser.getIntValue());
                case VALUE_NUMBER_FLOAT -> putEntry(prefix, result, parser, path, key -> parser.getDoubleValue());
                case VALUE_FALSE -> putEntry(prefix, result, parser, path, key -> Boolean.FALSE);
                case VALUE_TRUE -> putEntry(prefix, result, parser, path, key -> Boolean.TRUE);
                case VALUE_NULL -> {
                }
                case PROPERTY_NAME -> currentFieldName = parser.currentName();
                default -> logError(parser);
            }
        }

        result.replaceAll((key, value) -> value instanceof OsDefaultValue osDefaultValue ? osDefaultValue.value : value);
    }

    private static void putEntry(
        Deque<String> prefix,
        Map<String, Object> result,
        JsonParser parser,
        StringBuilder path,
        Function<String, Object> getter
    ) {
        if (!prefix.isEmpty()) {
            boolean isFirst = true;
            for (String element : prefix) {
                if (isFirst) {
                    isFirst = false;
                }
                else if (!"UI".equals(element)) {
                    path.append('.');
                }
                path.append(element);
            }
        }

        String key = parser.currentName();
        if (key.equals(getOsKey())) {
        }
        else if (OS_WINDOWS_KEY.equals(key) || OS_MACOS_KEY.equals(key) || OS_LINUX_KEY.equals(key)) {
            path.setLength(0);
            return;
        }
        else if (OS_DEFAULT_KEY.equals(key)) {
            String compositeKey = path.toString();
            path.setLength(0);

            Object value = getter.apply(compositeKey);
            Object oldValue = result.putIfAbsent(compositeKey, new OsDefaultValue(value));
            if (oldValue instanceof OsDefaultValue) {
                LOG.error("Duplicated value: (value=" + value + ", compositeKey=" + compositeKey + ")");
            }
            return;
        }
        else if ("UI".equals(key)) {
            path.append(key);
        }
        else {
            if (!path.isEmpty()) {
                path.append('.');
            }
            path.append(key);
        }

        String finalKey = path.toString();
        Object value = getter.apply(finalKey);
        result.put(finalKey, value);
        path.setLength(0);
    }

    private static String getOsKey() {
        return ourOsKey;
    }

    private static String resolveOsKey() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.startsWith("windows")) {
            return OS_WINDOWS_KEY;
        }
        if (osName.startsWith("mac")) {
            return OS_MACOS_KEY;
        }
        return OS_LINUX_KEY;
    }

    private static Map<String, ColorMap.ThemeColorValue> readColorMapFromJson(
        JsonParser parser,
        Map<String, ColorMap.ThemeColorValue> result
    ) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("expected an object");
        }

        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }

            switch (token) {
                case END_OBJECT -> {
                    return result;
                }
                case VALUE_STRING -> {
                    String text = parser.getString();
                    String key = parser.currentName();
                    if (UIThemeParser.isColorLike(text)) {
                        RGBColor color = UIThemeParser.parseColorOrNull(text, key);
                        if (color != null) {
                            result.put(key, new ColorMap.RGBColorValue(color));
                            continue;
                        }
                        LOG.warn(key + "=" + text + " has # prefix but cannot be parsed as color");
                    }
                    result.put(key, new ColorMap.NamedColorValue(text));
                }
                case PROPERTY_NAME -> {
                }
                case START_OBJECT, START_ARRAY -> {
                    logError(parser);
                    parser.skipChildren();
                }
                default -> logError(parser);
            }
        }

        return result;
    }

    private static Map<String, Object> readMapFromJson(JsonParser parser) {
        Map<String, Object> result = new LinkedHashMap<>();
        readMapFromJson(parser, result);
        return result;
    }

    private static void readMapFromJson(JsonParser parser, Map<String, Object> result) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("expected an object");
        }

        while (true) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                break;
            }

            switch (token) {
                case START_OBJECT -> {
                    Map<String, Object> nested = new LinkedHashMap<>();
                    result.put(parser.currentName(), nested);
                    readMapFromJson(parser, nested);
                }
                case END_OBJECT -> {
                    // END_OBJECT for nested maps is handled by readMapFromJson
                    return;
                }
                case VALUE_STRING -> {
                    String text = parser.getString();
                    String key = parser.currentName();
                    if (UIThemeParser.isColorLike(text)) {
                        RGBColor color = UIThemeParser.parseColorOrNull(text, key);
                        if (color != null) {
                            result.put(key, color);
                            continue;
                        }
                        LOG.warn(key + "=" + text + " has # prefix but cannot be parsed as color");
                    }
                    result.put(key, text);
                }
                case VALUE_NUMBER_INT -> result.put(parser.currentName(), parser.getIntValue());
                case VALUE_NUMBER_FLOAT -> result.put(parser.currentName(), parser.getDoubleValue());
                case VALUE_FALSE -> result.put(parser.currentName(), Boolean.FALSE);
                case VALUE_TRUE -> result.put(parser.currentName(), Boolean.TRUE);
                case VALUE_NULL, PROPERTY_NAME -> {
                }
                default -> logError(parser);
            }
        }
    }

    private static void logError(JsonParser parser) {
        LOG.warn("JSON contains data in unsupported format (token=" + parser.currentToken() + "): " + parser.currentValue());
    }

    static void importFromParentTheme(UIThemeBean theme, UIThemeBean parentTheme) {
        theme.ui = importMapFromParentTheme(theme.ui, parentTheme.ui);
        theme.icons = importIconsFromParentTheme(theme.icons, parentTheme.icons);
        theme.background = importMapFromParentTheme(theme.background, parentTheme.background);
        theme.emptyFrameBackground = importMapFromParentTheme(theme.emptyFrameBackground, parentTheme.emptyFrameBackground);
        theme.colorMap.rawMap = importMapFromParentTheme(theme.colorMap.rawMap, parentTheme.colorMap.rawMap);
        theme.iconColorOnSelectionMap.rawMap =
            importMapFromParentTheme(theme.iconColorOnSelectionMap.rawMap, parentTheme.iconColorOnSelectionMap.rawMap);
    }

    private static <T> @Nullable Map<String, T> importMapFromParentTheme(@Nullable Map<String, T> map, @Nullable Map<String, T> parentMap) {
        if (parentMap == null) {
            return map;
        }
        if (map == null) {
            return new LinkedHashMap<>(parentMap);
        }

        Map<String, T> result = new LinkedHashMap<>(parentMap.size() + map.size());
        for (Map.Entry<String, T> entry : parentMap.entrySet()) {
            if (!map.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        result.putAll(map);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, Object> importIconsFromParentTheme(
        @Nullable Map<String, Object> map,
        @Nullable Map<String, Object> parentMap
    ) {
        Map<String, Object> result = importMapFromParentTheme(map, parentMap);
        Object palette = map == null ? null : map.get("ColorPalette");
        Object parentPalette = parentMap == null ? null : parentMap.get("ColorPalette");

        if (result != null && palette instanceof Map && parentPalette instanceof Map) {
            Map<Object, Object> unitedPalette = new LinkedHashMap<>((Map<Object, Object>)parentPalette);
            unitedPalette.putAll((Map<Object, Object>)palette);
            Map<String, Object> mutableMap = new LinkedHashMap<>(result);
            mutableMap.put("ColorPalette", unitedPalette);
            return mutableMap;
        }
        return result;
    }

    private record OsDefaultValue(@Nullable Object value) {
    }
}
