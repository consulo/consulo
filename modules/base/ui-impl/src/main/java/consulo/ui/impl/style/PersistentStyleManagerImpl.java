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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeManager;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.style.Style;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-29
 */
public abstract class PersistentStyleManagerImpl<S extends StyleImpl> extends StyleManagerImpl {
    private boolean myInitialLoadState = true;

    protected @Nullable S myCurrentStyle;

    protected Map<String, S> myStyles = new LinkedHashMap<>();

    protected final IconLibraryManager myIconLibraryManager;
    protected final LocalizeManager myLocalizeManager;

    public PersistentStyleManagerImpl() {
        myIconLibraryManager = IconLibraryManager.get();
        myLocalizeManager = LocalizeManager.get();

        fill(style -> myStyles.put(style.getId(), style));

        myCurrentStyle = getDefaultStyle();
    }

    protected abstract void fill(Consumer<S> consumer);

    @Override
    public @Nullable Style getStyle(String styleId) {
        return myStyles.get(styleId);
    }

    @Override
    public List<Style> getStyles() {
        return List.copyOf(myStyles.values());
    }

    @Override
    public Style getCurrentStyle() {
        return Objects.requireNonNull(myCurrentStyle);
    }

    public void afterLoad(Disposable disposable) {
        myInitialLoadState = false;

        S style = myCurrentStyle;

        if (style != null) {
            S laf = myStyles.get(style.getId());
            if (laf != null) {
                BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager) IconLibraryManager.get();
                boolean fromStyle = iconLibraryManager.isFromStyle();
                String activeLibraryId = iconLibraryManager.getActiveLibraryId(style);
                setCurrentStyle(laf, false, false, fromStyle ? null : activeLibraryId);
            }
        }

        forceReinitAll();

        // refresh UI on localize change
        LocalizeManager.get().addListener((oldLocale, newLocale) -> forceReinitAll(), disposable);
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public void setCurrentStyle(Style currentStyle) {
        S style = (S) currentStyle;
        setCurrentStyle(style, true, true, null);
    }

    public void loadState(StyleManagerState state) {
        String localeText = state.locale;
        if (localeText != null) {
            myLocalizeManager.setLocale(myLocalizeManager.parseLocale(localeText), false);
        }

        String iconId = state.icon;
        if (iconId != null) {
            if (myIconLibraryManager.getLibraries().containsKey(iconId)) {
                myIconLibraryManager.setActiveLibrary(iconId);
            }
            else {
                myIconLibraryManager.setActiveLibrary(null);
            }
        }

        String themeId = state.theme;

        S styleFromXml = themeId == null ? null : myStyles.get(themeId);

        if (styleFromXml == null) {
            styleFromXml = getDefaultStyle();
        }

        if (myCurrentStyle != null && !styleFromXml.equals(myCurrentStyle)) {
            boolean fire = !myInitialLoadState;
            setCurrentStyle(styleFromXml, false, fire, iconId);

            if (fire) {
                // will be called #afterLoad(boolean)
                forceReinitAll();
            }
        }

        myCurrentStyle = styleFromXml;
    }

    public StyleManagerState getState() {
        StyleManagerState state = new StyleManagerState();
        if (myCurrentStyle != null) {
            state.theme = myCurrentStyle.getId();
        }

        if (!myLocalizeManager.isDefaultLocale()) {
            state.locale = myLocalizeManager.getLocale().toLanguageTag();
        }

        if (!myIconLibraryManager.isFromStyle()) {
            state.icon = myIconLibraryManager.getActiveLibraryId();
        }
        return state;
    }

    public S getDefaultStyle() {
        boolean darked = Platform.current().user().darkTheme();
        S style = myStyles.get(darked ? Style.DARK_ID : Style.LIGHT_ID);
        if (style != null) {
            return style;
        }
        throw new IllegalStateException("No default theme");
    }

    protected abstract void setCurrentStyle(@Nullable S style, boolean wantChangeScheme, boolean fire, @Nullable String iconLibraryId);
}
