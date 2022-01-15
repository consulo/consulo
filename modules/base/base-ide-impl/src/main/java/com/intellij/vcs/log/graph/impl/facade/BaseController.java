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

import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.permanent.PermanentGraphInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BaseController extends CascadeController {
  public BaseController(@Nonnull PermanentGraphInfo permanentGraphInfo) {
    super(null, permanentGraphInfo);
  }

  @Nonnull
  @Override
  protected LinearGraphAnswer delegateGraphChanged(@Nonnull LinearGraphAnswer delegateAnswer) {
    throw new IllegalStateException();
  }

  @Nullable
  @Override
  protected LinearGraphAnswer performAction(@Nonnull LinearGraphAction action) {
    return null;
  }

  @Nonnull
  @Override
  public LinearGraph getCompiledGraph() {
    return myPermanentGraphInfo.getLinearGraph();
  }
}
