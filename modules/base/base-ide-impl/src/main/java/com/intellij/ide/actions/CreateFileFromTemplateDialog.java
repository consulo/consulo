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

package com.intellij.ide.actions;

import com.intellij.ide.actions.newclass.CreateWithTemplatesDialogPanel;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposer;
import consulo.ui.ValidableComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author peter
 */
public class CreateFileFromTemplateDialog  {
  @Nonnull
  public static Builder createDialog(@Nonnull final Project project) {
    return new NonBlockingPopupBuilderImpl(project);
  }

  public static class NonBlockingPopupBuilderImpl implements Builder {
    @Nonnull
    private final Project myProject;

    private String myTitle = "Title";
    private final List<Trinity<String, Image, String>> myTemplatesList = new ArrayList<>();
    private InputValidator myInputValidator;
    private ValidableComponent.Validator<String> myValidator;

    private NonBlockingPopupBuilderImpl(@Nonnull Project project) {
      myProject = project;
    }

    @Override
    public Builder setTitle(String title) {
      myTitle = title;
      return this;
    }

    @Override
    public Builder addKind(@Nonnull String kind, @Nullable Image icon, @Nonnull String templateName) {
      myTemplatesList.add(Trinity.create(kind, icon, templateName));
      return this;
    }

    @Override
    public Builder setValidator(@Nonnull ValidableComponent.Validator<String> validator) {
      myValidator = validator;
      return this;
    }

    @Override
    public Builder setValidator(InputValidator validator) {
      myInputValidator = validator;
      return this;
    }

    @RequiredUIAccess
    @Override
    public <T extends PsiElement> void show(@Nonnull String errorTitle, @Nullable String selectedItem, @Nonnull FileCreator<T> fileCreator, Consumer<T> elementConsumer) {
      CreateWithTemplatesDialogPanel contentPanel = new CreateWithTemplatesDialogPanel(myTemplatesList, selectedItem);
      ElementCreator elementCreator = new ElementCreator(myProject, errorTitle) {

        @Override
        protected PsiElement[] create(@Nonnull String newName) {
          T element = fileCreator.createFile(contentPanel.getEnteredName(), contentPanel.getSelectedTemplate());
          return element != null ? new PsiElement[]{element} : PsiElement.EMPTY_ARRAY;
        }

        @Override
        protected String getActionName(String newName) {
          return fileCreator.getActionName(newName, contentPanel.getSelectedTemplate());
        }
      };

      JBPopup popup = NewItemPopupUtil.createNewItemPopup(myTitle, contentPanel, (JComponent)TargetAWT.to(contentPanel.getNameField()));
      if (myValidator != null) {
        contentPanel.addValidator(myValidator);
      }

      contentPanel.addValidator(value -> {
        if (myInputValidator != null && !myInputValidator.canClose(value)) {
          String message = InputValidatorEx.getErrorText(myInputValidator, value, LangBundle.message("incorrect.name"));
          return new ValidableComponent.ValidationInfo(message);
        }
        return null;
      });

      contentPanel.setApplyAction(e -> {
        String newElementName = contentPanel.getEnteredName();
        if (StringUtil.isEmptyOrSpaces(newElementName)) return;

        popup.closeOk(e);
        T createdElement = (T)createElement(newElementName, elementCreator);
        if (createdElement != null) {
          elementConsumer.accept(createdElement);
        }
      });

      Disposer.register(popup, contentPanel);
      popup.showCenteredInCurrentWindow(myProject);
    }

    @Nullable
    private static PsiElement createElement(String newElementName, ElementCreator creator) {
      PsiElement[] elements = creator.tryCreate(newElementName);
      return elements.length > 0 ? elements[0] : null;
    }
  }

  public interface Builder {
    Builder setTitle(String title);

    Builder setValidator(InputValidator validator);

    Builder setValidator(@Nonnull ValidableComponent.Validator<String> validator);

    Builder addKind(@Nonnull String kind, @Nullable Image icon, @Nonnull String templateName);

    @RequiredUIAccess
    <T extends PsiElement> void show(@Nonnull String errorTitle, @Nullable String selectedItem, @Nonnull FileCreator<T> creator, @RequiredUIAccess @Nonnull Consumer<T> consumer);

    @Nullable
    default Map<String, String> getCustomProperties() {
      return null;
    }
  }

  public interface FileCreator<T> {
    @Nullable
    T createFile(@Nonnull String name, @Nonnull String templateName);

    @Nonnull
    String getActionName(@Nonnull String name, @Nonnull String templateName);
  }
}
