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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.fileEditor.FileEditorManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.project.startup.IdeaStartupActivity;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 * Date: 4/11/12
 */
public class OpenFilesActivity implements IdeaStartupActivity.DumbAware {
  @Override
  public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    if (!(fileEditorManager instanceof FileEditorManagerImpl)) {
      return;
    }

    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText("Reopening files...");
    }

    final FileEditorManagerImpl manager = (FileEditorManagerImpl)fileEditorManager;
    manager.getMainSplitters().openFiles(uiAccess);
    manager.initDockableContentFactory();
  }
}
