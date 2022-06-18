/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.wm.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CallChain;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18-Jun-22
 */
@ExtensionImpl
public class InitToolWindowsActivity implements PostStartupActivity, DumbAware {
  @Override
  public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    UIAccess.assetIsNotUIThread();

    ToolWindowManagerBase manager = (ToolWindowManagerBase)ToolWindowManager.getInstance(project);

    CallChain.Link<Void, Void> chain = CallChain.first(uiAccess);
    chain = chain.linkUI(manager::initializeEditorComponent);
    chain = chain.linkAsync(() -> manager.registerToolWindowsFromBeans(uiAccess));
    chain = chain.linkUI(manager::postInitialize);
    chain = chain.linkUI(manager::connectModuleExtensionListener);

    // toss it, and wait result
    chain.tossAsync().getResultSync();
  }
}
