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

package consulo.language.editor.impl.internal.generation;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.generation.GenerateActionPopupTemplateInjector;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionGroupUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "Generate")
public class GenerateAction extends DumbAwareAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    DataContext dataContext = e.getDataContext();

    Project project = e.getRequiredData(Project.KEY);
    final ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(CodeInsightBundle.message("generate.list.popup.title"), wrapGroup(getGroup(), dataContext, project), dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                    false);

    popup.showInBestPositionFor(dataContext);
  }

  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      presentation.setEnabledAndVisible(isEnabled(event) && event.hasData(Editor.KEY));
    }
    else {
      presentation.setEnabled(isEnabled(event));
    }
  }

  private static boolean isEnabled(@Nonnull AnActionEvent event) {
    Project project = event.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    Editor editor = event.getData(Editor.KEY);
    return editor != null && !ActionGroupUtil.isGroupEmpty(getGroup(), event);
  }

  private static DefaultActionGroup getGroup() {
    return (DefaultActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_GENERATE);
  }

  private static ActionGroup wrapGroup(DefaultActionGroup actionGroup, DataContext dataContext, @Nonnull Project project) {
    final ActionGroup.Builder copy = ActionGroup.newImmutableBuilder();
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
    return copy.build();
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
    public boolean canBePerformed(@Nonnull DataContext context) {
      return true;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return new AnAction[]{myEditTemplateAction};
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
      final Project project = e.getRequiredData(Project.KEY);
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