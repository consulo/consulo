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
import consulo.desktop.awt.ui.plaf.intellij.IntelliJEditorTabsUI;
import consulo.desktop.awt.uiOld.components.OnOffButton;
import consulo.desktop.awt.ui.plaf.BasicStripeButtonUI;

import javax.swing.*;
import java.awt.*;
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
        uiDefaults.put("StripeButtonUI", BasicStripeButtonUI.class.getName());

        uiDefaults.put("JBEditorTabsUI", IntelliJEditorTabsUI.class.getName());
        uiDefaults.put("IdeStatusBarUI", BasicStatusBarUI.class.getName());
        uiDefaults.put("EditorTextFieldUI", FlatEditorTextFieldUI.class.getName());
        uiDefaults.put("OnOffButtonUI", OnOffButton.OnOffButtonUI.class.getName());

        uiDefaults.put("ComboBoxButtonUI", FlatComboBoxButtonUI.class.getName());
        uiDefaults.put("CaptionPanelUI", FlatCaptionPanelUI.class.getName());

        // disable selecting on focus
        uiDefaults.put("TextComponent.selectAllOnFocusPolicy", "never");

        uiDefaults.put("TreeUI", FlatTreeUIWidePatch.class.getName());
        uiDefaults.put("Tree.wideCellRenderer", true);

        uiDefaults.put("Menu.selectedArrowIcon", new FlatSelectedMenuArrowIcon());

        uiDefaults.put("TitlePane.titleMargins", new Insets(3, 0, 3, 6));
        //uiDefaults.put("FlatLaf.debug.panel.showPlaceholders", true);

        uiDefaults.put("Menu.selectedCheckboxIcon", new FlatSelectedCheckboxIcon());
    }
}
