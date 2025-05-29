// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.InlayPresentationFactory;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Pure presentation. If you need to preserve state between updates you should use {@link StatefulPresentation} or {@link ChangeOnClickPresentation}
 */
public class OnClickPresentation extends StaticDelegatePresentation {
    private final InlayPresentationFactory.ClickListener clickListener;

    public OnClickPresentation(InlayPresentation presentation,
                               InlayPresentationFactory.ClickListener clickListener) {
        super(presentation);
        this.clickListener = clickListener;
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        super.mouseClicked(event, translated);
        clickListener.onClick(event, translated);
    }
}
