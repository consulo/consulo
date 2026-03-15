/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.codeEditor;

import consulo.codeEditor.event.VisibleAreaListener;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import java.awt.*;

/**
 * Provides services for getting the visible area of the editor and scrolling the editor.
 *
 * @see Editor#getScrollingModel()
 */
public interface ScrollingModel {
  
  Rectangle getVisibleArea();

  
  Rectangle getVisibleAreaOnScrollingFinished();

  void scrollToCaret(ScrollType scrollType);

  void scrollTo(LogicalPosition pos, ScrollType scrollType);

  void runActionOnScrollingFinished(@RequiredUIAccess  Runnable action);

  void disableAnimation();

  void enableAnimation();

  default boolean isAnimationEnabled() {
    return true;
  }
  
  int getVerticalScrollOffset();

  int getHorizontalScrollOffset();

  void scrollVertically(int scrollOffset);

  void scrollHorizontally(int scrollOffset);

  void scroll(int horizontalOffset, int verticalOffset);

  void addVisibleAreaListener(VisibleAreaListener listener);

  void addVisibleAreaListener(VisibleAreaListener listener, Disposable disposable);

  void removeVisibleAreaListener(VisibleAreaListener listener);
}
