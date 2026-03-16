/*
 * Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.codeEditor.imaginary;

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.disposer.Disposable;

import java.awt.*;

/**
 * @author VISTALL
 * @since 2025-03-07
 */
public class ImaginaryScrollingModel implements ScrollingModel {
    private final ImaginaryEditor myEditor;

    public ImaginaryScrollingModel(ImaginaryEditor editor) {
        myEditor = editor;
    }

    @Override
    public Rectangle getVisibleArea() {
        return new Rectangle(0, 0);
    }

    @Override
    public Rectangle getVisibleAreaOnScrollingFinished() {
        return new Rectangle(0, 0);
    }

    @Override
    public void scrollToCaret(ScrollType scrollType) {
        // no-op
    }

    @Override
    public void scrollTo(LogicalPosition pos, ScrollType scrollType) {
        // no-op
    }

    @Override
    public void runActionOnScrollingFinished(Runnable action) {
        action.run();
    }

    @Override
    public void disableAnimation() {
        // no-op
    }

    @Override
    public void enableAnimation() {
        // no-op
    }

    @Override
    public int getVerticalScrollOffset() {
        return 0;
    }

    @Override
    public int getHorizontalScrollOffset() {
        return 0;
    }

    @Override
    public void scrollVertically(int scrollOffset) {
        // no-op
    }

    @Override
    public void scrollHorizontally(int scrollOffset) {
        // no-op
    }

    @Override
    public void scroll(int horizontalOffset, int verticalOffset) {
        // no-op
    }

    @Override
    public void addVisibleAreaListener(VisibleAreaListener listener) {
        // no-op
    }

    @Override
    public void addVisibleAreaListener(VisibleAreaListener listener, Disposable disposable) {
        // no-op
    }

    @Override
    public void removeVisibleAreaListener(VisibleAreaListener listener) {
        // no-op
    }
}
