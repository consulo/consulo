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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.project.Project;
import consulo.ui.ex.UIBundle;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;

@Deprecated
@DeprecationInfo("Use Alert/Alerts class from ui-api")
@SuppressWarnings("ALL")
public class MessagesEx extends Messages {

  public static MessageInfo fileIsReadOnly(Project project, String filePath) {
    return error(project, UIBundle.message("file.is.read.only.message.text", filePath));
  }

  public static MessageInfo filesAreReadOnly(Project project, String[] files) {
    if (files.length == 1){
      return fileIsReadOnly(project, files[0]);
    } else {
      return error(project, UIBundle.message("files.are.read.only.message.text", filePaths(files)));
    }
  }

  private static String filePaths(String[] files) {
    return StringUtil.join(files, ",\n");
  }

  public static MessageInfo fileIsReadOnly(Project project, VirtualFile file) {
    return fileIsReadOnly(project, file.getPresentableUrl());
  }

  public static MessageInfo error(Project project, String message) {
    return error(project, message, UIBundle.message("error.dialog.title"));
  }

  public static MessageInfo error(Project project, String message, String title) {
    return new MessageInfo(project, message, title);
  }

  public static abstract class BaseDialogInfo<ThisClass extends BaseDialogInfo> {
    private Project myProject;
    private String myMessage;
    private String myTitle;
    private Image myIcon;
    private String[] myOptions = new String[]{CommonBundle.getOkButtonText()};
    private int myDefaultOption = 0;

    protected BaseDialogInfo(Project project) {
      myProject = project;
    }

    public BaseDialogInfo(Project project, String message, String title, Image icon) {
      this(project);
      myMessage = message;
      myTitle = title;
      myIcon = icon;
    }

    public ThisClass setTitle(String title) { myTitle = title; return getThis(); }

    public String getMessage() { return myMessage; }

    public ThisClass appendMessage(String message) {
      myMessage += message;
      return getThis();
    }

    public void setOptions(String[] options, int defaultOption) {
      myOptions = options;
      myDefaultOption = defaultOption;
    }

    protected abstract ThisClass getThis();

    public ThisClass setIcon(Image icon) { myIcon = icon; return getThis(); }

    public void setMessage(String message) {
      myMessage = message;
    }

    public Project getProject() {
      return myProject;
    }

    public String getTitle() {
      return myTitle;
    }

    public String[] getOptions() {
      return myOptions;
    }

    public int getDefaultOption() {
      return myDefaultOption;
    }

    public Image getIcon() {
      return myIcon;
    }
  }

  public static class MessageInfo extends BaseDialogInfo<MessageInfo> {
    public MessageInfo(Project project, String message, String title) {
      super(project, message, title, getErrorIcon());
    }

    public int showNow() {
      return showDialog(getProject(), getMessage(), getTitle(), getOptions(), getDefaultOption(), getIcon());
    }

    public void showLater() {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
          public void run() {
            if (ApplicationManager.getApplication().isDisposed()) return;
            showNow();
          }
        });
    }

    public int askYesNo() {
      setIcon(getQuestionIcon());
      return showYesNoDialog(getProject(), getMessage(), getTitle(), getIcon());
    }

    public int ask(String[] options, int defaultOptionIndex) {
      setOptions(options, defaultOptionIndex);
      return showNow();
    }

    protected MessageInfo getThis() {
      return this;
    }
  }

  public static class ChoiceInfo extends BaseInputInfo<ChoiceInfo> {
    private String[] myChoises = ArrayUtil.EMPTY_STRING_ARRAY;
    private String myDefaultChoice = null;

    public ChoiceInfo(Project project) {
      super(project);
      setIcon(getQuestionIcon());
      setOptions(new String[]{CommonBundle.getOkButtonText()}, 0);
    }

    public ChoiceInfo getThis() {
      return this;
    }

    public ChoiceInfo setChoices(String[] choices, String defaultChoice) {
      myChoises = choices;
      myDefaultChoice = defaultChoice;
      return getThis();
    }

    public UserInput askUser() {
      ChooseDialog dialog = new ChooseDialog(getProject(), getMessage(), getTitle(), getIcon(), myChoises, myDefaultChoice, getOptions(), getDefaultOption());
      dialog.setValidator(null);
      JComboBox comboBox = dialog.getComboBox();
      comboBox.setEditable(false);
      comboBox.setSelectedItem(myDefaultChoice);
      dialog.show();
      Object selectedItem = comboBox.getSelectedItem();
      return new UserInput(selectedItem != null ? selectedItem.toString() : null, dialog.getExitCode());
    }
  }

  public static class UserInput {
    private final int mySelectedOption;
    private final String myInput;

    public UserInput(String choice, int option) {
      mySelectedOption = option;
      myInput = choice;
    }

    public String getInput() {
      return myInput;
    }

    public int getSelectedOption() {
      return mySelectedOption;
    }
  }

  public static class InputInfo extends BaseInputInfo<InputInfo> {
    private String myDefaultValue;

    public InputInfo(Project project) {
      super(project);
      setOptions(new String[]{CommonBundle.getOkButtonText(), CommonBundle.getCancelButtonText()}, 0);
    }

    public UserInput askUser() {
      InputDialog dialog = new InputDialog(getProject(), getMessage(), getTitle(), getIcon(), myDefaultValue, null, getOptions(), getDefaultOption());
      dialog.show();
      return new UserInput(dialog.getTextField().getText(), dialog.getExitCode());
    }

    public InputInfo getThis() {
      return this;
    }

    public void setDefaultValue(String defaultValue) {
      myDefaultValue = defaultValue;
    }
  }

  public static abstract class BaseInputInfo<ThisClass extends BaseInputInfo> extends BaseDialogInfo<ThisClass> {


    public BaseInputInfo(Project project) {
      super(project);
    }

    public String forceUserInput() {
      setOptions(new String[]{CommonBundle.getOkButtonText()}, 0);
      return askUser().getInput();
    }

    public abstract UserInput askUser();
  }
}
