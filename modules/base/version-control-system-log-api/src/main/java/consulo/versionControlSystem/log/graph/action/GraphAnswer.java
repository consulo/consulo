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
package consulo.versionControlSystem.log.graph.action;

import consulo.versionControlSystem.log.graph.action.ActionController;
import consulo.versionControlSystem.log.graph.action.GraphAction;

import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * The graph returns an instance of the GraphAnswer as a reaction to {@link ActionController#performAction(GraphAction)}.
 */
public interface GraphAnswer<Id> {

  @Nullable
  Cursor getCursorToSet();

  @Nullable
  Id getCommitToJump();

  boolean doJump();

  @Nullable
  Runnable getGraphUpdater();
}
