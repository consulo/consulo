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
package com.intellij.ide.ui.laf;

import com.intellij.ide.ui.BasicEditorTextFieldUI;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.impl.status.BasicStatusBarUI;
import com.intellij.ui.components.OnOffButton;
import consulo.desktop.ui.laf.ui.basic.BasicCaptionPanelUI;
import consulo.ide.ui.laf.intellij.ActionButtonUI;
import consulo.ide.ui.laf.intellij.IntelliJEditorTabsUI;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
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
  public static void initInputMapDefaults(UIDefaults defaults) {
    // Make ENTER work in JTrees
    InputMap treeInputMap = (InputMap)defaults.get("Tree.focusInputMap");
    if (treeInputMap != null) { // it's really possible. For example,  GTK+ doesn't have such map
      treeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggle");
    }
    // Cut/Copy/Paste in JTextAreas
    InputMap textAreaInputMap = (InputMap)defaults.get("TextArea.focusInputMap");
    if (textAreaInputMap != null) { // It really can be null, for example when LAF isn't properly initialized (Alloy license problem)
      installCutCopyPasteShortcuts(textAreaInputMap, false);
    }
    // Cut/Copy/Paste in JTextFields
    InputMap textFieldInputMap = (InputMap)defaults.get("TextField.focusInputMap");
    if (textFieldInputMap != null) { // It really can be null, for example when LAF isn't properly initialized (Alloy license problem)
      installCutCopyPasteShortcuts(textFieldInputMap, false);
    }
    // Cut/Copy/Paste in JPasswordField
    InputMap passwordFieldInputMap = (InputMap)defaults.get("PasswordField.focusInputMap");
    if (passwordFieldInputMap != null) { // It really can be null, for example when LAF isn't properly initialized (Alloy license problem)
      installCutCopyPasteShortcuts(passwordFieldInputMap, false);
    }
    // Cut/Copy/Paste in JTables
    InputMap tableInputMap = (InputMap)defaults.get("Table.ancestorInputMap");
    if (tableInputMap != null) { // It really can be null, for example when LAF isn't properly initialized (Alloy license problem)
      installCutCopyPasteShortcuts(tableInputMap, true);
    }
  }

  public static void insertCustomComponentUI(UIDefaults uiDefaults) {
    if (uiDefaults.get("ActionButtonUI") == null) {
      uiDefaults.put("ActionButtonUI", ActionButtonUI.class.getName());
    }

    if (uiDefaults.get("JBEditorTabsUI") == null) {
      uiDefaults.put("JBEditorTabsUI", IntelliJEditorTabsUI.class.getName());
    }

    if (uiDefaults.get("IdeStatusBarUI") == null) {
      uiDefaults.put("IdeStatusBarUI", BasicStatusBarUI.class.getName());
    }

    if (uiDefaults.get("ActionButtonUI") == null) {
      uiDefaults.put("ActionButtonUI", ActionButtonUI.class.getName());
    }

    if (uiDefaults.get("EditorTextFieldUI") == null) {
      uiDefaults.put("EditorTextFieldUI", BasicEditorTextFieldUI.class.getName());
    }

    if(uiDefaults.get("CaptionPanelUI") == null) {
      uiDefaults.put("CaptionPanelUI", BasicCaptionPanelUI.class.getName());
    }

    if (uiDefaults.get("OnOffButtonUI") == null) {
      uiDefaults.put("OnOffButtonUI", OnOffButton.OnOffButtonUI.class.getName());
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static void initFontDefaults(@Nonnull UIDefaults defaults, @Nonnull FontUIResource uiFont) {
    defaults.put("Tree.ancestorInputMap", null);
    FontUIResource textFont = new FontUIResource(uiFont);
    FontUIResource monoFont = new FontUIResource("Monospaced", Font.PLAIN, uiFont.getSize());

    for (String fontResource : ourPatchableFontResources) {
      defaults.put(fontResource, uiFont);
    }

    if (!SystemInfo.isMac) {
      defaults.put("PasswordField.font", monoFont);
    }
    defaults.put("TextArea.font", monoFont);
    defaults.put("TextPane.font", textFont);
    defaults.put("EditorPane.font", textFont);
  }

  private static void installCutCopyPasteShortcuts(InputMap inputMap, boolean useSimpleActionKeys) {
    String copyActionKey = useSimpleActionKeys ? "copy" : DefaultEditorKit.copyAction;
    String pasteActionKey = useSimpleActionKeys ? "paste" : DefaultEditorKit.pasteAction;
    String cutActionKey = useSimpleActionKeys ? "cut" : DefaultEditorKit.cutAction;
    // Ctrl+Ins, Shift+Ins, Shift+Del
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK), copyActionKey);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.SHIFT_MASK | InputEvent.SHIFT_DOWN_MASK), pasteActionKey);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.SHIFT_MASK | InputEvent.SHIFT_DOWN_MASK), cutActionKey);
    // Ctrl+C, Ctrl+V, Ctrl+X
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK), copyActionKey);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK), pasteActionKey);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK | InputEvent.CTRL_DOWN_MASK), DefaultEditorKit.cutAction);
  }
}
