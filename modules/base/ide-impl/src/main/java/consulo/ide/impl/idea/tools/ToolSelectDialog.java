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
package consulo.ide.impl.idea.tools;

import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.Balloon;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;
import java.io.IOException;

class ToolSelectDialog extends DialogWrapper {
  private final BaseToolsPanel myToolsPanel;

  protected ToolSelectDialog(@Nullable Project project, @Nullable String actionIdToSelect, BaseToolsPanel toolsPanel) {
    super(project);
    myToolsPanel = toolsPanel;
    myToolsPanel.reset();
    setOKActionEnabled(myToolsPanel.getSingleSelectedTool() != null);
    myToolsPanel.addSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        setOKActionEnabled(myToolsPanel.getSingleSelectedTool() != null);
      }
    });
    init();
    pack();
    if (actionIdToSelect != null) {
      myToolsPanel.selectTool(actionIdToSelect);
    }
    setTitle(ToolsBundle.message("tools.dialog.title"));
  }

  @Override
  protected void doOKAction() {
    try {
      myToolsPanel.apply();
    }
    catch (IOException e) {
      String message = ToolsBundle.message("tools.failed.to.save.changes.0", StringUtil.decapitalize(e.getMessage()));
      final JLayeredPane pane = myToolsPanel.getRootPane().getLayeredPane();
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, NotificationType.ERROR, null)
        .setShowCallout(false).setFadeoutTime(3000).setHideOnAction(true).setHideOnClickOutside(true).setHideOnKeyOutside(true).
        createBalloon().show(new RelativePoint(pane, new Point(pane.getWidth(), 0)), Balloon.Position.above);
      return;
    }
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myToolsPanel;
  }

  @Nullable
  Tool getSelectedTool() {
    return myToolsPanel.getSingleSelectedTool();
  }

  boolean isModified() {
    return myToolsPanel.isModified();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "consulo.ide.impl.idea.tools.ToolSelectDialog.dimensionServiceKey";
  }
}
