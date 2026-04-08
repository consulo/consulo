// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import consulo.ui.AntialiasingType;
import consulo.ui.ex.awt.BrowserHyperlinkListener;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.util.GraphicsUtil;
import org.jetbrains.annotations.Nls;

import javax.swing.JEditorPane;
import javax.swing.text.DefaultCaret;

/**
 * Prefer {@link com.intellij.collaboration.ui.SimpleHtmlPane}
 */
@ApiStatus.Obsolete
public class BaseHtmlEditorPane extends JEditorPane {
    public BaseHtmlEditorPane() {
        setEditorKit(new HTMLEditorKitBuilder().withWordWrapViewFactory().build());

        setEditable(false);
        setOpaque(false);
        addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
        setMargin(JBInsets.emptyInsets());
        GraphicsUtil.setAntialiasingType(this, AntialiasingType.getAATextInfoForSwingComponent());

        DefaultCaret caret = (DefaultCaret) getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    /**
     * @deprecated Deprecated in favour of a generic extension on JEditorPane.
     * Use {@code com.intellij.collaboration.ui.setHtmlBody} instead.
     */
    @Deprecated
    public void setBody(@Nls String body) {
        if (body.isEmpty()) {
            setText("");
        }
        else {
            setText("<html><body>" + body + "</body></html>");
        }
        setSize(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2);
    }
}
