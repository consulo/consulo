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

package consulo.ide.impl.idea.history.integration.patches;

import consulo.ide.impl.idea.openapi.diff.impl.patch.FilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import consulo.project.Project;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitContext;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.PatchWriter;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class PatchCreator {
  public static void create(Project p, List<Change> changes, String filePath, boolean isReverse, CommitContext commitContext)
          throws IOException, VcsException {
    create(p, ObjectUtil.assertNotNull(p.getBasePath()), changes, filePath, isReverse, commitContext, Charset.defaultCharset());
  }

  public static void create(Project p,
                            @Nonnull String basePath,
                            List<Change> changes,
                            String filePath,
                            boolean isReverse,
                            CommitContext commitContext, Charset charset)
          throws IOException, VcsException {
    List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(p, changes, basePath, isReverse);
    PatchWriter.writePatches(p, filePath, basePath, patches, commitContext, charset);
  }
}
