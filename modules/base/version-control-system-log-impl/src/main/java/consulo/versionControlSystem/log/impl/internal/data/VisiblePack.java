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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogRefs;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Map;

public class VisiblePack implements VcsLogDataPack {
  
  public static final VisiblePack EMPTY =
    new VisiblePack(DataPack.EMPTY, EmptyVisibleGraph.getInstance(), false, VcsLogFilterCollectionImpl.EMPTY);

  
  private final DataPackBase myDataPack;
  
  private final VisibleGraph<Integer> myVisibleGraph;
  private final boolean myCanRequestMore;
  
  private final VcsLogFilterCollection myFilters;

  VisiblePack(DataPackBase dataPack,
              VisibleGraph<Integer> graph,
              boolean canRequestMore,
              VcsLogFilterCollection filters) {
    myDataPack = dataPack;
    myVisibleGraph = graph;
    myCanRequestMore = canRequestMore;
    myFilters = filters;
  }

  
  public VisibleGraph<Integer> getVisibleGraph() {
    return myVisibleGraph;
  }

  
  public DataPackBase getDataPack() {
    return myDataPack;
  }

  public boolean canRequestMore() {
    return myCanRequestMore;
  }

  
  @Override
  public Map<VirtualFile, VcsLogProvider> getLogProviders() {
    return myDataPack.getLogProviders();
  }

  
  @Override
  public VcsLogRefs getRefs() {
    return myDataPack.getRefsModel();
  }

  public boolean isFull() {
    return myDataPack.isFull();
  }

  @Override
  
  public VcsLogFilterCollection getFilters() {
    return myFilters;
  }

  public VirtualFile getRoot(int row) {
    int head = myVisibleGraph.getRowInfo(row).getOneOfHeads();
    return myDataPack.getRefsModel().rootAtHead(head);
  }
}
