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
package consulo.desktop.awt.ui.impl;

import consulo.ui.UIAccess;
import consulo.ui.ex.awt.FontInfo;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.font.Typeface;
import consulo.ui.impl.font.TypefaceImpl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public class DesktopFontManagerImpl implements FontManager {
    public static final DesktopFontManagerImpl ourInstance = new DesktopFontManagerImpl();

    @Override
    public boolean isRequiredPermission() {
        return false;
    }

    /**
     * Reads the families by name, which also settles the pitch of each - asking the graphics environment for
     * every face instead would be markedly slower and would answer with face names, which is not what a family
     * is. That enumeration is the call worth keeping off the ui thread, and it orders itself for a chooser,
     * monospaced first, so it is sorted back into plain alphabetical order here.
     */
    @Override
    public CompletableFuture<List<Typeface>> getAvailableTypefacesAsync(UIAccess uiAccess) {
        CompletableFuture<List<Typeface>> result = new CompletableFuture<>();

        Thread.ofVirtual().start(() -> {
            try {
                List<Typeface> typefaces = new ArrayList<>();
                for (FontInfo fontInfo : FontInfo.getAllUncached()) {
                    typefaces.add(new TypefaceImpl(fontInfo.toString(), fontInfo.isMonospaced()));
                }
                typefaces.sort(Comparator.comparing(Typeface::getName));
                result.complete(typefaces);
            }
            catch (Throwable e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }

    @Override
    public Font createFont(String fontName, int fontSize, int fontStyles) {
        return new DesktopFontImpl(fontName, fontSize, fontStyles);
    }
}
