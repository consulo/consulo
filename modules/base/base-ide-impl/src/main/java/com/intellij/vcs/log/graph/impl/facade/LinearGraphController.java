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
package com.intellij.vcs.log.graph.impl.facade;

import com.intellij.vcs.log.graph.actions.GraphAction;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import javax.annotation.Nonnull;

import java.awt.*;
import java.util.Set;

public interface LinearGraphController {

  @Nonnull
  LinearGraph getCompiledGraph();

  @Nonnull
  LinearGraphAnswer performLinearGraphAction(@Nonnull LinearGraphAction action);

  interface LinearGraphAction extends GraphAction {
    @javax.annotation.Nullable
    @Override
    PrintElementWithGraphElement getAffectedElement();
  }

  // Integer = nodeId
  class LinearGraphAnswer {
    @javax.annotation.Nullable
    private final GraphChanges<Integer> myGraphChanges;
    @javax.annotation.Nullable
    private final Cursor myCursor;
    @javax.annotation.Nullable
    private final Set<Integer> mySelectedNodeIds;

    public LinearGraphAnswer(@javax.annotation.Nullable GraphChanges<Integer> changes, @javax.annotation.Nullable Cursor cursor, @javax.annotation.Nullable Set<Integer> selectedNodeIds) {
      myGraphChanges = changes;
      myCursor = cursor;
      mySelectedNodeIds = selectedNodeIds;
    }

    public LinearGraphAnswer(@javax.annotation.Nullable Cursor cursor, @javax.annotation.Nullable Set<Integer> selectedNodeIds) {
      this(null, cursor, selectedNodeIds);
    }

    public LinearGraphAnswer(@javax.annotation.Nullable GraphChanges<Integer> changes) {
      this(changes, null, null);
    }

    @javax.annotation.Nullable
    public GraphChanges<Integer> getGraphChanges() {
      return myGraphChanges;
    }

    @javax.annotation.Nullable
    public Runnable getGraphUpdater() {
      return null;
    }

    @javax.annotation.Nullable
    public Cursor getCursorToSet() {
      return myCursor;
    }

    @javax.annotation.Nullable
    public Set<Integer> getSelectedNodeIds() {
      return mySelectedNodeIds;
    }
  }
}
