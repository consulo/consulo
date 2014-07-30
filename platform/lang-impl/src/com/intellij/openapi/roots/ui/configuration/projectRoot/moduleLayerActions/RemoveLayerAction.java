/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.roots.ui.configuration.projectRoot.moduleLayerActions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public class RemoveLayerAction extends AnAction {
  private ModuleEditor myModuleEditor;

  public RemoveLayerAction(ModuleEditor moduleEditor) {
    myModuleEditor = moduleEditor;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    ModifiableRootModel modifiableRootModelProxy = myModuleEditor.getModifiableRootModelProxy();

    String currentLayerName = modifiableRootModelProxy.getCurrentLayerName();

    modifiableRootModelProxy.removeLayer(currentLayerName, true);
  }
}
