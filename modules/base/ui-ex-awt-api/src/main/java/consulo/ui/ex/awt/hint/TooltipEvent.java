/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ui.ex.awt.hint;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nullable;
import java.awt.event.InputEvent;

public class TooltipEvent {

  private InputEvent myInputEvent;
  private boolean myIsEventInsideBalloon;

  @Nullable
  private AnAction myAction;
  @Nullable
  private AnActionEvent myActionEvent;

  public TooltipEvent(InputEvent inputEvent, boolean isEventInsideBalloon, @Nullable AnAction action, @Nullable AnActionEvent actionEvent) {
    myInputEvent = inputEvent;
    myIsEventInsideBalloon = isEventInsideBalloon;
    myAction = action;
    myActionEvent = actionEvent;
  }

  public InputEvent getInputEvent() {
    return myInputEvent;
  }

  public boolean isIsEventInsideBalloon() {
    return myIsEventInsideBalloon;
  }

  @Nullable
  public AnAction getAction() {
    return myAction;
  }

  @Nullable
  public AnActionEvent getActionEvent() {
    return myActionEvent;
  }
}
