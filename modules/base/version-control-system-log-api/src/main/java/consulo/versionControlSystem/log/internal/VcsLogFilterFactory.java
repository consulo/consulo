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
package consulo.versionControlSystem.log.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.log.VcsLogBranchFilter;
import consulo.versionControlSystem.log.VcsLogDateFilter;
import consulo.versionControlSystem.log.VcsLogFilter;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogHashFilter;
import consulo.versionControlSystem.log.VcsLogRangeFilter;
import consulo.versionControlSystem.log.VcsLogRootFilter;
import consulo.versionControlSystem.log.VcsLogStructureFilter;
import consulo.versionControlSystem.log.VcsLogTextFilter;
import consulo.versionControlSystem.log.VcsLogUserFilter;
import consulo.versionControlSystem.log.VcsUser;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface VcsLogFilterFactory {
    VcsLogTextFilter text(String text, boolean isRegexpAllowed, boolean isMatchCase);

    VcsLogBranchFilter branches(List<String> branchNames);

    VcsLogBranchFilter branchPatterns(Collection<String> patterns, Set<String> existingBranches);

    VcsLogRangeFilter range(List<VcsLogRangeFilter.RefRange> ranges);

    VcsLogHashFilter hashes(Collection<String> hashes);

    VcsLogDateFilter dates(@Nullable Date after, @Nullable Date before);

    VcsLogUserFilter userNames(Collection<String> userNames, Map<VirtualFile, VcsUser> meData, Set<VcsUser> allUsers);

    VcsLogStructureFilter paths(Collection<FilePath> files);

    VcsLogRootFilter roots(Collection<VirtualFile> roots);

    VcsLogFilterCollection collection(List<VcsLogFilter> filters);
}
