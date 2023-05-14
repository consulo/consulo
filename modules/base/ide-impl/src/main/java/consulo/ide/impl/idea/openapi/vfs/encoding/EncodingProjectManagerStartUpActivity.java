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
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27-Jun-22
 */
@ExtensionImpl
final class EncodingProjectManagerStartUpActivity implements PostStartupActivity, DumbAware {
  @Inject
  EncodingProjectManagerStartUpActivity() {
  }

  @Override
  public void runActivity(@Nonnull Project project, UIAccess uiAccess) {
    GuiUtils.invokeLaterIfNeeded(() -> ((EncodingProjectManagerImpl)EncodingProjectManager.getInstance(project)).reloadAlreadyLoadedDocuments(), IdeaModalityState.NON_MODAL, project.getDisposed());
  }
}
