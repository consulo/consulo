/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.Module;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-plugin extension that declares how a file's indexed output depends on module-level
 * build context (preprocessor macros, target triple, preprocessor symbols, cfg flags, etc.).
 *
 * <p>Platform dispatches to providers by file type using {@link #getInputFileTypes()} — a
 * precomputed {@code FileType → providers} map keeps lookups O(1). A file may be claimed by
 * several providers contributing independent dimensions (macros, toolchain, target); the
 * index layer composes their returned {@link IndexOption}s.</p>
 *
 * <p>See {@code MODULE_AWARE_INDEX.md} for the full design.</p>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ModuleAwareIndexOptionProvider {


    /**
     * Stable identifier — appears in stored per-file meta. Changing it orphans all
     * existing entries written by this provider (they reindex on next access).
     */
    String getId();

    /**
     * Bump when the shape of options produced by this provider changes in a way that
     * affects index output. All stored hashes with a different version are invalidated.
     */
    int getVersion();

    /**
     * File types this provider claims. Used for platform-side filtering before any
     * {@link #getOptions} call. Static — must not vary per call.
     */
    Set<FileType> getInputFileTypes();

    /**
     * The options of {@code file} that can be known from the roots model and the file itself, with no index access:
     * this is called while the file is being indexed and parsed. Must not return {@code null} — use
     * {@link IndexOption#fullySharable()} when no module-aware context applies to this particular file. When
     * {@link #analyze} has produced a value for the file, that value is what indexing and parsing see instead.
     */
    IndexOption getOptions(Module module, VirtualFile file);

    /**
     * The analysis that needs more than one file: given the files that changed, returns new option values for every
     * file of this provider's types whose options may now differ. The platform decides when to call this, keeps the
     * returned values, works out which ones actually moved, rescans and reparses exactly those, and hands the kept
     * value back through {@link ModuleAwareIndexOptions#getRecordedOptions} at index and parse time. It is called
     * outside indexing, in a read action, and must not query indexes: file contents and the roots model only.
     * {@code changedFiles} is {@code null} for a full pass over the project.
     */
    default Map<VirtualFile, List<IndexOption>> analyze(Project project, @Nullable Collection<VirtualFile> changedFiles) {
        return Map.of();
    }
}
