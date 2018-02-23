/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.vcs.log.data;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.VcsLogDataPack;
import com.intellij.vcs.log.VcsLogFilterCollection;
import com.intellij.vcs.log.VcsLogProvider;
import com.intellij.vcs.log.VcsLogRefs;
import com.intellij.vcs.log.graph.VisibleGraph;
import com.intellij.vcs.log.impl.VcsLogFilterCollectionImpl;
import javax.annotation.Nonnull;

import java.util.Map;

public class VisiblePack implements VcsLogDataPack {
  @Nonnull
  public static final VisiblePack EMPTY =
    new VisiblePack(DataPack.EMPTY, EmptyVisibleGraph.getInstance(), false, VcsLogFilterCollectionImpl.EMPTY);

  @Nonnull
  private final DataPackBase myDataPack;
  @Nonnull
  private final VisibleGraph<Integer> myVisibleGraph;
  private final boolean myCanRequestMore;
  @Nonnull
  private final VcsLogFilterCollection myFilters;

  VisiblePack(@Nonnull DataPackBase dataPack,
              @Nonnull VisibleGraph<Integer> graph,
              boolean canRequestMore,
              @Nonnull VcsLogFilterCollection filters) {
    myDataPack = dataPack;
    myVisibleGraph = graph;
    myCanRequestMore = canRequestMore;
    myFilters = filters;
  }

  @Nonnull
  public VisibleGraph<Integer> getVisibleGraph() {
    return myVisibleGraph;
  }

  @Nonnull
  public DataPackBase getDataPack() {
    return myDataPack;
  }

  public boolean canRequestMore() {
    return myCanRequestMore;
  }

  @Nonnull
  @Override
  public Map<VirtualFile, VcsLogProvider> getLogProviders() {
    return myDataPack.getLogProviders();
  }

  @Nonnull
  @Override
  public VcsLogRefs getRefs() {
    return myDataPack.getRefsModel();
  }

  public boolean isFull() {
    return myDataPack.isFull();
  }

  @Override
  @Nonnull
  public VcsLogFilterCollection getFilters() {
    return myFilters;
  }

  public VirtualFile getRoot(int row) {
    int head = myVisibleGraph.getRowInfo(row).getOneOfHeads();
    return myDataPack.getRefsModel().rootAtHead(head);
  }
}
