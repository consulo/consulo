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
package consulo.ide.impl.idea.util.gist;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.language.psi.stub.gist.GistManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import java.util.List;

/**
* @author VISTALL
* @since 23-Jun-22
*/
@TopicImpl(ComponentScope.APPLICATION)
final class GistManagerBulkFileListener implements BulkFileListener {
  private Provider<GistManager> myGistManager;

  @Inject
  GistManagerBulkFileListener(Provider<GistManager> gistManager) {
    myGistManager = gistManager;
  }

  @Override
  public void after(@Nonnull List<? extends VFileEvent> events) {
    if (events.stream().anyMatch(GistManagerBulkFileListener::shouldDropCache)) {
      ((GistManagerImpl)myGistManager.get()).invalidateGists();
    }
  }

  private static boolean shouldDropCache(VFileEvent e) {
    if (!(e instanceof VFilePropertyChangeEvent)) return false;

    String propertyName = ((VFilePropertyChangeEvent)e).getPropertyName();
    return propertyName.equals(VirtualFile.PROP_NAME) || propertyName.equals(VirtualFile.PROP_ENCODING);
  }
}
