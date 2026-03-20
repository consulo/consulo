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
package consulo.versionControlSystem.log.graph;

import consulo.versionControlSystem.log.graph.action.GraphAction;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Set;

public interface LinearGraphController {

  
  LinearGraph getCompiledGraph();

  
  LinearGraphAnswer performLinearGraphAction(LinearGraphAction action);

  interface LinearGraphAction extends GraphAction {
    @Override
    @Nullable PrintElementWithGraphElement getAffectedElement();
  }

  // Integer = nodeId
  class LinearGraphAnswer {
    private final @Nullable GraphChanges<Integer> myGraphChanges;
    private final @Nullable Cursor myCursor;
    private final @Nullable Set<Integer> mySelectedNodeIds;

    public LinearGraphAnswer(@Nullable GraphChanges<Integer> changes, @Nullable Cursor cursor, @Nullable Set<Integer> selectedNodeIds) {
      myGraphChanges = changes;
      myCursor = cursor;
      mySelectedNodeIds = selectedNodeIds;
    }

    public LinearGraphAnswer(@Nullable Cursor cursor, @Nullable Set<Integer> selectedNodeIds) {
      this(null, cursor, selectedNodeIds);
    }

    public LinearGraphAnswer(@Nullable GraphChanges<Integer> changes) {
      this(changes, null, null);
    }

    public @Nullable GraphChanges<Integer> getGraphChanges() {
      return myGraphChanges;
    }

    public @Nullable Runnable getGraphUpdater() {
      return null;
    }

    public @Nullable Cursor getCursorToSet() {
      return myCursor;
    }

    public @Nullable Set<Integer> getSelectedNodeIds() {
      return mySelectedNodeIds;
    }
  }
}
