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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.application.util.function.ThrowableComputable;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.patch.FilePatch;
import consulo.versionControlSystem.impl.internal.patch.PatchSyntaxException;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * @author irengrig
 * @since 2011-02-25
 */
public interface ApplyPatchExecutor<T extends AbstractFilePatchInProgress> {
  
  String getName();

  void apply(List<FilePatch> remaining, MultiMap<VirtualFile, T> patchGroupsToApply,
             @Nullable LocalChangeList localList,
             @Nullable String fileName,
             @Nullable ThrowableComputable<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo);
}
