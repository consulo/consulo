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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;


import consulo.application.HelpManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class KeyboardShortcutDialog extends DialogWrapper {
  private StrokePanel myFirstStrokePanel;
  private StrokePanel mySecondStrokePanel;
  private final JCheckBox myEnableSecondKeystroke;
  private final JLabel myKeystrokePreview;
  private final JTextArea myConflictInfoArea;
  private Keymap myKeymap;
  private final String myActionId;
  private final KeymapGroupImpl myMainGroup;

  public KeyboardShortcutDialog(Component component, String actionId, final QuickList[] quickLists) {
    super(component, true);
    setTitle(KeyMapLocalize.keyboardShortcutDialogTitle());
    myActionId = actionId;
    final Project project = DataManager.getInstance().getDataContext(component).getData(Project.KEY);
    myMainGroup = ActionsTreeUtil.createMainGroup(project, myKeymap, quickLists, null, false, null); //without current filter
    myEnableSecondKeystroke = new JCheckBox();
    UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myEnableSecondKeystroke);
    myEnableSecondKeystroke.setBorder(new EmptyBorder(4, 0, 0, 2));
    myEnableSecondKeystroke.setFocusable(false);
    myKeystrokePreview = new JLabel(" ");
    myConflictInfoArea = new JTextArea("");
    myConflictInfoArea.setFocusable(false);
    init();
  }

  @Override
  @Nonnull
  protected Action[] createActions(){
    return new Action[]{getOKAction(),getCancelAction(),getHelpAction()};
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridBagLayout());

    // First stroke

    myFirstStrokePanel = new StrokePanel(KeyMapLocalize.firstStrokePanelTitle().get());
    panel.add(
      myFirstStrokePanel,
      new GridBagConstraints(
        0,0,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,
        JBUI.emptyInsets(),0,0
      )
    );

    // Second stroke panel

    panel.add(
      myEnableSecondKeystroke,
      new GridBagConstraints(
        0,1,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,
        JBUI.emptyInsets(),0,0
      )
    );

    mySecondStrokePanel = new StrokePanel(KeyMapLocalize.secondStrokePanelTitle().get());
    panel.add(
      mySecondStrokePanel,
      new GridBagConstraints(
        1,1,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,
        JBUI.emptyInsets(),0,0
      )
    );

    // Shortcut preview

    JPanel previewPanel = new JPanel(new BorderLayout());
    previewPanel.setBorder(IdeBorderFactory.createTitledBorder(KeyMapLocalize.shortcutPreviewIdeBorderFactoryTitle().get(), true));
    previewPanel.add(myKeystrokePreview);
    panel.add(
      previewPanel,
      new GridBagConstraints(
        0,2,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,
        JBUI.emptyInsets(),0,0
      )
    );

    // Conflicts

    JPanel conflictsPanel = new JPanel(new BorderLayout());
    conflictsPanel.setBorder(IdeBorderFactory.createTitledBorder(KeyMapLocalize.conflictsIdeBorderFactoryTitle().get(), true));
    myConflictInfoArea.setEditable(false);
    myConflictInfoArea.setBackground(panel.getBackground());
    myConflictInfoArea.setLineWrap(true);
    myConflictInfoArea.setWrapStyleWord(true);
    final JScrollPane conflictInfoScroll = ScrollPaneFactory.createScrollPane(myConflictInfoArea);
    conflictInfoScroll.setPreferredSize(new Dimension(260, 60));
    conflictInfoScroll.setBorder(null);
    conflictsPanel.add(conflictInfoScroll);
    panel.add(
      conflictsPanel,
      new GridBagConstraints(
        0,3,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,
        JBUI.emptyInsets(),0,0
      )
    );

    myEnableSecondKeystroke.addActionListener(e -> {
        handleSecondKey();
        updateCurrentKeyStrokeInfo();

        /** TODO[anton]????  */
        if (myEnableSecondKeystroke.isSelected()) {
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(mySecondStrokePanel.getShortcutTextField());
        }
        else {
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myFirstStrokePanel.getShortcutTextField());
        }
      });
    return panel;
  }

  public JComponent getPreferredFocusedComponent(){
    return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myFirstStrokePanel);
  }

  public void setData(Keymap keymap, KeyboardShortcut shortcut) {
    myKeymap = keymap;
    myEnableSecondKeystroke.setSelected(false);
    if (shortcut != null) {
      myFirstStrokePanel.getShortcutTextField().setKeyStroke(shortcut.getFirstKeyStroke());
      if (shortcut.getSecondKeyStroke() != null) {
        myEnableSecondKeystroke.setSelected(true);
        mySecondStrokePanel.getShortcutTextField().setKeyStroke(shortcut.getSecondKeyStroke());
      }
    }
    handleSecondKey();
    updateCurrentKeyStrokeInfo();
  }

  private void updateCurrentKeyStrokeInfo() {
    if (myConflictInfoArea == null || myKeystrokePreview == null){
      return;
    }

    myConflictInfoArea.setText(null);
    myKeystrokePreview.setText(" ");

    if (myKeymap == null){
      return;
    }

    KeyboardShortcut keyboardShortcut = getKeyboardShortcut();
    if (keyboardShortcut == null){
      return;
    }

    String strokeText = getTextByKeyStroke(keyboardShortcut.getFirstKeyStroke());
    String suffixText = getTextByKeyStroke(keyboardShortcut.getSecondKeyStroke());
    if (suffixText != null && suffixText.length() > 0) {
      strokeText += ',' + suffixText;
    }
    myKeystrokePreview.setText(strokeText);

    StringBuilder buffer = new StringBuilder();

    Map<String, ArrayList<KeyboardShortcut>> conflicts = myKeymap.getConflicts(myActionId, keyboardShortcut);

    Set<String> keys = conflicts.keySet();
    String[] actionIds = ArrayUtil.toStringArray(keys);
    boolean loaded = true;
    for (String actionId : actionIds) {
      String actionPath = myMainGroup.getActionQualifiedPath(actionId);
      if (actionPath == null) {
        loaded = false;
      }
      if (buffer.length() > 1) {
        buffer.append('\n');
      }
      buffer.append('[');
      buffer.append(actionPath != null ? actionPath : actionId);
      buffer.append(']');
    }

    if (buffer.length() == 0) {
      myConflictInfoArea.setForeground(UIUtil.getTextAreaForeground());
      myConflictInfoArea.setText(KeyMapLocalize.noConflictInfoMessage().get());
    }
    else {
      myConflictInfoArea.setForeground(JBColor.RED);
      if (loaded) {
        myConflictInfoArea.setText(KeyMapLocalize.assignedToInfoMessage(buffer.toString()).get());
      } else {
        myConflictInfoArea.setText("Assigned to " + buffer.toString() + " which is now not loaded but may be loaded later");
      }
    }
  }

  private void handleSecondKey() {
    mySecondStrokePanel.setEnabled(myEnableSecondKeystroke.isSelected());
  }

  public KeyboardShortcut getKeyboardShortcut() {
    KeyStroke firstStroke = myFirstStrokePanel.getKeyStroke();
    if (firstStroke == null) {
      return null;
    }
    KeyStroke secondStroke = myEnableSecondKeystroke.isSelected() ? mySecondStrokePanel.getKeyStroke() : null;
    return new KeyboardShortcut(firstStroke, secondStroke);
  }

  static String getTextByKeyStroke(KeyStroke keyStroke) {
    if (keyStroke == null) {
      return "";
    }
    return KeymapUtil.getKeystrokeText(keyStroke);
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp("preferences.keymap.shortcut");
  }

  private class StrokePanel extends JPanel {
    private final ShortcutTextField myShortcutTextField;

    public StrokePanel(String borderText) {
      setLayout(new BorderLayout());
      setBorder(IdeBorderFactory.createTitledBorder(borderText, false));

      myShortcutTextField = new ShortcutTextField(){
        @Override
        protected void updateCurrentKeyStrokeInfo() {
          KeyboardShortcutDialog.this.updateCurrentKeyStrokeInfo();
        }
      };
      add(myShortcutTextField);
    }

    public ShortcutTextField getShortcutTextField() {
      return myShortcutTextField;
    }

    @Override
    public void setEnabled(boolean state) {
      myShortcutTextField.setEnabled(state);
      repaint();
    }

    public KeyStroke getKeyStroke() {
      return myShortcutTextField.getKeyStroke();
    }
  }
}
