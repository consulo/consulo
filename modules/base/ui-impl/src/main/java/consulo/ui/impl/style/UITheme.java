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
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class UITheme {
    public static final String FILE_EXT_ENDING = ".theme.json";

    private static final String DEFAULT_DARK_PARENT_THEME = "Darcula";
    private static final String DEFAULT_LIGHT_PARENT_THEME = "IntelliJ";

    private static final String RESOURCE_PREFIX = "/consulo/ui/impl/style/";

    private static final Logger LOG = Logger.getInstance(UITheme.class);

    private static final Map<String, String> ourThemeIdAliases = Map.of(
        DEFAULT_DARK_PARENT_THEME, "dark",
        DEFAULT_LIGHT_PARENT_THEME, "light",
        "Dark", "dark",
        "Light", "light"
    );

    private static final ConcurrentMap<String, UITheme> ourThemes = new ConcurrentHashMap<>();

    private final String myId;
    private final UIThemeBean myBean;
    private final List<Map.Entry<String, Object>> myPatterns = new ArrayList<>();

    private UITheme(String id, UIThemeBean bean) {
        myId = id;
        myBean = bean;

        Map<String, Object> ui = bean.ui;
        if (ui != null) {
            for (Map.Entry<String, Object> entry : ui.entrySet()) {
                if (entry.getKey().startsWith("*.")) {
                    myPatterns.add(Map.entry(entry.getKey().substring(1), entry.getValue()));
                }
            }
        }
    }

    public static @Nullable UITheme get(String themeId) {
        return ourThemes.computeIfAbsent(themeId, id -> {
            UIThemeBean bean = loadBean(id, new HashSet<>());
            return bean == null ? null : new UITheme(id, bean);
        });
    }

    private static @Nullable UIThemeBean loadBean(String themeId, Set<String> visited) {
        if (!visited.add(themeId)) {
            LOG.error("Cyclic parentTheme reference at " + themeId);
            return null;
        }

        String resourceName = RESOURCE_PREFIX + resolveThemeId(themeId) + FILE_EXT_ENDING;

        UIThemeBean bean;
        try (InputStream stream = UITheme.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                LOG.warn("No theme found by id " + themeId);
                return null;
            }

            bean = UIThemeBean.readTheme(new JsonFactory().createParser(ObjectReadContext.empty(), stream));
        }
        catch (Exception e) {
            LOG.error("Failed to read theme " + themeId, e);
            return null;
        }

        String parentThemeId = bean.parentTheme;
        if (parentThemeId == null) {
            parentThemeId = bean.dark ? DEFAULT_DARK_PARENT_THEME : DEFAULT_LIGHT_PARENT_THEME;
        }

        if (!resolveThemeId(parentThemeId).equals(resolveThemeId(themeId))) {
            UIThemeBean parentBean = loadBean(parentThemeId, visited);
            if (parentBean != null) {
                UIThemeBean.importFromParentTheme(bean, parentBean);
            }
        }

        ColorMap.initializeNamedColors(bean);
        return bean;
    }

    private static String resolveThemeId(String themeId) {
        return ourThemeIdAliases.getOrDefault(themeId, themeId);
    }

    public String getId() {
        return myId;
    }

    public @Nullable String getName() {
        return myBean.name;
    }

    public boolean isDark() {
        return myBean.dark;
    }

    public @Nullable String getEditorSchemeId() {
        return myBean.editorScheme;
    }

    public @Nullable String getIconLibraryId() {
        return myBean.iconLibraryId;
    }

    public Map<String, RGBColor> getPalette() {
        return myBean.colorMap.map;
    }

    public @Nullable RGBColor getColor(String key) {
        Map<String, Object> ui = myBean.ui;
        if (ui == null) {
            return null;
        }

        Object value = ui.get(key);
        if (value == null) {
            for (Map.Entry<String, Object> pattern : myPatterns) {
                if (key.endsWith(pattern.getKey())) {
                    value = pattern.getValue();
                    break;
                }
            }
        }

        return toColor(value);
    }

    private @Nullable RGBColor toColor(@Nullable Object value) {
        if (value instanceof RGBColor color) {
            return color;
        }
        if (value instanceof String name) {
            return myBean.colorMap.map.get(name);
        }
        return null;
    }
}
