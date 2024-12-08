/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.impl.internal.ui.layout;

import consulo.execution.internal.layout.GridCell;
import consulo.execution.internal.layout.ViewContext;
import consulo.ui.ex.action.ActionGroup;
import consulo.util.concurrent.AsyncResult;

public interface ViewContextEx extends ViewContext {
  RunnerLayoutImpl getLayoutSettings();

  ActionGroup getCellPopupGroup(String place);

  boolean isOriginal();
  
  int getWindow();

  AsyncResult<Void> detachTo(int window, GridCell cell);
}