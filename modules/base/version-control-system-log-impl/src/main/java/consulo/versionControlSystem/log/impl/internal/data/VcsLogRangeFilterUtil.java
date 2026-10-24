// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.logging.Logger;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogRangeFilter;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.base.HashImpl;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class VcsLogRangeFilterUtil {
    private static final Logger LOG = Logger.getInstance(VcsLogRangeFilterUtil.class);

    private VcsLogRangeFilterUtil() {
    }

    static RangeFilterResult filterByRange(VcsLogStorage storage,
                                           Map<VirtualFile, VcsLogProvider> logProviders,
                                           DataPack dataPack,
                                           VcsLogRangeFilter rangeFilter) {
        Set<Integer> set = new HashSet<>();
        for (VcsLogRangeFilter.RefRange range : rangeFilter.getRanges()) {
            boolean rangeResolvedAnywhere = false;
            for (VirtualFile root : logProviders.keySet()) {
                Pair<CommitId, CommitId> resolvedRange = resolveCommits(storage, dataPack, root, range);
                if (resolvedRange != null) {
                    Set<Integer> commits = getCommitsByRange(storage, dataPack, root, resolvedRange);
                    if (commits == null) {
                        return new RangeFilterResult.Error(); // error => will be handled by the VCS provider
                    }
                    else {
                        set.addAll(commits);
                    }
                    rangeResolvedAnywhere = true;
                }
            }
            // If a range is resolved in some roots, but not all of them => skip others and handle those which know about the range.
            // Otherwise, if none of the roots know about the range => return null and let VcsLogProviders handle the range
            if (!rangeResolvedAnywhere) {
                LOG.warn("Range limits unresolved for: " + range);
                return new RangeFilterResult.InvalidRange();
            }
        }
        return new RangeFilterResult.Commits(set);
    }

    private static @Nullable Set<Integer> getCommitsByRange(VcsLogStorage storage,
                                                            DataPack dataPack,
                                                            VirtualFile root,
                                                            Pair<CommitId, CommitId> range) {
        int fromIndex = storage.getCommitIndex(range.first.getHash(), root);
        int toIndex = storage.getCommitIndex(range.second.getHash(), root);

        return DataPackUtil.subgraphDifference(dataPack.getPermanentGraph(), toIndex, fromIndex);
    }

    private static @Nullable Pair<CommitId, CommitId> resolveCommits(VcsLogStorage vcsLogStorage,
                                                                     DataPack dataPack,
                                                                     VirtualFile root,
                                                                     VcsLogRangeFilter.RefRange range) {
        CommitId from = resolveCommit(vcsLogStorage, dataPack, root, range.exclusiveRef());
        if (from == null) {
            LOG.debug("Can not resolve " + range.exclusiveRef() + " in " + root + " for range " + range);
            return null;
        }
        CommitId to = resolveCommit(vcsLogStorage, dataPack, root, range.inclusiveRef());
        if (to == null) {
            LOG.debug("Can not resolve " + range.inclusiveRef() + " in " + root + " for range " + range);
            return null;
        }
        return Pair.create(from, to);
    }

    private static @Nullable CommitId resolveCommit(VcsLogStorage storage, DataPack dataPack, VirtualFile root, String refName) {
        VcsLogProvider provider = dataPack.getLogProviders().get(root);
        if (provider != null && provider.isFullHash(root, refName)) {
            CommitId commitId = new CommitId(HashImpl.build(refName), root);
            return storage.containsCommit(commitId) ? commitId : null;
        }

        VcsRef ref = DataPackUtil.findBranch(dataPack.getRefsModel(), refName, root);
        if (ref != null) {
            return new CommitId(ref.getCommitHash(), root);
        }
        if (refName.length() >= VcsLogUtil.SHORT_HASH_LENGTH && VcsLogUtil.GIT_HASH_REGEX.matcher(refName).matches()) {
            // don't search for too short hashes: high probability to treat a ref, existing not in all roots, as a hash
            return storage.findCommitId(new CommitIdByStringCondition(refName));
        }
        return null;
    }
}
