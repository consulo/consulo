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
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.vcs.FilePath;
import consulo.ide.impl.idea.openapi.vcs.changes.IgnoredFileProvider;
import consulo.ide.impl.components.impl.stores.IProjectStore;
import consulo.project.Project;

import javax.annotation.Nonnull;

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
