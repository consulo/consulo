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
package consulo.ide.impl.idea.openapi.vcs.contentAnnotation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.vcs.RichTextItem;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2011-08-03
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface VcsContentAnnotation {
  @Nullable
  VcsRevisionNumber fileRecentlyChanged(final VirtualFile vf);

  boolean intervalRecentlyChanged(VirtualFile file, final TextRange lineInterval, VcsRevisionNumber currentRevisionNumber);

  class Details {
    private final boolean myLineChanged;
    // meaningful enclosing structure
    private final boolean myMethodChanged;
    private final boolean myFileChanged;
    @Nullable
    private final List<RichTextItem> myDetails;

    public Details(boolean lineChanged, boolean methodChanged, boolean fileChanged, List<RichTextItem> details) {
      myLineChanged = lineChanged;
      myMethodChanged = methodChanged;
      myFileChanged = fileChanged;
      myDetails = details;
    }

    public boolean isLineChanged() {
      return myLineChanged;
    }

    public boolean isMethodChanged() {
      return myMethodChanged;
    }

    public boolean isFileChanged() {
      return myFileChanged;
    }
  }
}
