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
package consulo.desktop.qt.application.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.impl.internal.UIFontManagerImpl;
import consulo.util.lang.Pair;
import io.qt.gui.QFont;
import io.qt.widgets.QApplication;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@Singleton
@ServiceImpl
public class DesktopQtUIFontManagerImpl extends UIFontManagerImpl {
    @Override
    protected Pair<String, Integer> resolveSystemFontData() {
        QFont font = QApplication.font();

        return Pair.create(font.family(), font.pointSize());
    }
}
