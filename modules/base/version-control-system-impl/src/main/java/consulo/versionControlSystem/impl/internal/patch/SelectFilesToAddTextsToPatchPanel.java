/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.virtualFileSystem.VirtualFile;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectFilesToAddTextsToPatchPanel {

  private static final Logger LOG = Logger.getInstance(SelectFilesToAddTextsToPatchPanel.class);

  public static Set<Change> getBig(List<Change> changes) {
    Set<Change> exclude = new HashSet<>();
    for (Change change : changes) {
      // try to estimate size via VF: we assume that base content hasn't been changed much
      VirtualFile virtualFile = getVfFromChange(change);
      if (virtualFile != null) {
        if (isBig(virtualFile)) {
          exclude.add(change);
        }
        continue;
      }
      // otherwise, to avoid regression we have to process context length
      ContentRevision beforeRevision = change.getBeforeRevision();
      if (beforeRevision != null) {
        try {
          String content = beforeRevision.getContent();
          if (content == null) {
            FilePath file = beforeRevision.getFile();
            LOG.info("null content for " + (file.getPath()) + ", is dir: " + (file.isDirectory()));
            continue;
          }
          if (content.length() > VcsConfiguration.ourMaximumFileForBaseRevisionSize) {
            exclude.add(change);
          }
        }
        catch (VcsException e) {
          LOG.info(e);
        }
      }
    }
    return exclude;
  }

  private static boolean isBig(@Nonnull VirtualFile virtualFile) {
    return virtualFile.getLength() > VcsConfiguration.ourMaximumFileForBaseRevisionSize;
  }

  @Nullable
  private static VirtualFile getVfFromChange(@Nonnull Change change) {
    ContentRevision after = change.getAfterRevision();
    return after != null ? after.getFile().getVirtualFile() : null;
  }
}
