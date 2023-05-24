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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ConfigurationErrorQuickFix;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.update.Activatable;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactErrorPanel {
  private JPanel myMainPanel;
  private JButton myFixButton;
  private JLabel myErrorLabel;
  private List<? extends ConfigurationErrorQuickFix> myCurrentQuickFixes;
  private String myErrorText;

  public ArtifactErrorPanel(final ArtifactEditorImpl artifactEditor) {
    myErrorLabel.setIcon(TargetAWT.to(PlatformIconGroup.generalError()));
    new UiNotifyConnector(myMainPanel, new Activatable() {
      @Override
      public void showNotify() {
        if (myErrorText != null) {
          myErrorLabel.setText(myErrorText);
          myErrorText = null;
        }
      }
    });
    myFixButton.addActionListener(e -> {
      if (!myCurrentQuickFixes.isEmpty()) {
        if (myCurrentQuickFixes.size() == 1) {
          performFix(ContainerUtil.getFirstItem(myCurrentQuickFixes, null), artifactEditor);
        }
        else {
          JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<ConfigurationErrorQuickFix>(null, myCurrentQuickFixes) {
            @Nonnull
            @Override
            public String getTextFor(ConfigurationErrorQuickFix value) {
              return value.getActionName();
            }

            @Override
            public PopupStep onChosen(ConfigurationErrorQuickFix selectedValue, boolean finalChoice) {
              performFix(selectedValue, artifactEditor);
              return FINAL_CHOICE;
            }
          }).showUnderneathOf(myFixButton);
        }
      }
    });
    clearError();
  }

  private void performFix(ConfigurationErrorQuickFix quickFix, ArtifactEditorImpl artifactEditor) {
    quickFix.performFix(DataManager.getInstance().getDataContext(myFixButton));
    artifactEditor.queueValidation();
  }

  public void showError(@Nonnull String message, @Nonnull List<? extends ConfigurationErrorQuickFix> quickFixes) {
    myErrorLabel.setVisible(true);
    final String errorText = XmlStringUtil.wrapInHtml(message);
    if (myErrorLabel.isShowing()) {
      myErrorLabel.setText(errorText);
    }
    else {
      myErrorText = errorText;
    }
    myMainPanel.setVisible(true);
    myCurrentQuickFixes = quickFixes;
    myFixButton.setVisible(!quickFixes.isEmpty());
    if (!quickFixes.isEmpty()) {
      myFixButton.setText(quickFixes.size() == 1 ? ContainerUtil.getFirstItem(quickFixes, null).getActionName() : "Fix...");
    }
  }

  public void clearError() {
    myErrorText = null;
    myMainPanel.setVisible(false);
    myErrorLabel.setVisible(false);
    myFixButton.setVisible(false);
  }

  public JComponent getMainPanel() {
    return myMainPanel;
  }
}
