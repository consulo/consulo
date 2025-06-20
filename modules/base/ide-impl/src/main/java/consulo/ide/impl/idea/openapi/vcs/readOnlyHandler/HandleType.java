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
package consulo.ide.impl.idea.openapi.vcs.readOnlyHandler;

import consulo.ide.IdeBundle;
import consulo.application.ApplicationManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.ReadOnlyAttributeUtil;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class HandleType {
  private final String myName;
  private final boolean myUseVcs;

  public static final HandleType USE_FILE_SYSTEM = new HandleType(IdeBundle.message("handle.ro.file.status.type.using.file.system"), false) {
    public void processFiles(final Collection<VirtualFile> virtualFiles, String changelist) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          try {
            for (VirtualFile file : virtualFiles) {
              ReadOnlyAttributeUtil.setReadOnlyAttribute(file, false);
              file.refresh(false, false);
            }
          }
          catch (IOException e) {
            //ignore
          }
        }
      });
    }
  };

  protected HandleType(String name, boolean useVcs) {
    myName = name;
    myUseVcs = useVcs;
  }

  public String toString() {
    return myName;
  }

  public boolean getUseVcs() {
    return myUseVcs;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final HandleType that = (HandleType)o;

    if (myUseVcs != that.myUseVcs) return false;
    if (myName != null ? !myName.equals(that.myName) : that.myName != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myName != null ? myName.hashCode() : 0);
    result = 31 * result + (myUseVcs ? 1 : 0);
    return result;
  }

  public abstract void processFiles(final Collection<VirtualFile> virtualFiles, @Nullable String changelist);

  public List<String> getChangelists() {
    return Collections.emptyList();
  }

  @Nullable
  public String getDefaultChangelist() {
    return null;
  }
}
