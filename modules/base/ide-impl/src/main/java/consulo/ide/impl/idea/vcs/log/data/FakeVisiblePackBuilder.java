/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.data;

import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsRef;
import consulo.ide.impl.idea.vcs.log.graph.GraphColorManagerImpl;
import consulo.ide.impl.idea.vcs.log.graph.VisibleGraph;
import consulo.ide.impl.idea.vcs.log.graph.api.permanent.PermanentGraphInfo;
import consulo.ide.impl.idea.vcs.log.graph.collapsing.CollapsedController;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.BaseController;
import consulo.ide.impl.idea.vcs.log.graph.impl.facade.VisibleGraphImpl;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogUtil;
import javax.annotation.Nonnull;

import java.util.Map;
import java.util.Set;

public class FakeVisiblePackBuilder {
  @Nonnull
  private final VcsLogStorage myHashMap;

  public FakeVisiblePackBuilder(@Nonnull VcsLogStorage hashMap) {
    myHashMap = hashMap;
  }

  @Nonnull
  public VisiblePack build(@Nonnull VisiblePack visiblePack) {
    if (visiblePack.getVisibleGraph() instanceof VisibleGraphImpl && visiblePack.getVisibleGraph().getVisibleCommitCount() > 0) {
      return build(visiblePack.getDataPack(), ((VisibleGraphImpl<Integer>)visiblePack.getVisibleGraph()), visiblePack.getFilters());
    }
    else {
      VisibleGraph<Integer> newGraph = EmptyVisibleGraph.getInstance();
      DataPackBase newPack = new DataPackBase(visiblePack.getDataPack().getLogProviders(), createEmptyRefsModel(), false);
      return new VisiblePack(newPack, newGraph, true, visiblePack.getFilters());
    }
  }

  @Nonnull
  private VisiblePack build(@Nonnull DataPackBase oldPack,
                            @Nonnull VisibleGraphImpl<Integer> oldGraph,
                            @Nonnull VcsLogFilterCollection filters) {
    final PermanentGraphInfo<Integer> info = oldGraph.buildSimpleGraphInfo();
    Set<Integer> heads = ContainerUtil.map2Set(info.getPermanentGraphLayout().getHeadNodeIndex(),
                                               integer -> info.getPermanentCommitsInfo().getCommitId(integer));

    RefsModel newRefsModel = createRefsModel(oldPack.getRefsModel(), heads, oldGraph, oldPack.getLogProviders());
    DataPackBase newPack = new DataPackBase(oldPack.getLogProviders(), newRefsModel, false);

    GraphColorManagerImpl colorManager =
            new GraphColorManagerImpl(newRefsModel, DataPack.createHashGetter(myHashMap), DataPack.getRefManagerMap(oldPack.getLogProviders()));

    VisibleGraph<Integer> newGraph =
            new VisibleGraphImpl<>(new CollapsedController(new BaseController(info), info, null), info, colorManager);

    return new VisiblePack(newPack, newGraph, true, filters);
  }

  @Nonnull
  private RefsModel createEmptyRefsModel() {
    return new RefsModel(ContainerUtil.newHashMap(), ContainerUtil.newHashSet(), myHashMap, ContainerUtil.newHashMap());
  }

  private RefsModel createRefsModel(@Nonnull RefsModel refsModel,
                                    @Nonnull Set<Integer> heads,
                                    @Nonnull VisibleGraph<Integer> visibleGraph,
                                    @Nonnull Map<VirtualFile, VcsLogProvider> providers) {
    Set<VcsRef> branchesAndHeads = ContainerUtil.newHashSet();
    refsModel.getBranches().stream().filter(ref -> {
      int index = myHashMap.getCommitIndex(ref.getCommitHash(), ref.getRoot());
      Integer row = visibleGraph.getVisibleRowIndex(index);
      return row != null && row >= 0;
    }).forEach(branchesAndHeads::add);
    heads.stream().flatMap(head -> refsModel.refsToCommit(head).stream()).forEach(branchesAndHeads::add);

    Map<VirtualFile, Set<VcsRef>> map = VcsLogUtil.groupRefsByRoot(branchesAndHeads);
    Map<VirtualFile, CompressedRefs> refs = ContainerUtil.newHashMap();
    for (VirtualFile root : providers.keySet()) {
      Set<VcsRef> refsForRoot = map.get(root);
      refs.put(root, new CompressedRefs(refsForRoot == null ? ContainerUtil.newHashSet() : refsForRoot, myHashMap));
    }
    return new RefsModel(refs, heads, myHashMap, providers);
  }
}
