// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.codeStyle.statusbar;

import consulo.application.util.SystemInfo;
import com.intellij.openapi.wm.impl.status.TextPanel;
import consulo.ui.ex.awt.JBFont;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class CodeStyleStatusBarPanel extends JPanel {
  private final TextPanel myLabel;
  private final JLabel myIconLabel;

  public CodeStyleStatusBarPanel() {
    super();
    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setAlignmentY(Component.CENTER_ALIGNMENT);
    myLabel = new TextPanel() {
    };
    myLabel.setFont(SystemInfo.isMac ? JBUI.Fonts.label(11) : JBFont.label());
    add(myLabel);
    myIconLabel = new JLabel("");
    myIconLabel.setBorder(JBUI.Borders.empty(2, 2, 2, 0));
    add(myIconLabel);
    setBorder(JBUI.Borders.empty(0));
  }

  public void setText(@Nonnull @Nls String text) {
    myLabel.setText(text);
  }

  @Nullable
  public String getText() {
    return myLabel.getText();
  }

  public void setIcon(@Nullable Image icon) {
    myIconLabel.setIcon(TargetAWT.to(icon));
    myIconLabel.setVisible(icon != null);
  }
}
