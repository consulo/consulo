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
package consulo.desktop.qt.ui.impl;

import consulo.ui.impl.style.StyleImpl;
import consulo.ui.impl.style.UITheme;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtStyleImpl extends StyleImpl {
    private final String myId;

    public DesktopQtStyleImpl(String id) {
        myId = id;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getName() {
        UITheme theme = getTheme();
        String name = theme == null ? null : theme.getName();
        return name == null ? myId : name;
    }

    @Override
    public boolean isDark() {
        UITheme theme = getTheme();
        return theme != null && theme.isDark();
    }
}
