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

package consulo.ide.action;

import consulo.dataContext.DataContext;
import consulo.ide.IdeView;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The base abstract class for actions which create new file elements in IDE view
 *
 * @since 15.1
 */
public abstract class CreateInDirectoryActionBase extends AnAction {
  protected CreateInDirectoryActionBase() {
  }

  protected CreateInDirectoryActionBase(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  protected CreateInDirectoryActionBase(@Nonnull LocalizeValue text) {
    super(text);
  }

  protected CreateInDirectoryActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  protected CreateInDirectoryActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent e) {
    if (!e.getPresentation().isVisible()) {
      return;
    }

    final DataContext dataContext = e.getDataContext();
    final Presentation presentation = e.getPresentation();

    final boolean enabled = isAvailable(dataContext);

    presentation.setVisible(enabled);
    presentation.setEnabled(enabled);
  }

  @Override
  public boolean startInTransaction() {
    return true;
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  protected boolean isAvailable(final DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    if (DumbService.getInstance(project).isDumb() && !isDumbAware()) {
      return false;
    }

    final IdeView view = dataContext.getData(IdeView.KEY);
    if (view == null || view.getDirectories().length == 0) {
      return false;
    }

    return true;
  }
}
