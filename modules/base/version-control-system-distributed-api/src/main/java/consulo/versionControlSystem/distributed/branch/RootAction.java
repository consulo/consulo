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
package consulo.versionControlSystem.distributed.branch;

import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The element of the branch popup which allows to show branches of the selected repository.
 * It is available only in projects with multiple roots.
 *
 * @author Kirill Likhodedov
 * @author Nadya Zabrodina
 */
public class RootAction<T extends Repository> extends ActionGroup {

  @Nonnull
  protected final T myRepository;
  @Nonnull
  private final ActionGroup myGroup;
  @Nonnull
  private final String myBranchText;

  /**
   * @param currentRepository Pass null in the case of common repositories - none repository will be highlighted then.
   * @param actionsGroup
   * @param branchText
   */
  public RootAction(@Nonnull T repository, @Nullable T currentRepository, @Nonnull ActionGroup actionsGroup, @Nonnull String branchText) {
    super("", true);
    myRepository = repository;
    myGroup = actionsGroup;
    myBranchText = branchText;
    if (repository.equals(currentRepository)) {
      getTemplatePresentation().setIcon(AllIcons.Actions.Checked);
    }
    getTemplatePresentation().setText(DvcsUtil.getShortRepositoryName(repository), false);
  }

  @Nonnull
  public String getCaption() {
    return "Current branch in " + DvcsUtil.getShortRepositoryName(myRepository) + ": " + getDisplayableBranchText();
  }

  @Nonnull
  public String getDisplayableBranchText() {
    return myBranchText;
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return myGroup.getChildren(e);
  }
}




