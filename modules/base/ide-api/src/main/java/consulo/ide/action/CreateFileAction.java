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

package consulo.ide.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.application.dumb.DumbAware;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ide.IdeBundle;
import consulo.ide.action.ui.NewItemPopupUtil;
import consulo.ide.action.ui.NewItemSimplePopupPanel;
import consulo.language.LangBundle;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.TextBox;
import consulo.ui.ValidableComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;

public class CreateFileAction extends CreateElementActionBase implements DumbAware {

  public CreateFileAction() {
    super(IdeLocalize.actionCreateNewFile(), IdeLocalize.actionCreateNewFileDescription(), AllIcons.FileTypes.Text);
  }

  public CreateFileAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
    super(text, description, icon);
  }

  public CreateFileAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
    super(text, description);
  }

  public CreateFileAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
    super(text, description, icon);
  }

  @Override
  public boolean isDumbAware() {
    return CreateFileAction.class.equals(getClass());
  }

  @Override
  @Nonnull
  protected void invokeDialog(final Project project, PsiDirectory directory, @Nonnull Consumer<PsiElement[]> elementsConsumer) {
    MyInputValidator validator = new MyValidator(project, directory);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      try {
        elementsConsumer.accept(validator.create("test"));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      createLightWeightPopup(validator, elementsConsumer).showCenteredInCurrentWindow(project);
    }
  }

  private JBPopup createLightWeightPopup(MyInputValidator validator, Consumer<PsiElement[]> consumer) {
    NewItemSimplePopupPanel contentPanel = new NewItemSimplePopupPanel();
    TextBox nameField = contentPanel.getTextField();
    JBPopup popup = NewItemPopupUtil.createNewItemPopup(IdeBundle.message("title.new.file"), contentPanel, (JComponent)TargetAWT.to(nameField));
    contentPanel.addValidator(value -> {
      if (!validator.checkInput(value)) {
        String message = InputValidatorEx.getErrorText(validator, value, LangBundle.message("incorrect.name"));
        return new ValidableComponent.ValidationInfo(message);
      }

      return null;
    });

    contentPanel.setApplyAction(event -> {
      validator.canClose(nameField.getValue());

      popup.closeOk(event);
      consumer.accept(validator.getCreatedElements());
    });

    return popup;
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  protected PsiElement[] create(String newName, PsiDirectory directory) throws Exception {
    MkDirs mkdirs = new MkDirs(newName, directory);
    return new PsiElement[]{WriteAction.compute(() -> mkdirs.directory.createFile(getFileName(mkdirs.newName)))};
  }

  public static PsiDirectory findOrCreateSubdirectory(@Nonnull PsiDirectory parent, @Nonnull String subdirName) {
    final PsiDirectory sub = parent.findSubdirectory(subdirName);
    return sub == null ? WriteAction.compute(() -> parent.createSubdirectory(subdirName)) : sub;
  }

  public static class MkDirs {
    public final String newName;
    public final PsiDirectory directory;

    @RequiredReadAction
    public MkDirs(@Nonnull String newName, @Nonnull PsiDirectory directory) {
      if (Platform.current().os().isWindows()) {
        newName = newName.replace('\\', '/');
      }
      if (newName.contains("/")) {
        final List<String> subDirs = StringUtil.split(newName, "/");
        newName = subDirs.remove(subDirs.size() - 1);
        boolean firstToken = true;
        for (String dir : subDirs) {
          if (firstToken && "~".equals(dir)) {
            final VirtualFile userHomeDir = VirtualFileUtil.getUserHomeDir();
            if (userHomeDir == null) throw new IncorrectOperationException("User home directory not found");
            final PsiDirectory directory1 = directory.getManager().findDirectory(userHomeDir);
            if (directory1 == null) throw new IncorrectOperationException("User home directory not found");
            directory = directory1;
          }
          else if ("..".equals(dir)) {
            final PsiDirectory parentDirectory = directory.getParentDirectory();
            if (parentDirectory == null) throw new IncorrectOperationException("Not a valid directory");
            directory = parentDirectory;
          }
          else if (!".".equals(dir)) {
            directory = findOrCreateSubdirectory(directory, dir);
          }
          firstToken = false;
        }
      }

      this.newName = newName;
      this.directory = directory;
    }
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName) {
    return IdeBundle.message("progress.creating.file", directory.getVirtualFile().getPresentableUrl(), File.separator, newName);
  }

  @Override
  protected String getErrorTitle() {
    return IdeBundle.message("title.cannot.create.file");
  }

  @Override
  protected String getCommandName() {
    return IdeBundle.message("command.create.file");
  }

  protected String getFileName(String newName) {
    if (getDefaultExtension() == null || FileUtil.getExtension(newName).length() > 0) {
      return newName;
    }
    return newName + "." + getDefaultExtension();
  }

  @Nullable
  protected String getDefaultExtension() {
    return null;
  }

  protected class MyValidator extends MyInputValidator implements InputValidatorEx {
    private String myErrorText;

    public MyValidator(Project project, PsiDirectory directory) {
      super(project, directory);
    }

    @RequiredUIAccess
    @Override
    public boolean checkInput(String inputString) {
      final StringTokenizer tokenizer = new StringTokenizer(inputString, "\\/");
      VirtualFile vFile = getDirectory().getVirtualFile();
      boolean firstToken = true;
      while (tokenizer.hasMoreTokens()) {
        final String token = tokenizer.nextToken();
        if ((token.equals(".") || token.equals("..")) && !tokenizer.hasMoreTokens()) {
          myErrorText = "Can't create file with name '" + token + "'";
          return false;
        }
        if (vFile != null) {
          if (firstToken && "~".equals(token)) {
            final VirtualFile userHomeDir = VirtualFileUtil.getUserHomeDir();
            if (userHomeDir == null) {
              myErrorText = "User home directory not found";
              return false;
            }
            vFile = userHomeDir;
          }
          else if ("..".equals(token)) {
            vFile = vFile.getParent();
            if (vFile == null) {
              myErrorText = "Not a valid directory";
              return false;
            }
          }
          else if (!".".equals(token)) {
            final VirtualFile child = vFile.findChild(token);
            if (child != null) {
              if (!child.isDirectory()) {
                myErrorText = "A file with name '" + token + "' already exists";
                return false;
              }
              else if (!tokenizer.hasMoreTokens()) {
                myErrorText = "A directory with name '" + token + "' already exists";
                return false;
              }
            }
            vFile = child;
          }
        }
        if (FileTypeManager.getInstance().isFileIgnored(getFileName(token))) {
          myErrorText = "'" + token + "' is an ignored name (Settings | Editor | File Types | Ignore files and folders)";
          return true;
        }
        firstToken = false;
      }
      myErrorText = null;
      return true;
    }

    @Override
    public String getErrorText(String inputString) {
      return myErrorText;
    }

    @Override
    public PsiElement[] create(String newName) throws Exception {
      UsageTrigger.trigger("CreateFile." + CreateFileAction.this.getClass().getSimpleName());
      return super.create(newName);
    }
  }
}
