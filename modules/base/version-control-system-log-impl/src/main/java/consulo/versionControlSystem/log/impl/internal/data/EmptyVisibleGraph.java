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

import consulo.versionControlSystem.log.graph.PrintElement;
import consulo.versionControlSystem.log.graph.RowInfo;
import consulo.versionControlSystem.log.graph.RowType;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.versionControlSystem.log.graph.action.ActionController;
import consulo.versionControlSystem.log.graph.action.GraphAction;
import consulo.versionControlSystem.log.graph.action.GraphAnswer;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

class EmptyVisibleGraph implements VisibleGraph<Integer> {

  private static final VisibleGraph<Integer> INSTANCE = new EmptyVisibleGraph();

  
  public static VisibleGraph<Integer> getInstance() {
    return INSTANCE;
  }

  @Override
  public int getVisibleCommitCount() {
    return 0;
  }

  
  @Override
  public RowInfo<Integer> getRowInfo(int visibleRow) {
    return EmptyRowInfo.INSTANCE;
  }

  @Override
  public @Nullable Integer getVisibleRowIndex(Integer integer) {
    return null;
  }

  
  @Override
  public ActionController<Integer> getActionController() {
    return DumbActionController.INSTANCE;
  }

  @Override
  public int getRecommendedWidth() {
    return 0;
  }

  private static class DumbActionController implements ActionController<Integer> {

    private static ActionController<Integer> INSTANCE = new DumbActionController();

    
    @Override
    public GraphAnswer<Integer> performAction(GraphAction graphAction) {
      return EmptyGraphAnswer.INSTANCE;
    }

    @Override
    public boolean areLongEdgesHidden() {
      return false;
    }

    @Override
    public void setLongEdgesHidden(boolean longEdgesHidden) {
    }

    private static class EmptyGraphAnswer implements GraphAnswer<Integer> {
      private static EmptyGraphAnswer INSTANCE = new EmptyGraphAnswer();

      @Override
      public @Nullable Cursor getCursorToSet() {
        return null;
      }

      @Override
      public @Nullable Integer getCommitToJump() {
        return null;
      }

      @Override
      public @Nullable Runnable getGraphUpdater() {
        return null;
      }

      @Override
      public boolean doJump() {
        return false;
      }
    }
  }

  private static class EmptyRowInfo implements RowInfo<Integer> {

    private static final RowInfo<Integer> INSTANCE = new EmptyRowInfo();

    
    @Override
    public Integer getCommit() {
      return 0;
    }

    
    @Override
    public Integer getOneOfHeads() {
      return 0;
    }

    
    @Override
    public Collection<PrintElement> getPrintElements() {
      return Collections.emptyList();
    }

    
    @Override
    public RowType getRowType() {
      return RowType.NORMAL;
    }
  }
}
