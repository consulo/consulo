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

package consulo.ide.impl.idea.openapi.vcs.changes.shelf;

import consulo.project.Project;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.ide.impl.idea.openapi.vcs.FilePathImpl;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.ide.impl.idea.openapi.vcs.changes.CurrentBinaryContentRevision;
import consulo.ide.impl.idea.openapi.vcs.changes.TextRevisionNumber;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import org.jdom.Element;
import javax.annotation.Nonnull;

import java.io.File;

/**
 * @author yole
 */
public class ShelvedBinaryFile implements JDOMExternalizable {
  public String BEFORE_PATH;
  public String AFTER_PATH;
  @javax.annotation.Nullable
  public String SHELVED_PATH;         // null if binary file was deleted

  public ShelvedBinaryFile() {
  }

  public ShelvedBinaryFile(final String beforePath, final String afterPath, @javax.annotation.Nullable final String shelvedPath) {
    assert beforePath != null || afterPath != null;
    BEFORE_PATH = beforePath;
    AFTER_PATH = afterPath;
    SHELVED_PATH = shelvedPath;
  }

  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
  }

  public FileStatus getFileStatus() {
    if (BEFORE_PATH == null) {
      return FileStatus.ADDED;
    }
    if (SHELVED_PATH == null) {
      return FileStatus.DELETED;
    }
    return FileStatus.MODIFIED;
  }

  public Change createChange(final Project project) {
    ContentRevision before = null;
    ContentRevision after = null;
    final File baseDir = new File(project.getBaseDir().getPath());
    if (BEFORE_PATH != null) {
      final FilePathImpl file = new FilePathImpl(new File(baseDir, BEFORE_PATH), false);
      file.refresh();
      before = new CurrentBinaryContentRevision(file) {
        @Nonnull
        @Override
        public VcsRevisionNumber getRevisionNumber() {
          return new TextRevisionNumber(VcsBundle.message("local.version.title"));
        }
      };
    }
    if (AFTER_PATH != null) {
      final FilePathImpl file = new FilePathImpl(new File(baseDir, AFTER_PATH), false);
      file.refresh();
      after = new ShelvedBinaryContentRevision(file, SHELVED_PATH);
    }
    return new Change(before, after);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ShelvedBinaryFile that = (ShelvedBinaryFile)o;

    if (AFTER_PATH != null ? !AFTER_PATH.equals(that.AFTER_PATH) : that.AFTER_PATH != null) return false;
    if (BEFORE_PATH != null ? !BEFORE_PATH.equals(that.BEFORE_PATH) : that.BEFORE_PATH != null) return false;
    if (SHELVED_PATH != null ? !SHELVED_PATH.equals(that.SHELVED_PATH) : that.SHELVED_PATH != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = BEFORE_PATH != null ? BEFORE_PATH.hashCode() : 0;
    result = 31 * result + (AFTER_PATH != null ? AFTER_PATH.hashCode() : 0);
    result = 31 * result + (SHELVED_PATH != null ? SHELVED_PATH.hashCode() : 0);
    return result;
  }
}