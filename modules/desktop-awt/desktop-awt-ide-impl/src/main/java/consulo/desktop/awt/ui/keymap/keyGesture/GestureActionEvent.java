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
package consulo.desktop.awt.ui.keymap.keyGesture;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionEventVisitor;
import consulo.ui.ex.action.ActionManager;
import jakarta.annotation.Nonnull;

public class GestureActionEvent extends AnActionEvent {
  public GestureActionEvent(KeyboardGestureProcessor processor) {
    super(processor.myContext.actionKey,
          processor.myContext.dataContext,
          processor.myContext.actionPlace,
          processor.myContext.actionPresentation, ActionManager.getInstance(),
          0);
  }

  public static class Init extends GestureActionEvent {
    public Init(KeyboardGestureProcessor processor) {
      super(processor);
    }

    @Override
    public void accept(@Nonnull AnActionEventVisitor visitor) {
      visitor.visitGestureInitEvent(this);
    }
  }

  public static class PerformAction extends GestureActionEvent {
    public PerformAction(KeyboardGestureProcessor processor) {
      super(processor);
    }

    @Override
    public void accept(@Nonnull AnActionEventVisitor visitor) {
      visitor.visitGesturePerformedEvent(this);
    }
  }

  public static class Finish extends GestureActionEvent {
    public Finish(KeyboardGestureProcessor processor) {
      super(processor);
    }

    @Override
    public void accept(@Nonnull AnActionEventVisitor visitor) {
      visitor.visitGestureFinishEvent(this);
    }
  }
}