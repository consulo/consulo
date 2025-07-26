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
package consulo.codeEditor.impl.internal.textEditor;

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * @author yole
 */
public class TextComponentScrollingModel implements ScrollingModel {
    private final JTextComponent myTextComponent;

    public TextComponentScrollingModel(@Nonnull JTextComponent textComponent) {
        myTextComponent = textComponent;
    }

    @Nonnull
    @Override
    public Rectangle getVisibleArea() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nonnull
    @Override
    public Rectangle getVisibleAreaOnScrollingFinished() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void scrollToCaret(@Nonnull final ScrollType scrollType) {
        final int position = myTextComponent.getCaretPosition();
        try {
            final Rectangle rectangle = myTextComponent.modelToView(position);
            myTextComponent.scrollRectToVisible(rectangle);
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void scrollTo(@Nonnull final LogicalPosition pos, @Nonnull final ScrollType scrollType) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void runActionOnScrollingFinished(@Nonnull final Runnable action) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void disableAnimation() {
    }

    @Override
    public void enableAnimation() {
    }

    @Override
    public int getVerticalScrollOffset() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getHorizontalScrollOffset() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void scrollVertically(final int scrollOffset) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void scrollHorizontally(final int scrollOffset) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void scroll(int horizontalOffset, int verticalOffset) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addVisibleAreaListener(@Nonnull final VisibleAreaListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addVisibleAreaListener(@Nonnull VisibleAreaListener listener, @Nonnull Disposable disposable) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void removeVisibleAreaListener(@Nonnull final VisibleAreaListener listener) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
