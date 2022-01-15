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
package com.intellij.ide.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.actions.CreateFromTemplateActionBase;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import consulo.extensions.CompositeExtensionPointName;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.apache.velocity.runtime.parser.ParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * @author Dmitry Avdeev
 */
public abstract class CreateFileFromTemplateAction extends CreateFromTemplateAction<PsiFile> {
  public static interface ModuleResolver {
    public static final CompositeExtensionPointName<ModuleResolver> EP_NAME = CompositeExtensionPointName.applicationPoint("com.intellij.createFromTemplateActionModuleResolver", ModuleResolver.class);

    @Nullable
    Module resolveModule(@Nonnull PsiDirectory directory, @Nonnull FileType fileType);
  }

  protected CreateFileFromTemplateAction(String text, String description, Image icon) {
    super(text, description, icon);
  }

  protected CreateFileFromTemplateAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  protected PsiFile createFileFromTemplate(String name, FileTemplate template, PsiDirectory dir) {
    return createFileFromTemplate(name, template, dir, getDefaultTemplateProperty(), true);
  }

  @Nullable
  public static PsiFile createFileFromTemplate(@Nullable String name, @Nonnull FileTemplate template, @Nonnull PsiDirectory dir, @Nullable String defaultTemplateProperty, boolean openFile) {
    return createFileFromTemplate(name, template, dir, defaultTemplateProperty, openFile, Collections.emptyMap());
  }

  @Nullable
  public static PsiFile createFileFromTemplate(@Nullable String name,
                                               @Nonnull FileTemplate template,
                                               @Nonnull PsiDirectory dir,
                                               @Nullable String defaultTemplateProperty,
                                               boolean openFile,
                                               @Nonnull Map<String, String> liveTemplateDefaultValues) {
    if (name != null) {
      CreateFileAction.MkDirs mkdirs = new CreateFileAction.MkDirs(name, dir);
      name = mkdirs.newName;
      dir = mkdirs.directory;
    }

    PsiElement element;
    Project project = dir.getProject();
    try {
      element = FileTemplateUtil.createFromTemplate(template, name, Collections.emptyMap(), dir);
      final PsiFile psiFile = element.getContainingFile();

      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        if (openFile) {
          if (template.isLiveTemplateEnabled()) {
            CreateFromTemplateActionBase.startLiveTemplate(psiFile, liveTemplateDefaultValues);
          }
          else {
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
          }
        }
        if (defaultTemplateProperty != null) {
          PropertiesComponent.getInstance(project).setValue(defaultTemplateProperty, template.getName());
        }
        return psiFile;
      }
    }
    catch (ParseException e) {
      throw new IncorrectOperationException("Error parsing Velocity template: " + e.getMessage(), (Throwable)e);
    }
    catch (IncorrectOperationException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e);
    }

    return null;
  }

  @Nullable
  protected FileType getFileTypeForModuleResolve() {
    return null;
  }

  @RequiredUIAccess
  @Override
  protected void postProcess(PsiFile createdElement, String templateName, Map<String, String> customProperties) {
    super.postProcess(createdElement, templateName, customProperties);

    FileType templateFileType = getFileTypeForModuleResolve();
    if(templateFileType != null) {
      PsiDirectory parent = createdElement.getParent();
      assert parent != null;
      Module module = ModuleResolver.EP_NAME.composite().resolveModule(parent, templateFileType);
      if(module != null) {
        ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();

        rootModel.addContentEntry(createdElement.getVirtualFile());

        WriteAction.run(rootModel::commit);
      }
    }
  }

  @Override
  protected PsiFile createFile(String name, String templateName, PsiDirectory dir) {
    final FileTemplate template = FileTemplateManager.getInstance(dir.getProject()).getInternalTemplate(templateName);
    return createFileFromTemplate(name, template, dir);
  }
}
