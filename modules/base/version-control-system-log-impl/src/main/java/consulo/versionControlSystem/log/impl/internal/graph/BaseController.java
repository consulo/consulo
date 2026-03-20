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

import consulo.versionControlSystem.log.graph.LinearGraph;
import consulo.versionControlSystem.log.graph.PermanentGraphInfo;
import org.jspecify.annotations.Nullable;

public class BaseController extends CascadeController {
  public BaseController(PermanentGraphInfo permanentGraphInfo) {
    super(null, permanentGraphInfo);
  }

  
  @Override
  protected LinearGraphAnswer delegateGraphChanged(LinearGraphAnswer delegateAnswer) {
    throw new IllegalStateException();
  }

  @Override
  protected @Nullable LinearGraphAnswer performAction(LinearGraphAction action) {
    return null;
  }

  
  @Override
  public LinearGraph getCompiledGraph() {
    return myPermanentGraphInfo.getLinearGraph();
  }
}
