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
package consulo.ide.impl.idea.xdebugger.impl.ui;

import consulo.codeEditor.Editor;
import consulo.execution.debug.XDebuggerActions;
import consulo.ide.impl.idea.openapi.actionSystem.CompositeShortcutSet;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.LinkLabel;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: zajac
 * Date: 4/4/12
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class BreakpointEditor {
  public JPanel getMainPanel() {
    return myMainPanel;
  }

  private void createUIComponents() {
    AnAction action = ActionManager.getInstance().getAction(XDebuggerActions.VIEW_BREAKPOINTS);
    String shortcutText = action != null ? KeymapUtil.getFirstKeyboardShortcutText(action) : null;
    String text = shortcutText != null ? "More (" + shortcutText + ")" : "More";
    myShowMoreOptionsLink = new LinkLabel(text, null, (aSource, aLinkData) -> {
      if (myDelegate != null) {
        myDelegate.more();
      }
    });
  }

  public void setShowMoreOptionsLink(boolean b) {
    myShowMoreOptionsLink.setVisible(b);
  }

  public interface Delegate {
    void done();
    void more();
  }

  private JPanel myMainPanel;
  private JButton myDoneButton;
  private JPanel myPropertiesPlaceholder;
  private LinkLabel myShowMoreOptionsLink;
  private Delegate myDelegate;

  public BreakpointEditor() {
    myDoneButton.addActionListener(actionEvent -> done());

    final AnAction doneAction = new AnAction() {
      @Override
      public void update(AnActionEvent e) {
        super.update(e);
        boolean lookup = LookupManager.getInstance(e == null ? null : e.getData(Project.KEY)).getActiveLookup() != null;
        Editor editor = e.getData(Editor.KEY);
        e.getPresentation().setEnabled(!lookup && (editor == null || StringUtil.isEmpty(editor.getSelectionModel().getSelectedText())) );
      }

      public void actionPerformed(AnActionEvent e) {
        done();
      }
    };
    doneAction.registerCustomShortcutSet(new CompositeShortcutSet(CustomShortcutSet.fromString("ESCAPE"), CustomShortcutSet.fromString("ENTER")), myMainPanel);
  }

  private void done() {
    if (myDelegate != null) {
      myDelegate.done();
    }
  }

  public void setPropertiesPanel(JComponent p) {
    myPropertiesPlaceholder.removeAll();
    myPropertiesPlaceholder.add(p, BorderLayout.CENTER);
  }

  public void setDelegate(Delegate d) {
    myDelegate = d;
  }
}
