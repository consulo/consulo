/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.plaf2.flat;

import consulo.desktop.awt.ui.plaf.BasicCaptionPanelUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;

/**
 * @author VISTALL
 * @since 2024-11-26
 */
public class FlatCaptionPanelUI extends BasicCaptionPanelUI {
    public static ComponentUI createUI(JComponent c) {
        return new FlatCaptionPanelUI();
    }
}
