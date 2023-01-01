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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionEventVisitor;
import consulo.ui.ex.action.KeyboardGestureAction;
import consulo.application.dumb.DumbAware;

public class TestGestureAction extends AnAction implements KeyboardGestureAction, DumbAware {
  public void actionPerformed(final AnActionEvent e) {
    e.accept(new AnActionEventVisitor() {
      @Override
      public void visitGestureInitEvent(final AnActionEvent e) {
        System.out.println("TestGestureAction.visitGestureInitEvent");
      }

      @Override
      public void visitGesturePerformedEvent(final AnActionEvent e) {
        System.out.println("TestGestureAction.visitGesturePerformedEvent");
      }

      @Override
      public void visitGestureFinishEvent(final AnActionEvent e) {
        System.out.println("TestGestureAction.visitGestureFinishEvent");
      }

      @Override
      public void visitEvent(final AnActionEvent e) {
        System.out.println("TestGestureAction.visitEvent");
      }
    });
  }
}