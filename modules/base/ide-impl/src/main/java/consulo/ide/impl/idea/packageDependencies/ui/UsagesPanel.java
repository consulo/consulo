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

package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.usage.UsageViewManager;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.disposer.Disposable;
import consulo.dataContext.DataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.util.Alarm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public abstract class UsagesPanel extends JPanel implements Disposable, DataProvider {
  protected static final Logger LOG = Logger.getInstance(UsagesPanel.class);

  private final Project myProject;
  protected ProgressIndicator myCurrentProgress;
  private JComponent myCurrentComponent;
  private UsageView myCurrentUsageView;
  protected final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);

  public UsagesPanel(@Nonnull Project project) {
    super(new BorderLayout());
    myProject = project;
  }

  public void setToInitialPosition() {
    cancelCurrentFindRequest();
    setToComponent(createLabel(getInitialPositionText()));
  }

  public abstract String getInitialPositionText();

  public abstract String getCodeUsagesString();


  void cancelCurrentFindRequest() {
    if (myCurrentProgress != null) {
      myCurrentProgress.cancel();
    }
  }

  protected void showUsages(@Nonnull PsiElement[] primaryElements, @Nonnull UsageInfo[] usageInfos) {
    if (myCurrentUsageView != null) {
      Disposer.dispose(myCurrentUsageView);
    }
    try {
      Usage[] usages = UsageInfoToUsageConverter.convert(primaryElements, usageInfos);
      UsageViewPresentation presentation = new UsageViewPresentation();
      presentation.setCodeUsagesString(getCodeUsagesString());
      myCurrentUsageView = UsageViewManager.getInstance(myProject).createUsageView(UsageTarget.EMPTY_ARRAY, usages, presentation, null);
      setToComponent(myCurrentUsageView.getComponent());
    }
    catch (ProcessCanceledException e) {
      setToCanceled();
    }
  }

  private void setToCanceled() {
    setToComponent(createLabel(AnalysisScopeBundle.message("usage.view.canceled")));
  }

  void setToComponent(final JComponent cmp) {
    SwingUtilities.invokeLater(() -> {
      if (myProject.isDisposed()) return;
      if (myCurrentComponent != null) {
        if (myCurrentUsageView != null && myCurrentComponent == myCurrentUsageView.getComponent()) {
          Disposer.dispose(myCurrentUsageView);
          myCurrentUsageView = null;
        }
        remove(myCurrentComponent);
      }
      myCurrentComponent = cmp;
      add(cmp, BorderLayout.CENTER);
      revalidate();
    });
  }

  @Override
  public void dispose() {
    if (myCurrentUsageView != null) {
      Disposer.dispose(myCurrentUsageView);
      myCurrentUsageView = null;
    }
  }

  private static JComponent createLabel(String text) {
    JLabel label = new JLabel(text);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    return label;
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Key dataId) {
    if (PlatformDataKeys.HELP_ID == dataId) {
      return "ideaInterface.find";
    }
    return null;
  }
}
