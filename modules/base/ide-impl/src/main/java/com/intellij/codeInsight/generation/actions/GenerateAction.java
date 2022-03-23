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

package com.intellij.codeInsight.generation.actions;

import consulo.language.editor.CodeInsightBundle;
import com.intellij.openapi.actionSystem.*;
import consulo.codeEditor.Editor;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.dataContext.DataContext;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.ListPopup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GenerateAction extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    final ListPopup popup =
            JBPopupFactory.getInstance().createActionGroupPopup(
                    CodeInsightBundle.message("generate.list.popup.title"),
                    wrapGroup(getGroup(), dataContext, project),
                    dataContext,
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    false);

    popup.showInBestPositionFor(dataContext);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }

    Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor == null) {
      presentation.setEnabled(false);
      return;
    }

    boolean groupEmpty = ActionGroupUtil.isGroupEmpty(getGroup(), event);
    presentation.setEnabled(!groupEmpty);
  }

  private static DefaultActionGroup getGroup() {
    return (DefaultActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_GENERATE);
  }

  private static DefaultActionGroup wrapGroup(DefaultActionGroup actionGroup, DataContext dataContext, @Nonnull Project project) {
    final DefaultActionGroup copy = new DefaultActionGroup();
    for (final AnAction action : actionGroup.getChildren(null)) {
      if (DumbService.isDumb(project) && !action.isDumbAware()) {
        continue;
      }

      if (action instanceof GenerateActionPopupTemplateInjector) {
        final AnAction editTemplateAction = ((GenerateActionPopupTemplateInjector)action).createEditTemplateAction(dataContext);
        if (editTemplateAction != null) {
          copy.add(new GenerateWrappingGroup(action, editTemplateAction));
          continue;
        }
      }
      if (action instanceof DefaultActionGroup) {
        copy.add(wrapGroup((DefaultActionGroup)action, dataContext, project));
      }
      else {
        copy.add(action);
      }
    }
    return copy;
  }

  private static class GenerateWrappingGroup extends ActionGroup {

    private final AnAction myAction;
    private final AnAction myEditTemplateAction;

    public GenerateWrappingGroup(AnAction action, AnAction editTemplateAction) {
      myAction = action;
      myEditTemplateAction = editTemplateAction;
      copyFrom(action);
      setPopup(true);
    }

    @Override
    public boolean canBePerformed(DataContext context) {
      return true;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return new AnAction[] {myEditTemplateAction};
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      final Project project = e.getData(CommonDataKeys.PROJECT);
      assert project != null;
      final DumbService dumbService = DumbService.getInstance(project);
      try {
        dumbService.setAlternativeResolveEnabled(true);
        myAction.actionPerformed(e);
      }
      finally {
        dumbService.setAlternativeResolveEnabled(false);
      }
    }
  }
}