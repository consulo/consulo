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
package consulo.desktop.awt.internal.diff;

import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.desktop.awt.ui.impl.image.DesktopAWTScalableImage;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffUserDataKeys;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.ui.WindowWrapperBuilder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.WindowWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public abstract class DiffWindowBase {
  @Nullable protected final Project myProject;
  @Nonnull
  protected final DiffDialogHints myHints;

  private DiffRequestProcessor myProcessor;
  private WindowWrapper myWrapper;

  public DiffWindowBase(@Nullable Project project, @Nonnull DiffDialogHints hints) {
    myProject = project;
    myHints = hints;
  }

  protected void init() {
    if (myWrapper != null) return;

    myProcessor = createProcessor();

    String dialogGroupKey = myProcessor.getContextUserData(DiffUserDataKeys.DIALOG_GROUP_KEY);
    if (dialogGroupKey == null) dialogGroupKey = "DiffContextDialog";

    myWrapper = new WindowWrapperBuilder(AWTDiffUtil.getWindowMode(myHints), new MyPanel(myProcessor.getComponent()))
            .setProject(myProject)
            .setParent(myHints.getParent())
            .setDimensionServiceKey(dialogGroupKey)
            .setOnShowCallback(new Runnable() {
              @Override
              public void run() {
                myProcessor.updateRequest();
                myProcessor.requestFocus(); // TODO: not needed for modal dialogs. Make a flag in WindowWrapperBuilder ?
              }
            })
            .build();
    myWrapper.setImage(new DesktopAWTScalableImage(PlatformIconGroup.actionsDiff()));
    Disposer.register(myWrapper, myProcessor);

    new DumbAwareAction() {
      public void actionPerformed(final AnActionEvent e) {
        myWrapper.close();
      }
    }.registerCustomShortcutSet(CommonShortcuts.getCloseActiveWindow(), myProcessor.getComponent());
  }

  public void show() {
    init();
    myWrapper.show();
  }

  @Nonnull
  protected abstract DiffRequestProcessor createProcessor();

  //
  // Delegate
  //

  protected void setWindowTitle(@Nonnull String title) {
    myWrapper.setTitle(title);
  }

  protected void onAfterNavigate() {
    AWTDiffUtil.closeWindow(myWrapper.getWindow(), true, true);
  }

  //
  // Getters
  //

  protected WindowWrapper getWrapper() {
    return myWrapper;
  }

  protected DiffRequestProcessor getProcessor() {
    return myProcessor;
  }

  private static class MyPanel extends JPanel {
    public MyPanel(@Nonnull JComponent content) {
      super(new BorderLayout());
      add(content, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension windowSize = AWTDiffUtil.getDefaultDiffWindowSize();
      Dimension size = super.getPreferredSize();
      return new Dimension(Math.max(windowSize.width, size.width), Math.max(windowSize.height, size.height));
    }
  }
}
