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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.plugins.whatsNew.WhatsNewVirtualFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
@ActionImpl(id = "WhatsNewAction", parents = {
        @ActionParentRef(value = @ActionRef(id = "HelpMenu"), anchor = ActionRefAnchor.AFTER, relatedToAction = @ActionRef(type = OnlineDocAction.class))
})
public class WhatsNewAction extends AnAction implements DumbAware {
  private final Application myApplication;

  @Inject
  public WhatsNewAction(Application application) {
    myApplication = application;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }

    LocalizeValue text = IdeLocalize.whatsnewActionCustomText(myApplication.getName());

    FileEditorManager.getInstance(project).openFile(new WhatsNewVirtualFile(text), true);
  }

  @RequiredUIAccess
  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setVisible(e.getData(Project.KEY) != null);
    e.getPresentation().setTextValue(IdeLocalize.whatsnewActionCustomText(myApplication.getName()));
    e.getPresentation().setDescriptionValue(IdeLocalize.whatsnewActionCustomDescription(myApplication.getName()));
  }
}
