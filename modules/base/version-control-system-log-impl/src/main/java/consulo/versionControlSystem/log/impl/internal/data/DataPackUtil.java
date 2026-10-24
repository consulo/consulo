// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.versionControlSystem.log.VcsLogRefs;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.graph.PermanentGraphInfo;
import consulo.versionControlSystem.log.impl.internal.graph.GraphUtil;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class DataPackUtil {
    private DataPackUtil() {
    }

    public static @Nullable VcsRef findBranch(VcsLogRefs refs, String name, VirtualFile root) {
        return VcsLogUtil.findBranch(refs, root, name);
    }

    public static @Nullable Set<Integer> subgraphDifference(PermanentGraph<Integer> permanentGraph,
                                                           VcsRef withRef,
                                                           VcsRef withoutRef,
                                                           VcsLogStorage storage) {
        if (!withoutRef.getRoot().equals(withRef.getRoot())) {
            return null;
        }

        int withRefIndex = storage.getCommitIndex(withRef.getCommitHash(), withRef.getRoot());
        int withoutRefIndex = storage.getCommitIndex(withoutRef.getCommitHash(), withoutRef.getRoot());

        return subgraphDifference(permanentGraph, withRefIndex, withoutRefIndex);
    }

    @SuppressWarnings("unchecked")
    public static @Nullable Set<Integer> subgraphDifference(PermanentGraph<Integer> permanentGraph, int withRefIndex, int withoutRefIndex) {
        if (withRefIndex == withoutRefIndex) {
            return new HashSet<>();
        }

        if (!(permanentGraph instanceof PermanentGraphInfo<?> info)) {
            return null;
        }
        PermanentGraphInfo<Integer> permanentGraphInfo = (PermanentGraphInfo<Integer>) info;

        int withRefNode = permanentGraphInfo.getPermanentCommitsInfo().getNodeId(withRefIndex);
        int withoutRefNode = permanentGraphInfo.getPermanentCommitsInfo().getNodeId(withoutRefIndex);

        if (withRefNode < 0 || withoutRefNode < 0) {
            return null;
        }

        Set<Integer> withRefNodeIds = GraphUtil.subgraphDifference(permanentGraphInfo.getLinearGraph(), withRefNode, withoutRefNode);
        Set<Integer> result = new HashSet<>();
        for (int nodeId : withRefNodeIds) {
            result.add(permanentGraphInfo.getPermanentCommitsInfo().getCommitId(nodeId));
        }
        return result;
    }
}
