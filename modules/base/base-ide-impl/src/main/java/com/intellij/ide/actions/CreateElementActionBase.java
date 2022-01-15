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

package com.intellij.ide.actions;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * The base class for actions which create new file elements.
 *
 * @since 5.1
 */
public abstract class CreateElementActionBase extends CreateInDirectoryActionBase {

  protected CreateElementActionBase() {
  }

  protected CreateElementActionBase(@Nonnull LocalizeValue text) {
    super(text);
  }

  protected CreateElementActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  protected CreateElementActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  protected CreateElementActionBase(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  protected abstract void invokeDialog(Project project, PsiDirectory directory, @Nonnull Consumer<PsiElement[]> elementsConsumer);

  /**
   * @return created elements. Never null.
   */
  @Nonnull
  @RequiredUIAccess
  protected abstract PsiElement[] create(String newName, PsiDirectory directory) throws Exception;

  protected abstract String getErrorTitle();

  protected abstract String getCommandName();

  protected abstract String getActionName(PsiDirectory directory, String newName);

  @RequiredUIAccess
  @Override
  public final void actionPerformed(@Nonnull final AnActionEvent e) {
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    if (view == null) {
      return;
    }

    final Project project = e.getProject();

    final PsiDirectory dir = view.getOrChooseDirectory();
    if (dir == null) return;

    invokeDialog(project, dir, elements -> {

      for (PsiElement createdElement : elements) {
        view.selectElement(createdElement);
      }
    });
  }

  public static String filterMessage(String message) {
    if (message == null) return null;
    @NonNls final String ioExceptionPrefix = "java.io.IOException:";
    message = StringUtil.trimStart(message, ioExceptionPrefix);
    return message;
  }

  protected class MyInputValidator extends ElementCreator implements InputValidator {
    private final PsiDirectory myDirectory;
    private PsiElement[] myCreatedElements = PsiElement.EMPTY_ARRAY;

    public MyInputValidator(final Project project, final PsiDirectory directory) {
      super(project, getErrorTitle());
      myDirectory = directory;
    }

    public PsiDirectory getDirectory() {
      return myDirectory;
    }

    @RequiredUIAccess
    @Override
    public boolean checkInput(final String inputString) {
      return true;
    }

    @Override
    public PsiElement[] create(String newName) throws Exception {
      return CreateElementActionBase.this.create(newName, myDirectory);
    }

    @Override
    public String getActionName(String newName) {
      return CreateElementActionBase.this.getActionName(myDirectory, newName);
    }

    @RequiredUIAccess
    @Override
    public boolean canClose(final String inputString) {
      myCreatedElements = tryCreate(inputString);
      return myCreatedElements.length > 0;
    }

    public final PsiElement[] getCreatedElements() {
      return myCreatedElements;
    }
  }
}
