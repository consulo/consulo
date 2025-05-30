// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.BasePresentation;
import consulo.language.editor.inlay.InlayPresentation;

import java.awt.*;

public class SpacePresentation extends BasePresentation {
    private int width;
    private int height;

    public SpacePresentation(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
    }

    @Override
    public String toString() {
        return " ";
    }

    @Override
    public boolean updateState(InlayPresentation previousPresentation) {
        if (!(previousPresentation instanceof SpacePresentation)) {
            return true;
        }
        SpacePresentation previous = (SpacePresentation) previousPresentation;
        return width != previous.width || height != previous.height;
    }
}
