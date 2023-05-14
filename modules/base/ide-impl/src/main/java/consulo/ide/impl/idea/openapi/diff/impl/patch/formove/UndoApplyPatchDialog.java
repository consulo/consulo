/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.patch.formove;

import consulo.application.AllIcons;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.FilePathChangesTreeList;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.JBLabel;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;

class UndoApplyPatchDialog extends DialogWrapper {


  private final List<FilePath> myFailedFilePaths;
  private final Project myProject;
  private final boolean myShouldInformAboutBinaries;

  UndoApplyPatchDialog(@Nonnull Project project,
                       @Nonnull List<FilePath> filePaths,
                       boolean shouldInformAboutBinaries) {
    super(project, true);
    myProject = project;
    setTitle("Patch Applying Partly Failed");
    setOKButtonText("Rollback");
    myFailedFilePaths = filePaths;
    myShouldInformAboutBinaries = shouldInformAboutBinaries;
    init();
  }

  @jakarta.annotation.Nullable
  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    int numFiles = myFailedFilePaths.size();
    JPanel labelsPanel = new JPanel(new BorderLayout());
    String detailedText = numFiles == 0 ? "" : String.format("Failed to apply %s below.<br>", StringUtil.pluralize("file", numFiles));
    final JLabel infoLabel = new JBLabel(XmlStringUtil.wrapInHtml(detailedText + "Would you like to rollback all applied?"));
    labelsPanel.add(infoLabel, BorderLayout.NORTH);
    if (myShouldInformAboutBinaries) {
      JLabel warningLabel = new JLabel("Rollback will not affect binaries");
      warningLabel.setIcon(TargetAWT.to(AllIcons.General.BalloonWarning));
      labelsPanel.add(warningLabel, BorderLayout.CENTER);
    }
    panel.add(labelsPanel, BorderLayout.NORTH);
    if (numFiles > 0) {
      FilePathChangesTreeList browser = new FilePathChangesTreeList(myProject, myFailedFilePaths, false, false, null, null) {
        @Override
        public Dimension getPreferredSize() {
          return new Dimension(infoLabel.getPreferredSize().width, 50);
        }
      };
      browser.setChangesToDisplay(myFailedFilePaths);
      panel.add(ScrollPaneFactory.createScrollPane(browser), BorderLayout.CENTER);
    }
    return panel;
  }
}
