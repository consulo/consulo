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
package consulo.ide.impl.idea.vcs.log.graph.impl.facade.bek;

import consulo.logging.Logger;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.vcs.log.graph.api.LinearGraph;
import jakarta.annotation.Nonnull;

import static consulo.ide.impl.idea.vcs.log.graph.utils.LinearGraphUtils.getDownNodes;
import static consulo.ide.impl.idea.vcs.log.graph.utils.LinearGraphUtils.getUpNodes;

public class BekChecker {
  private final static Logger LOG = Logger.getInstance(BekChecker.class);

  public static void checkLinearGraph(@Nonnull LinearGraph linearGraph) {
    Pair<Integer, Integer> reversedEdge = findReversedEdge(linearGraph);
    if (reversedEdge != null) {
      LOG.error("Illegal edge: up node " + reversedEdge.first + ", downNode " + reversedEdge.second);
    }
  }

  @jakarta.annotation.Nullable
  public static Pair<Integer, Integer> findReversedEdge(@Nonnull LinearGraph linearGraph) {
    for (int i = 0; i < linearGraph.nodesCount(); i++) {
      for (int downNode : getDownNodes(linearGraph, i)) {
        if (downNode <= i) {
          return Pair.create(i, downNode);
        }
      }

      for (int upNode : getUpNodes(linearGraph, i)) {
        if (upNode >= i) {
          return Pair.create(upNode, i);
        }
      }
    }
    return null;
  }
}
