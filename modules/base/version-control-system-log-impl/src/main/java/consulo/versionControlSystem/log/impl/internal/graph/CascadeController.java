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

import consulo.versionControlSystem.log.graph.GraphElement;
import consulo.versionControlSystem.log.graph.PermanentGraphInfo;
import consulo.versionControlSystem.log.graph.PrintElementWithGraphElement;
import consulo.versionControlSystem.log.graph.LinearGraphController;
import consulo.versionControlSystem.log.graph.LinearGraphUtils;
import org.jspecify.annotations.Nullable;

public abstract class CascadeController implements LinearGraphController {
  private final @Nullable CascadeController myDelegateController;
  
  protected final PermanentGraphInfo myPermanentGraphInfo;

  protected CascadeController(@Nullable CascadeController delegateController, PermanentGraphInfo permanentGraphInfo) {
    myDelegateController = delegateController;
    myPermanentGraphInfo = permanentGraphInfo;
  }

  
  @Override
  public LinearGraphAnswer performLinearGraphAction(LinearGraphAction action) {
    LinearGraphAnswer answer = performAction(action);
    if (answer == null && myDelegateController != null) {
      answer = myDelegateController.performLinearGraphAction(
        new VisibleGraphImpl.LinearGraphActionImpl(convertToDelegate(action.getAffectedElement()), action.getType()));
      answer = delegateGraphChanged(answer);
    }
    if (answer != null) return answer;
    return LinearGraphUtils.DEFAULT_GRAPH_ANSWER;
  }

  private @Nullable PrintElementWithGraphElement convertToDelegate(@Nullable PrintElementWithGraphElement element) {
    if (element == null) return null;
    GraphElement convertedGraphElement = convertToDelegate(element.getGraphElement());
    if (convertedGraphElement == null) return null;
    return PrintElementWithGraphElement.converted(element, convertedGraphElement);
  }

  protected @Nullable GraphElement convertToDelegate(GraphElement graphElement) {
    return graphElement;
  }

  
  protected CascadeController getDelegateController() {
    assert myDelegateController != null;
    return myDelegateController;
  }

  
  public PermanentGraphInfo getPermanentGraphInfo() {
    return myPermanentGraphInfo;
  }

  
  protected abstract LinearGraphAnswer delegateGraphChanged(LinearGraphAnswer delegateAnswer);

  // null mean that this action must be performed by delegateGraphController
  protected abstract @Nullable LinearGraphAnswer performAction(LinearGraphAction action);
}
