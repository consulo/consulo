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

package consulo.ide.action;

import consulo.disposer.Disposer;
import consulo.ide.action.ui.CreateWithTemplatesDialogPanel;
import consulo.ide.action.ui.NewItemPopupUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.LangBundle;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.HasValidator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
  public static Builder createDialog(@Nonnull Project project) {
    return new NonBlockingPopupBuilderImpl(project);
  }

  public static class NonBlockingPopupBuilderImpl implements Builder {
    @Nonnull
    private final Project myProject;

    private LocalizeValue myTitle = IdeLocalize.titleNewFile();
    private final List<Trinity<LocalizeValue, Image, String>> myTemplatesList = new ArrayList<>();
    private InputValidator myInputValidator;
    private HasValidator.Validator<String> myValidator;

    private NonBlockingPopupBuilderImpl(@Nonnull Project project) {
      myProject = project;
    }

    @Override
    public Builder setTitle(@Nonnull LocalizeValue title) {
      myTitle = title;
      return this;
    }

    @Override
    public Builder addKind(@Nonnull LocalizeValue kind, @Nullable Image icon, @Nonnull String templateName) {
      myTemplatesList.add(Trinity.create(kind, icon, templateName));
      return this;
    }

    @Override
    public Builder setValidator(@Nonnull HasValidator.Validator<String> validator) {
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
    public <T extends PsiElement> void show(@Nonnull LocalizeValue errorTitle,
                                            @Nullable String selectedItem,
                                            @Nonnull FileCreator<T> fileCreator,
                                            @Nonnull Consumer<T> elementConsumer) {
      CreateWithTemplatesDialogPanel contentPanel = new CreateWithTemplatesDialogPanel(myTemplatesList, selectedItem);
      ElementCreator elementCreator = new ElementCreator(myProject, errorTitle) {

        @Override
        protected PsiElement[] create(@Nonnull String newName) {
          T element = fileCreator.createFile(contentPanel.getEnteredName(), contentPanel.getSelectedTemplate());
          return element != null ? new PsiElement[]{element} : PsiElement.EMPTY_ARRAY;
        }

        @Override
        protected LocalizeValue getActionName(String newName) {
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
          return new HasValidator.ValidationInfo(message);
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
    Builder setTitle(@Nonnull LocalizeValue title);

    Builder setValidator(InputValidator validator);

    Builder setValidator(@Nonnull HasValidator.Validator<String> validator);

    Builder addKind(@Nonnull LocalizeValue kind, @Nullable Image icon, @Nonnull String templateName);

    @RequiredUIAccess
    <T extends PsiElement> void show(@Nonnull LocalizeValue errorTitle, @Nullable String selectedItem, @Nonnull FileCreator<T> creator, @RequiredUIAccess @Nonnull Consumer<T> consumer);

    @Nullable
    default Map<String, String> getCustomProperties() {
      return null;
    }
  }

  public interface FileCreator<T> {
    @Nullable
    T createFile(@Nonnull String name, @Nonnull String templateName);

    @Nonnull
    LocalizeValue getActionName(@Nonnull String name, @Nonnull String templateName);
  }
}
