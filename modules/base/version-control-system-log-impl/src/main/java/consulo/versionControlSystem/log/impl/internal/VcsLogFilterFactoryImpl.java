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
package consulo.versionControlSystem.log.impl.internal;

import consulo.annotation.component.ServiceImpl;
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
import consulo.versionControlSystem.log.impl.internal.data.VcsLogBranchFilterImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDateFilterImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogRangeFilterImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogStructureFilterImpl;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogRootFilterImpl;
import consulo.versionControlSystem.log.impl.internal.ui.filter.VcsLogTextFilterImpl;
import consulo.versionControlSystem.log.internal.VcsLogFilterFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
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
@ServiceImpl
@Singleton
public class VcsLogFilterFactoryImpl implements VcsLogFilterFactory {
    @Override
    public VcsLogTextFilter text(String text, boolean isRegexpAllowed, boolean isMatchCase) {
        return new VcsLogTextFilterImpl(text, isRegexpAllowed, isMatchCase);
    }

    @Override
    public VcsLogBranchFilter branches(List<String> branchNames) {
        return VcsLogBranchFilterImpl.fromBranches(branchNames);
    }

    @Override
    public VcsLogBranchFilter branchPatterns(Collection<String> patterns, Set<String> existingBranches) {
        return VcsLogBranchFilterImpl.fromTextPresentation(patterns, existingBranches);
    }

    @Override
    public VcsLogRangeFilter range(List<VcsLogRangeFilter.RefRange> ranges) {
        return new VcsLogRangeFilterImpl(ranges);
    }

    @Override
    public VcsLogHashFilter hashes(Collection<String> hashes) {
        return new VcsLogHashFilterImpl(hashes);
    }

    @Override
    public VcsLogDateFilter dates(@Nullable Date after, @Nullable Date before) {
        return new VcsLogDateFilterImpl(after, before);
    }

    @Override
    public VcsLogUserFilter userNames(Collection<String> userNames, Map<VirtualFile, VcsUser> meData, Set<VcsUser> allUsers) {
        return new VcsLogUserFilterImpl(userNames, meData, allUsers);
    }

    @Override
    public VcsLogStructureFilter paths(Collection<FilePath> files) {
        return new VcsLogStructureFilterImpl(files);
    }

    @Override
    public VcsLogRootFilter roots(Collection<VirtualFile> roots) {
        return new VcsLogRootFilterImpl(roots);
    }

    @Override
    public VcsLogFilterCollection collection(List<VcsLogFilter> filters) {
        VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder builder = new VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder();
        for (VcsLogFilter filter : filters) {
            builder.with(filter);
        }
        return builder.build();
    }
}
