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
package consulo.desktop.awt.wm.impl;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * The IDE main menu bar abstraction. Implemented by the Swing {@link DefaultIdeMenuBar} and by the native
 * macOS screen menu bar {@code consulo.desktop.awt.wm.impl.mac.MacScreenIdeMenuBar}.
 *
 * @author VISTALL
 */
public interface IdeMenuBar {
    @RequiredUIAccess
    void updateMenuActions();

    /**
     * Attaches this menu bar to the given root pane (e.g. {@code setJMenuBar}, layered pane, or nothing for a native
     * menu bar) and performs any initial fill.
     */
    void install(IdeRootPane rootPane);

    default void dispose() {
    }
}
