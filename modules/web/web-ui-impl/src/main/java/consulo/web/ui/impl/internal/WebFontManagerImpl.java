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
package consulo.web.ui.impl.internal;

import consulo.ui.impl.font.BundledFontRegistry;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class WebFontManagerImpl implements FontManager {
    public static final WebFontManagerImpl ourInstance = new WebFontManagerImpl();

    @Override
    public boolean isRequiredPermission() {
        return true;
    }

    @Override
    public CompletableFuture<Set<String>> getAvailableFontNamesAsync() {
        return CompletableFuture.completedFuture(Set.of());
    }

    @Override
    public Set<String> getAvailableFontNames() {
        // the browser has no api to enumerate what it can render, so only the bundled faces are offered - they
        // are the only ones the page ships a @font-face for
        return BundledFontRegistry.getFamilyNames();
    }

    @Override
    public Font createFont(String fontName, int fontSize, int fontStyle) {
        return new WebFontImpl(fontName, fontSize, fontStyle);
    }
}
