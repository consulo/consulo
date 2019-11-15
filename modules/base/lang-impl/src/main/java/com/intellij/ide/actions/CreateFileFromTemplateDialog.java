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

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.newclass.CreateWithTemplatesDialogPanel;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import consulo.awt.TargetAWT;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ValidableComponent;
import consulo.ui.image.Image;
import consulo.ui.migration.SwingImageRef;

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
public class CreateFileFromTemplateDialog extends DialogWrapper {
  private JTextField myNameField;
  private TemplateKindCombo myKindCombo;
  private JPanel myPanel;
  private JLabel myUpDownHint;
  private JLabel myKindLabel;
  private JLabel myNameLabel;

  private ElementCreator myCreator;
  @Nullable
  private InputValidator myInputValidator;

  protected CreateFileFromTemplateDialog(@Nonnull Project project) {
    super(project, true);

    myKindLabel.setLabelFor(myKindCombo);
    myKindCombo.registerUpDownHint(myNameField);
    myUpDownHint.setIcon(AllIcons.Ide.UpDown);
    setTemplateKindComponentsVisible(false);
    init();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (myInputValidator != null) {
      final String text = myNameField.getText().trim();
      final boolean canClose = myInputValidator.canClose(text);
      if (!canClose) {
        String errorText = LangBundle.message("incorrect.name");
        if (myInputValidator instanceof InputValidatorEx) {
          String message = ((InputValidatorEx)myInputValidator).getErrorText(text);
          if (message != null) {
            errorText = message;
          }
        }
        return new ValidationInfo(errorText, myNameField);
      }
    }
    return super.doValidate();
  }

  protected JTextField getNameField() {
    return myNameField;
  }

  protected TemplateKindCombo getKindCombo() {
    return myKindCombo;
  }

  protected JLabel getKindLabel() {
    return myKindLabel;
  }

  protected JLabel getNameLabel() {
    return myNameLabel;
  }

  private String getEnteredName() {
    final JTextField nameField = getNameField();
    final String text = nameField.getText().trim();
    nameField.setText(text);
    return text;
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Override
  protected void doOKAction() {
    if (myCreator != null && myCreator.tryCreate(getEnteredName()).length == 0) {
      return;
    }
    super.doOKAction();
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return getNameField();
  }

  public void setTemplateKindComponentsVisible(boolean flag) {
    myKindCombo.setVisible(flag);
    myKindLabel.setVisible(flag);
    myUpDownHint.setVisible(flag);
  }

  public static Builder createDialog(@Nonnull final Project project) {
    return new NonBlockingPopupBuilderImpl(project);
  }

  private static class BuilderImpl implements Builder {
    private final CreateFileFromTemplateDialog myDialog;
    private final Project myProject;

    public BuilderImpl(CreateFileFromTemplateDialog dialog, Project project) {
      myDialog = dialog;
      myProject = project;
    }

    @Override
    public Builder setTitle(String title) {
      myDialog.setTitle(title);
      return this;
    }

    @Override
    public Builder addKind(@Nonnull String kind, @Nullable Image icon, @Nonnull String templateName) {
      myDialog.getKindCombo().addItem(kind, icon, templateName);
      if (myDialog.getKindCombo().getComboBox().getItemCount() > 1) {
        myDialog.setTemplateKindComponentsVisible(true);
      }
      return this;
    }

    @Override
    public Builder setValidator(@Nonnull InputValidator validator) {
      myDialog.myInputValidator = validator;
      return this;
    }

    @Override
    public Builder setValidator(@Nonnull ValidableComponent.Validator<String> validator) {
      if (myDialog.myInputValidator != null) {
        throw new IllegalArgumentException("already set");
      }
      myDialog.myInputValidator = new InputValidatorEx() {
        @Nullable
        @Override
        public String getErrorText(String inputString) {
          ValidableComponent.ValidationInfo validationInfo = validator.validateValue(inputString);
          return validationInfo == null ? null : validationInfo.getMessage();
        }

        @RequiredUIAccess
        @Override
        public boolean checkInput(String inputString) {
          return getErrorText(inputString) == null;
        }
      };
      return this;
    }

    @RequiredUIAccess
    @Override
    public <T extends PsiElement> void show(@Nonnull String errorTitle, @Nullable String selectedTemplateName, @Nonnull final FileCreator<T> creator, @Nonnull Consumer<T> consumer) {
      final Ref<T> created = Ref.create(null);
      myDialog.getKindCombo().setSelectedName(selectedTemplateName);
      myDialog.myCreator = new ElementCreator(myProject, errorTitle) {

        @Override
        protected PsiElement[] create(String newName) throws Exception {
          final T element = creator.createFile(myDialog.getEnteredName(), myDialog.getKindCombo().getSelectedName());
          created.set(element);
          if (element != null) {
            return new PsiElement[]{element};
          }
          return PsiElement.EMPTY_ARRAY;
        }

        @Override
        protected String getActionName(String newName) {
          return creator.getActionName(newName, myDialog.getKindCombo().getSelectedName());
        }
      };

      myDialog.showAsync().doWhenDone(() -> consumer.accept(created.get()));
    }
  }

  private static class NonBlockingPopupBuilderImpl implements Builder {
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

    @Deprecated
    default Builder addKind(@Nonnull String kind, @Nullable Icon icon, @Nonnull String templateName) {
      return addKind(kind, TargetAWT.from(icon), templateName);
    }

    default Builder addKind(@Nonnull String kind, @Nullable SwingImageRef icon, @Nonnull String templateName) {
      return addKind(kind, (Image)icon, templateName);
    }

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
