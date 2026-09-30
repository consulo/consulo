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
package consulo.desktop.qt.ui.impl.font;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.UIAccess;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.font.Typeface;
import consulo.ui.impl.font.TypefaceImpl;
import io.qt.core.QByteArray;
import io.qt.gui.QFontDatabase;
import io.qt.gui.QRawFont;
import io.qt.widgets.QApplication;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtFontManagerImpl implements FontManager {
    public static final DesktopQtFontManagerImpl INSTANCE = new DesktopQtFontManagerImpl();

    @Override
    public boolean isRequiredPermission() {
        return false;
    }

    @Override
    public CompletableFuture<List<Typeface>> getAvailableTypefacesAsync(UIAccess uiAccess) {
        return uiAccess.giveAsync(DesktopQtFontManagerImpl::readTypefaces);
    }

    @Override
    public Font createFont(String fontName, int fontSize, int fontStyle) {
        return new DesktopQtFontImpl(fontName, fontSize, fontStyle);
    }

    @Override
    public CompletableFuture<Font> registerFontAsync(URL url, UIAccess uiAccess, Disposable parent) {
        CompletableFuture<byte[]> data = new CompletableFuture<>();
        Thread.ofVirtual().start(() -> {
            try (InputStream stream = url.openStream()) {
                data.complete(stream.readAllBytes());
            }
            catch (Throwable e) {
                data.completeExceptionally(e);
            }
        });
        return data.thenCompose(bytes -> uiAccess.giveAsync(() -> registerFont(bytes, uiAccess, parent)));
    }

    private static Font registerFont(byte[] bytes, UIAccess uiAccess, Disposable parent) {
        int id = QFontDatabase.addApplicationFontFromData(bytes);
        if (id < 0) {
            throw new IllegalArgumentException("Unsupported font data");
        }

        List<String> families = QFontDatabase.applicationFontFamilies(id);
        if (families.isEmpty() || Disposer.isDisposed(parent)) {
            QFontDatabase.removeApplicationFont(id);
            throw new CancellationException();
        }

        Disposer.register(parent, () -> uiAccess.give(() -> QFontDatabase.removeApplicationFont(id)));

        String family = families.getFirst();

        QRawFont rawFont = new QRawFont(new QByteArray(bytes), QApplication.font().pointSizeF());
        String style = rawFont.isValid() ? rawFont.styleName() : "";
        String fontName = style.isBlank() || style.equalsIgnoreCase("Regular") ? family : family + " " + style;

        return new DesktopQtFontImpl(family, fontName, QApplication.font().pointSize(), Font.PLAIN);
    }

    private static List<Typeface> readTypefaces() {
        List<Typeface> typefaces = new ArrayList<>();
        for (String family : QFontDatabase.families()) {
            typefaces.add(new TypefaceImpl(family, QFontDatabase.isFixedPitch(family)));
        }
        return typefaces;
    }
}
