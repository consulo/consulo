/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.change.ContentRevision;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

public class FakeRevision implements ContentRevision {
  private final FilePath myFile;

  public FakeRevision(String path) throws ChangeListManagerSerialization.OutdatedFakeRevisionException {
    final FilePath file = VcsContextFactory.SERVICE.getInstance().createFilePathOn(new File(path));
    if (file == null) throw new ChangeListManagerSerialization.OutdatedFakeRevisionException();
    myFile = file;
  }

  @Override
  @Nullable
  public String getContent() { return null; }

  @Override
  @Nonnull
  public FilePath getFile() {
    return myFile;
  }

  @Override
  @Nonnull
  public VcsRevisionNumber getRevisionNumber() {
    return VcsRevisionNumber.NULL;
  }
}
