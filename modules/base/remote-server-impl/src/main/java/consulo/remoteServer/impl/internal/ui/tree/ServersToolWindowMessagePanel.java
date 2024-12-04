// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.ui.tree;

import consulo.remoteServer.CloudBundle;
import consulo.remoteServer.impl.internal.ui.RemoteServersDeploymentManager;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.util.LafProperty;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public class ServersToolWindowMessagePanel implements RemoteServersDeploymentManager.MessagePanel {
  private JPanel myPanel;
  private JEditorPane myMessageArea;
  private String myCurrentText;

  public ServersToolWindowMessagePanel() {
    myMessageArea.setBackground(UIUtil.getPanelBackground());
    myMessageArea.setBorder(JBUI.Borders.empty());
    if (myMessageArea.getCaret() != null) {
      myMessageArea.setCaretPosition(0);
    }
    myMessageArea.setEditable(false);
  }

  @Override
  public void setEmptyText(@Nonnull String text) {
    if (text.equals(myCurrentText)) {
      return;
    }
    myMessageArea.setText(CloudBundle.message("editor.pane.text.empty.text", UIUtil.getCssFontDeclaration(UIUtil.getLabelFont(), null, null, null),
                                              ColorUtil.toHex(UIUtil.getPanelBackground()), ColorUtil.toHex(
            LafProperty.getInactiveTextColor()), text));
    myCurrentText = text;
  }

  @Override
  public @Nonnull JComponent getComponent() {
    return myPanel;
  }
}
