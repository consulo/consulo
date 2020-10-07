/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.ui.configuration.projectRoot.moduleLayerActions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public class NewLayerAction extends AnAction {
  private ModuleEditor myModuleEditor;
  private boolean myCopy;

  public NewLayerAction(ModuleEditor moduleEditor, boolean copy) {
    super(copy ? "Copy layer" : "New layer", null, copy ? PlatformIconGroup.actionsCopy() : AllIcons.General.Add);
    myModuleEditor = moduleEditor;
    myCopy = copy;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ModifiableRootModel modifiableRootModel = myModuleEditor.getModifiableRootModelProxy();
    String copyName = myCopy ? modifiableRootModel.getCurrentLayerName() : null;
    String newName = Messages.showInputDialog(modifiableRootModel.getProject(), "Name", "Enter Name", Messages.getQuestionIcon(),
                                              createUniqueSdkName(copyName, modifiableRootModel), new InputValidator() {
      @RequiredUIAccess
      @Override
      public boolean checkInput(String inputString) {
        String trimString = inputString.trim();
        return !StringUtil.isEmpty(trimString) && modifiableRootModel.findLayerByName(trimString) == null;
      }

      @RequiredUIAccess
      @Override
      public boolean canClose(String inputString) {
        return true;
      }
    });

    if (newName != null) {
      modifiableRootModel.addLayer(newName.trim(), copyName, true);
      String moduleDirUrl = modifiableRootModel.getModule().getModuleDirUrl();
      if (moduleDirUrl != null) {
        modifiableRootModel.addContentEntry(moduleDirUrl);
      }
    }
  }

  @Nonnull
  private static String createUniqueSdkName(String suggestedName, final ModifiableRootModel modifiableRootModel) {
    if (suggestedName == null) {
      suggestedName = ModifiableRootModel.DEFAULT_LAYER_NAME;
    }
    final Set<String> names = modifiableRootModel.getLayers().keySet();
    String newSdkName = suggestedName;
    int i = 0;
    while (names.contains(newSdkName)) {
      newSdkName = suggestedName + String.valueOf(++i);
    }
    return newSdkName;
  }
}
