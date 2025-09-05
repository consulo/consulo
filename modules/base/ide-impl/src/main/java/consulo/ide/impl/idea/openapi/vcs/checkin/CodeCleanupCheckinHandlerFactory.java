/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.checkin;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.versionControlSystem.checkin.CheckinHandlerFactory;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.impl.internal.checkin.CheckinMetaHandler;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.NonFocusableCheckBox;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

@ExtensionImpl(id = "code-cleanup", order = "after todo")
public class CodeCleanupCheckinHandlerFactory extends CheckinHandlerFactory {
  @Override
  @Nonnull
  public CheckinHandler createHandler(@Nonnull CheckinProjectPanel panel, @Nonnull CommitContext commitContext) {
    return new CleanupCodeCheckinHandler(panel);
  }

  private static class CleanupCodeCheckinHandler extends CheckinHandler implements CheckinMetaHandler {
    private final CheckinProjectPanel myPanel;
    private Project myProject;

    public CleanupCodeCheckinHandler(CheckinProjectPanel panel) {
      myProject = panel.getProject();
      myPanel = panel;
    }

    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
      final JCheckBox cleanupCodeCb = new NonFocusableCheckBox(VcsBundle.message("before.checkin.cleanup.code"));
      return new RefreshableOnComponent() {
        @Override
        public JComponent getComponent() {
          JPanel cbPanel = new JPanel(new BorderLayout());
          cbPanel.add(cleanupCodeCb, BorderLayout.WEST);
          CheckinHandlerUtil
                  .disableWhenDumb(myProject, cleanupCodeCb, "Code analysis is impossible until indices are up-to-date");
          return cbPanel;
        }

        @Override
        public void refresh() {
        }

        @Override
        public void saveState() {
          VcsConfiguration.getInstance(myProject).CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT = cleanupCodeCb.isSelected();
        }

        @Override
        public void restoreState() {
          cleanupCodeCb.setSelected(VcsConfiguration.getInstance(myProject).CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT);
        }
      };
    }


    @Override
    public void runCheckinHandlers(Runnable runnable) {
      if (VcsConfiguration.getInstance(myProject).CHECK_CODE_CLEANUP_BEFORE_PROJECT_COMMIT  && !DumbService.isDumb(myProject)) {

        List<VirtualFile> filesToProcess = CheckinHandlerUtil.filterOutGeneratedAndExcludedFiles(myPanel.getVirtualFiles(), myProject);
        GlobalInspectionContextBase.codeCleanup(myProject, new AnalysisScope(myProject, filesToProcess), runnable);

      } else {
        runnable.run();
      }
    }
  }
}