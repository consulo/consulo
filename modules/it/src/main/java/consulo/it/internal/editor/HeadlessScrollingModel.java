/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.editor;

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModelEx;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;

import java.awt.Rectangle;

/**
 * Scrolling model for a headless editor. There is no viewport, so the visible area is an empty
 * rectangle at the origin and every scroll request is a no-op.
 */
public class HeadlessScrollingModel extends CodeEditorScrollingModelBase implements ScrollingModelEx {
    private Rectangle myVisibleArea = new Rectangle(0, 0, 0, 0);

    private boolean myAnimationEnabled = true;

    public HeadlessScrollingModel(CodeEditorBase editor) {
        super(editor);
    }

    public void setVisibleArea(Rectangle visibleArea) {
        Rectangle oldArea = myVisibleArea;
        if (oldArea.equals(visibleArea)) {
            return;
        }

        myVisibleArea = new Rectangle(visibleArea);

        VisibleAreaEvent event = new VisibleAreaEvent(myEditor, oldArea, myVisibleArea);
        for (VisibleAreaListener listener : myVisibleAreaListeners) {
            listener.visibleAreaChanged(event);
        }
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
    public void accumulateViewportChanges() {
    }

    @Override
    public void flushViewportChanges() {
    }

    @Override
    public void scrollToCaret(ScrollType scrollType) {
    }

    @Override
    public void scrollTo(LogicalPosition pos, ScrollType scrollType) {
    }

    @Override
    public void runActionOnScrollingFinished(Runnable action) {
        action.run();
    }

    @Override
    public void disableAnimation() {
        myAnimationEnabled = false;
    }

    @Override
    public void enableAnimation() {
        myAnimationEnabled = true;
    }

    @Override
    public boolean isAnimationEnabled() {
        return myAnimationEnabled;
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
