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
package consulo.ide.impl.roots.ui.configuration.projectRoot.moduleLayerActions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.util.IconUtil;
import jakarta.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 30.07.14
 */
public class DeleteLayerAction extends AnAction {
  private ModuleEditor myModuleEditor;

  public DeleteLayerAction(ModuleEditor moduleEditor) {
    super("Delete layer", null, IconUtil.getRemoveIcon());
    myModuleEditor = moduleEditor;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ModifiableRootModel modifiableRootModelProxy = myModuleEditor.getModifiableRootModelProxy();

    String currentLayerName = modifiableRootModelProxy.getCurrentLayerName();

    modifiableRootModelProxy.removeLayer(currentLayerName, true);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    ModifiableRootModel modifiableRootModelProxy = myModuleEditor.getModifiableRootModelProxy();
    e.getPresentation().setEnabled(modifiableRootModelProxy != null && modifiableRootModelProxy.getLayers().size() > 1);
  }
}
