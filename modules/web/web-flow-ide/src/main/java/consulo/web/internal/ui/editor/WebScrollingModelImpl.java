/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.internal.ui.editor;

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModelEx;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;

import java.awt.*;

/**
 * @author VISTALL
 * @since 06/12/2020
 */
public class WebScrollingModelImpl extends CodeEditorScrollingModelBase implements ScrollingModelEx {
  private Rectangle myVisibleArea = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);

  public WebScrollingModelImpl(CodeEditorBase editor) {
    super(editor);
  }

  public void setVisibleAreaFromClient(Rectangle visibleArea) {
    Rectangle oldArea = myVisibleArea;
    if (oldArea.equals(visibleArea)) {
      return;
    }

    myVisibleArea = visibleArea;

    VisibleAreaEvent event = new VisibleAreaEvent(myEditor, oldArea, visibleArea);
    for (VisibleAreaListener listener : myVisibleAreaListeners) {
      listener.visibleAreaChanged(event);
    }
  }

  @Override
  public void accumulateViewportChanges() {

  }

  @Override
  public void flushViewportChanges() {

  }


  @Override
  public Rectangle getVisibleArea() {
    return myVisibleArea;
  }


  @Override
  public Rectangle getVisibleAreaOnScrollingFinished() {
    return myVisibleArea;
  }

  @Override
  public void scrollToCaret(ScrollType scrollType) {

  }

  @Override
  public void scrollTo(LogicalPosition pos, ScrollType scrollType) {

  }

  @Override
  public void disableAnimation() {

  }

  @Override
  public void enableAnimation() {

  }

  @Override
  public int getVerticalScrollOffset() {
    return myVisibleArea.y;
  }

  @Override
  public int getHorizontalScrollOffset() {
    return myVisibleArea.x;
  }

  @Override
  public void scrollVertically(int scrollOffset) {

  }

  @Override
  public void scrollHorizontally(int scrollOffset) {

  }

  @Override
  public void scroll(int horizontalOffset, int verticalOffset) {

  }
}
