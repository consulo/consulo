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
package consulo.ide.impl.idea.openapi.vcs.changes.patch;

import consulo.ide.impl.idea.openapi.diff.impl.patch.FilePatch;
import consulo.ide.impl.idea.openapi.diff.impl.patch.PatchSyntaxException;
import consulo.application.util.function.ThrowableComputable;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.collection.MultiMap;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author irengrig
 * @since 2011-02-25
 */
public interface ApplyPatchExecutor<T extends AbstractFilePatchInProgress> {
  @Nls(capitalization = Nls.Capitalization.Title)
  String getName();

  void apply(@Nonnull List<FilePatch> remaining, @Nonnull final MultiMap<VirtualFile, T> patchGroupsToApply,
             @Nullable final LocalChangeList localList,
             @Nullable String fileName,
             @Nullable ThrowableComputable<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo);
}
