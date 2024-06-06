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
package consulo.ide.impl.configurationStore.vcs;

import consulo.annotation.component.ExtensionImpl;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.project.impl.internal.store.IProjectStore;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 06-Nov-16
 */
@ExtensionImpl
public class StoreIgnoredFileProvider implements IgnoredFileProvider {
  @Override
  public boolean isIgnoredFile(@Nonnull Project project, @Nonnull FilePath filePath) {
    IProjectStore stateStore = project.getInstance(IProjectStore.class);
    return Comparing.equal(filePath.getVirtualFile(), stateStore.getWorkspaceFile());
  }
}
