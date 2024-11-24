/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.plaf;

import consulo.platform.Platform;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class LafManagerImplUtil {
    @NonNls
    public static final String[] ourPatchableFontResources =
        {"Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font", "ColorChooser.font", "ComboBox.font", "Label.font", "List.font", "MenuBar.font", "MenuItem.font",
            "MenuItem.acceleratorFont", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "Menu.font", "PopupMenu.font", "OptionPane.font", "Panel.font", "ProgressBar.font",
            "ScrollPane.font", "Viewport.font", "TabbedPane.font", "Table.font", "TableHeader.font", "TextField.font", "PasswordField.font", "TextArea.font", "TextPane.font", "EditorPane.font",
            "TitledBorder.font", "ToolBar.font", "ToolTip.font", "Tree.font", "FormattedTextField.font", "Spinner.font"};


    @SuppressWarnings({"HardCodedStringLiteral"})
    public static void initFontDefaults(@Nonnull UIDefaults defaults, @Nonnull FontUIResource uiFont) {
        defaults.put("Tree.ancestorInputMap", null);
        FontUIResource textFont = new FontUIResource(uiFont);
        FontUIResource monoFont = new FontUIResource("Monospaced", Font.PLAIN, uiFont.getSize());

        for (String fontResource : ourPatchableFontResources) {
            defaults.put(fontResource, uiFont);
        }

        if (!Platform.current().os().isMac()) {
            defaults.put("PasswordField.font", monoFont);
        }
        defaults.put("TextArea.font", monoFont);
        defaults.put("TextPane.font", textFont);
        defaults.put("EditorPane.font", textFont);
    }
}
