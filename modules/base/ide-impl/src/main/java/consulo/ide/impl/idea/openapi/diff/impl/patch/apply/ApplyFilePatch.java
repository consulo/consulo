/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.patch.apply;

import consulo.ide.impl.idea.openapi.diff.impl.patch.ApplyPatchContext;
import consulo.ide.impl.idea.openapi.diff.impl.patch.ApplyPatchStatus;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.CommitContext;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.ApplyPatchForBaseRevisionTexts;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;

public interface ApplyFilePatch {
  Result SUCCESS = new Result(ApplyPatchStatus.SUCCESS) {
    @Override
    public ApplyPatchForBaseRevisionTexts getMergeData() {
      return null;
    }
  };

  Result apply(VirtualFile fileToPatch,
               ApplyPatchContext context,
               Project project,
               FilePath pathBeforeRename,
               Getter<CharSequence> baseContents,
               CommitContext commitContext) throws IOException;

  abstract class Result {
    private final ApplyPatchStatus myStatus;
    private IOException myException;

    protected Result(ApplyPatchStatus status) {
      myStatus = status;
    }

    protected Result(ApplyPatchStatus status, IOException e) {
      myStatus = status;
      myException = e;
    }

    public abstract ApplyPatchForBaseRevisionTexts getMergeData();

    public ApplyPatchStatus getStatus() {
      return myStatus;
    }

    public static Result createThrow(final IOException e) {
      return new Result(ApplyPatchStatus.FAILURE, e) {
        @Override
        public ApplyPatchForBaseRevisionTexts getMergeData() {
          return null;
        }
      };
    }
  }
}
