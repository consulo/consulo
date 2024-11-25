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

import com.formdev.flatlaf.FlatDefaultsAddon;
import consulo.desktop.awt.ui.plaf.BasicStatusBarUI;
import consulo.desktop.awt.ui.plaf.darcula.DarculaCaptionPanelUI;
import consulo.desktop.awt.ui.plaf.darcula.DarculaEditorTextFieldUI;
import consulo.desktop.awt.ui.plaf.intellij.IntelliJEditorTabsUI;
import consulo.desktop.awt.uiOld.components.OnOffButton;

import javax.swing.*;
import java.io.InputStream;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
public class ConsuloFlatDefaultsAddon extends FlatDefaultsAddon {
    @Override
    public InputStream getDefaults(Class<?> lafClass) {
        return null;
    }

    @Override
    public void afterDefaultsLoading(LookAndFeel laf, UIDefaults uiDefaults) {
        uiDefaults.put("JBEditorTabsUI", IntelliJEditorTabsUI.class.getName());
        uiDefaults.put("IdeStatusBarUI", BasicStatusBarUI.class.getName());
        uiDefaults.put("EditorTextFieldUI", DarculaEditorTextFieldUI.class.getName());
        uiDefaults.put("CaptionPanelUI", DarculaCaptionPanelUI.class.getName());
        uiDefaults.put("OnOffButtonUI", OnOffButton.OnOffButtonUI.class.getName());

        uiDefaults.put("ComboBoxButtonUI", FlatComboBoxButtonUI.class.getName());
    }
}
