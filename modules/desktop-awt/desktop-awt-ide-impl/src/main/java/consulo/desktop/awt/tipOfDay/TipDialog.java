
/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.desktop.awt.tipOfDay;

import consulo.platform.base.localize.CommonLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.event.ActionEvent;

public class TipDialog extends DialogWrapper {
  private final TipPanel myTipPanel;

  public TipDialog() {
    super(true);
    setModal(false);
    setTitle(IdeLocalize.titleTipOfTheDay());
    setCancelButtonText(CommonLocalize.buttonClose().get());
    myTipPanel = new TipPanel();
    myTipPanel.nextTip();
    setDoNotAskOption(myTipPanel);
    setHorizontalStretch(1.33f);
    setVerticalStretch(1.25f);
    init();
  }

  @Nullable
  @Override
  protected Border createContentPaneBorder() {
    return JBUI.Borders.empty();
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    JComponent panel = super.createSouthPanel();
    assert panel != null;
    panel.setBorder(new CompoundBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0), super.createContentPaneBorder()));
    return panel;
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    return new Action[]{new PreviousTipAction(), new NextTipAction(), getCancelAction()};
  }

  @Override
  protected JComponent createCenterPanel() {
    return myTipPanel;
  }

  private class PreviousTipAction extends AbstractAction {
    public PreviousTipAction() {
      super(IdeLocalize.actionPreviousTip().get());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      myTipPanel.prevTip();
    }
  }

  private class NextTipAction extends AbstractAction {
    public NextTipAction() {
      super(IdeLocalize.actionNextTip().get());
      putValue(DialogWrapper.DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      myTipPanel.nextTip();
    }
  }
}
