/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.fileTemplates.actions;

import consulo.ide.impl.idea.ide.IdeView;
import consulo.ide.impl.idea.ide.fileTemplates.ui.CreateFromTemplateDialog;
import consulo.ide.impl.idea.ide.util.DirectoryChooserUtil;
import consulo.dataContext.DataContext;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.language.editor.impl.fileTemplate.EditorFileTemplateUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static consulo.ide.impl.idea.util.ObjectUtils.notNull;

public abstract class CreateFromTemplateActionBase extends AnAction {
  public CreateFromTemplateActionBase(final String title, final String description, final Image icon) {
    super(title, description, icon);
  }

  @RequiredUIAccess
  @Override
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    IdeView view = dataContext.getData(IdeView.KEY);
    if (view == null) return;
    PsiDirectory dir = getTargetDirectory(dataContext, view);
    if (dir == null) return;
    Project project = dir.getProject();

    FileTemplate selectedTemplate = getTemplate(project, dir);
    if (selectedTemplate != null) {
      AnAction action = getReplacedAction(selectedTemplate);
      if (action != null) {
        action.actionPerformed(e);
      }
      else {
        FileTemplateManager.getInstance(project).addRecentName(selectedTemplate.getName());
        AttributesDefaults defaults = getAttributesDefaults(dataContext);
        Map<String, Object> properties = defaults != null ? defaults.getDefaultProperties() : null;
        CreateFromTemplateDialog dialog = new CreateFromTemplateDialog(dir, selectedTemplate, defaults, properties);
        PsiElement createdElement = dialog.create();
        if (createdElement != null) {
          elementCreated(dialog, createdElement);
          view.selectElement(createdElement);
          if (selectedTemplate.isLiveTemplateEnabled() && createdElement instanceof PsiFile) {
            Map<String, String> defaultValues = getLiveTemplateDefaults(dataContext, ((PsiFile)createdElement));
            startLiveTemplate((PsiFile)createdElement, notNull(defaultValues, Collections.emptyMap()));
          }
        }
      }
    }
  }

  public static void startLiveTemplate(@Nonnull PsiFile file) {
    EditorFileTemplateUtil.startLiveTemplate(file, Map.of());
  }

  public static void startLiveTemplate(@Nonnull PsiFile file, @Nonnull Map<String, String> defaultValues) {
    EditorFileTemplateUtil.startLiveTemplate(file, defaultValues);
  }

  @Nullable
  protected PsiDirectory getTargetDirectory(DataContext dataContext, IdeView view) {
    return DirectoryChooserUtil.getOrChooseDirectory(view);
  }

  protected abstract FileTemplate getTemplate(Project project, PsiDirectory dir);

  @Nullable
  protected AnAction getReplacedAction(FileTemplate selectedTemplate) {
    return null;
  }

  @Nullable
  protected AttributesDefaults getAttributesDefaults(DataContext dataContext) {
    return null;
  }

  protected void elementCreated(CreateFromTemplateDialog dialog, PsiElement createdElement) {
  }

  @Nullable
  protected Map<String, String> getLiveTemplateDefaults(DataContext dataContext, @Nonnull PsiFile file) {
    return null;
  }
}