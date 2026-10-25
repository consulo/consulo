// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.log;

import consulo.application.Application;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.log.internal.VcsLogFilterFactory;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class VcsLogFilterObject {
    public static final String ME = "*";

    private static final Pattern HASH_SEPARATOR = Pattern.compile("[,;\\s]+");

    private static VcsLogFilterFactory factory() {
        return Application.get().getInstance(VcsLogFilterFactory.class);
    }

    public static VcsLogTextFilter fromPattern(String text) {
        return fromPattern(text, false, false);
    }

    public static VcsLogTextFilter fromPattern(String text, boolean isRegexpAllowed, boolean isMatchCase) {
        return factory().text(text, isRegexpAllowed, isMatchCase);
    }

    public static VcsLogBranchFilter fromBranch(String branchName) {
        return fromBranches(List.of(branchName));
    }

    public static VcsLogBranchFilter fromBranches(List<String> branchNames) {
        return factory().branches(branchNames);
    }

    public static VcsLogRangeFilter fromRange(String exclusiveRef, String inclusiveRef) {
        return fromRange(List.of(new VcsLogRangeFilter.RefRange(exclusiveRef, inclusiveRef)));
    }

    public static VcsLogRangeFilter fromRange(List<VcsLogRangeFilter.RefRange> ranges) {
        return factory().range(ranges);
    }

    public static VcsLogBranchFilter fromBranchPatterns(Collection<String> patterns, Set<String> existingBranches) {
        return factory().branchPatterns(patterns, existingBranches);
    }

    public static @Nullable VcsLogHashFilter fromHash(String text) {
        List<String> hashes = new ArrayList<>();
        for (String word : HASH_SEPARATOR.split(text.trim())) {
            if (word.isEmpty()) {
                continue;
            }
            if (!VcsLogUtil.GIT_HASH_REGEX.matcher(word).matches()) {
                return null;
            }
            hashes.add(word);
        }
        if (hashes.isEmpty()) {
            return null;
        }

        return fromHashes(hashes);
    }

    public static VcsLogHashFilter fromHashes(Collection<String> hashes) {
        return factory().hashes(hashes);
    }

    public static VcsLogDateFilter fromDates(@Nullable Date after, @Nullable Date before) {
        return factory().dates(after, before);
    }

    public static VcsLogDateFilter fromDates(long after, long before) {
        return fromDates(after > 0 ? new Date(after) : null, before > 0 ? new Date(before) : null);
    }

    public static VcsLogUserFilter fromUser(VcsUser user) {
        return fromUser(user, Set.of(user));
    }

    public static VcsLogUserFilter fromUser(VcsUser user, Set<VcsUser> allUsers) {
        return fromUserNames(List.of(VcsUserUtil.getShortPresentation(user)), Map.of(), allUsers);
    }

    public static VcsLogUserFilter fromUserNames(Collection<String> userNames, Map<VirtualFile, VcsUser> meData, Set<VcsUser> allUsers) {
        return factory().userNames(userNames, meData, allUsers);
    }

    public static VcsLogStructureFilter fromPaths(Collection<FilePath> files) {
        return factory().paths(files);
    }

    public static VcsLogStructureFilter fromVirtualFiles(Collection<VirtualFile> files) {
        List<FilePath> paths = new ArrayList<>(files.size());
        for (VirtualFile file : files) {
            paths.add(VcsUtil.getFilePath(file));
        }
        return fromPaths(paths);
    }

    public static VcsLogRootFilter fromRoot(VirtualFile root) {
        return fromRoots(List.of(root));
    }

    public static VcsLogRootFilter fromRoots(Collection<VirtualFile> roots) {
        return factory().roots(roots);
    }

    public static VcsLogFilterCollection collection(@Nullable VcsLogFilter... filters) {
        List<VcsLogFilter> list = new ArrayList<>();
        for (VcsLogFilter filter : filters) {
            if (filter != null) {
                list.add(filter);
            }
        }
        return factory().collection(list);
    }

    private VcsLogFilterObject() {
    }
}
