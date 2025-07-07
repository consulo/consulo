/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcsUtil;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author irengrig
 */
public abstract class MoreAction  extends AnAction implements CustomComponentAction {
  public static final String LOAD_MORE = "Load more";
  protected final JLabel myLabel;
  private final JPanel myPanel;
  private boolean myEnabled;
  private boolean myVisible;
  private JButton myLoadMoreBtn;

  protected MoreAction() {
    this(LOAD_MORE);
  }

  protected MoreAction(final String name) {
    myPanel = new JPanel();
    final BoxLayout layout = new BoxLayout(myPanel, BoxLayout.X_AXIS);
    myPanel.setLayout(layout);
    myLoadMoreBtn = new JButton(name);
    myLoadMoreBtn.setMargin(new Insets(2, 2, 2, 2));
    myLoadMoreBtn.addActionListener(new ActionListener() {
      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull ActionEvent e) {
        MoreAction.this.actionPerformed(null);
      }
    });
    myPanel.add(myLoadMoreBtn);
    myLabel = new JLabel("Loading...");
    myLabel.setForeground(UIUtil.getInactiveTextColor());
    myLabel.setBorder(BorderFactory.createEmptyBorder(1, 3, 1, 1));
    myPanel.add(myLabel);
  }

  @Override
  public JComponent createCustomComponent(Presentation presentation, String place) {
    return myPanel;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
    myLoadMoreBtn.setVisible(myEnabled);
    myLabel.setVisible(! myEnabled);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(myEnabled);
    e.getPresentation().setVisible(myVisible);
  }

  public void setVisible(boolean b) {
    myVisible = b;
  }
}
