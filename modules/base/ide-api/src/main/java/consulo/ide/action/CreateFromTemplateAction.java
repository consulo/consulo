/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.application.CommonBundle;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * @author Eugene.Kudelevsky
 */
public abstract class CreateFromTemplateAction<T extends PsiElement> extends AnAction {
  protected static final Logger LOG = Logger.getInstance(CreateFromTemplateAction.class);

  public CreateFromTemplateAction(String text, String description, Image icon) {
    super(text, description, icon);
  }

  protected CreateFromTemplateAction(@Nonnull LocalizeValue text) {
    super(text);
  }

  protected CreateFromTemplateAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  protected CreateFromTemplateAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  @Override
  @RequiredUIAccess
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    final IdeView view = e.getData(IdeView.KEY);
    if (view == null) {
      return;
    }

    final Project project = e.getData(Project.KEY);

    final PsiDirectory dir = view.getOrChooseDirectory();
    if (dir == null || project == null) return;

    final CreateFileFromTemplateDialog.Builder builder = CreateFileFromTemplateDialog.createDialog(project);
    buildDialog(project, dir, builder);

    final Ref<String> selectedTemplateName = Ref.create(null);
    builder.show(getErrorTitle(), getDefaultTemplateName(dir), new CreateFileFromTemplateDialog.FileCreator<T>() {
      @Override
      public T createFile(@Nonnull String name, @Nonnull String templateName) {
        selectedTemplateName.set(templateName);
        return CreateFromTemplateAction.this.createFile(name, templateName, dir);
      }

      @Override
      @Nonnull
      public String getActionName(@Nonnull String name, @Nonnull String templateName) {
        return CreateFromTemplateAction.this.getActionName(dir, name, templateName);
      }
    }, createdElement -> {
      view.selectElement(createdElement);
      postProcess(createdElement, selectedTemplateName.get(), builder.getCustomProperties());
    });
  }

  @RequiredUIAccess
  protected void postProcess(T createdElement, String templateName, Map<String, String> customProperties) {
  }

  @Nullable
  protected abstract T createFile(String name, String templateName, PsiDirectory dir);

  protected abstract void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder);

  @Nullable
  protected String getDefaultTemplateName(@Nonnull PsiDirectory dir) {
    String property = getDefaultTemplateProperty();
    return property == null ? null : ProjectPropertiesComponent.getInstance(dir.getProject()).getValue(property);
  }

  @Nullable
  protected Class<? extends ModuleExtension> getModuleExtensionClass() {
    return null;
  }

  @Nullable
  protected String getDefaultTemplateProperty() {
    return null;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (!e.getPresentation().isVisible()) {
      return;
    }

    e.getPresentation().setEnabledAndVisible(isAvailable(e.getDataContext()) && e.getPresentation().isEnabled());
  }

  protected boolean isAvailable(DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    final IdeView view = dataContext.getData(IdeView.KEY);
    if (project == null || view == null) {
      return false;
    }

    if (view.getDirectories().length != 0) {
      Module module = dataContext.getData(Module.KEY);
      if (module == null) {
        return false;
      }

      final Class moduleExtensionClass = getModuleExtensionClass();
      if (moduleExtensionClass != null && ModuleUtilCore.getExtension(module, moduleExtensionClass) == null) {
        return false;
      }
      
      return true;
    }
    return false;
  }

  protected abstract String getActionName(PsiDirectory directory, String newName, String templateName);

  protected String getErrorTitle() {
    return CommonBundle.getErrorTitle();
  }

  //todo append $END variable to templates?
  public static void moveCaretAfterNameIdentifier(PsiNameIdentifierOwner createdElement) {
    final Project project = createdElement.getProject();
    final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) {
      final VirtualFile virtualFile = createdElement.getContainingFile().getVirtualFile();
      if (virtualFile != null) {
        if (FileDocumentManager.getInstance().getDocument(virtualFile) == editor.getDocument()) {
          final PsiElement nameIdentifier = createdElement.getNameIdentifier();
          if (nameIdentifier != null) {
            editor.getCaretModel().moveToOffset(nameIdentifier.getTextRange().getEndOffset());
          }
        }
      }
    }
  }
}
