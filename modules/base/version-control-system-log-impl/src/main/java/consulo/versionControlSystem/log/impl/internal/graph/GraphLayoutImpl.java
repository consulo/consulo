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
package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.versionControlSystem.log.graph.GraphLayout;
import consulo.versionControlSystem.log.impl.internal.util.CompressedIntList;
import consulo.versionControlSystem.log.impl.internal.util.IntList;

import java.util.List;

public class GraphLayoutImpl implements GraphLayout {
  
  private final IntList myLayoutIndex;

  
  private final List<Integer> myHeadNodeIndex;
  
  private final int[] myStartLayoutIndexForHead;

  public GraphLayoutImpl(int[] layoutIndex, List<Integer> headNodeIndex, int[] startLayoutIndexForHead) {
    myLayoutIndex = CompressedIntList.newInstance(layoutIndex);
    myHeadNodeIndex = headNodeIndex;
    myStartLayoutIndexForHead = startLayoutIndexForHead;
  }

  @Override
  public int getLayoutIndex(int nodeIndex) {
    return myLayoutIndex.get(nodeIndex);
  }

  @Override
  public int getOneOfHeadNodeIndex(int nodeIndex) {
    return getHeadNodeIndex(getLayoutIndex(nodeIndex));
  }

  public int getHeadNodeIndex(int layoutIndex) {
    return myHeadNodeIndex.get(getHeadOrder(layoutIndex));
  }

  
  public List<Integer> getHeadNodeIndex() {
    return myHeadNodeIndex;
  }

  private int getHeadOrder(int layoutIndex) {
    int a = 0;
    int b = myStartLayoutIndexForHead.length - 1;
    while (b > a) {
      int middle = (a + b + 1) / 2;
      if (myStartLayoutIndexForHead[middle] <= layoutIndex) {
        a = middle;
      }
      else {
        b = middle - 1;
      }
    }
    return a;
  }
}
