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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.CommonBundle;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.ui.ex.awt.OptionsDialog;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Eugene Zhuravlev
 *         Date: Jun 2, 2005
 */
public class ChangelistMoveOfferDialog extends OptionsDialog {
  private final VcsConfiguration myConfig;

  public ChangelistMoveOfferDialog(VcsConfiguration config) {
    super(false);
    myConfig = config;
    setTitle(VcsBundle.message("changes.commit.partial.offer.to.move.title"));
    init();
  }

  @Nonnull
  protected Action[] createActions() {
    setOKButtonText(CommonBundle.getYesButtonText());
    setCancelButtonText(CommonBundle.getNoButtonText());
    return new Action[] {getOKAction(), getCancelAction()};
  }

  protected boolean isToBeShown() {
    return myConfig.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT;
  }

  protected void setToBeShown(boolean value, boolean onOk) {
    myConfig.OFFER_MOVE_TO_ANOTHER_CHANGELIST_ON_PARTIAL_COMMIT = value;
  }

  protected boolean shouldSaveOptionsOnCancel() {
    return true;
  }

  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    final JLabel label = new JLabel(VcsBundle.message("changes.commit.partial.offer.to.move.text"));
    label.setUI(new MultiLineLabelUI());
    label.setIconTextGap(10);
    label.setIcon(TargetAWT.to(Messages.getQuestionIcon()));
    panel.add(label, BorderLayout.CENTER);
    panel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
    return panel;
  }
}
