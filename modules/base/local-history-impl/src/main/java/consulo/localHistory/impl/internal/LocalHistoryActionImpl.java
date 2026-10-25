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
package consulo.localHistory.impl.internal;

import consulo.localHistory.ActivityId;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

public class LocalHistoryActionImpl implements LocalHistoryAction {
  private final LocalizeValue myName;
  private final @Nullable ActivityId myActivityId;
  private final LocalHistoryEventDispatcher myDispatcher;

  public LocalHistoryActionImpl(LocalHistoryEventDispatcher l, LocalizeValue name, @Nullable ActivityId activityId) {
    myName = name;
    myActivityId = activityId;
    myDispatcher = l;
  }

  public void start() {
    myDispatcher.startAction();
  }

  public void finish() {
    myDispatcher.finishAction(myName, myActivityId);
  }
}
