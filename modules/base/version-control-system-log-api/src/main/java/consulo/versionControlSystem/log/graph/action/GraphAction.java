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

import consulo.versionControlSystem.log.graph.PrintElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface GraphAction {

  @Nullable
  PrintElement getAffectedElement();

  @Nonnull
  Type getType();

  enum Type {
    MOUSE_CLICK,
    MOUSE_OVER,
    BUTTON_COLLAPSE,
    BUTTON_EXPAND
  }

  class GraphActionImpl implements GraphAction {
    @Nullable private final PrintElement myPrintElement;
    @Nonnull
    private final Type myActionType;

    public GraphActionImpl(@Nullable PrintElement printElement, @Nonnull Type actionType) {
      myPrintElement = printElement;
      myActionType = actionType;
    }

    @Nullable
    @Override
    public PrintElement getAffectedElement() {
      return myPrintElement;
    }

    @Nonnull
    @Override
    public Type getType() {
      return myActionType;
    }
  }
}
