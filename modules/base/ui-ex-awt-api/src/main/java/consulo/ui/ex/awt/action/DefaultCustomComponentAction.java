// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.action;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.function.Supplier;

public class DefaultCustomComponentAction extends AnAction implements CustomComponentAction {
    @Nonnull
    private final Supplier<? extends JComponent> myProducer;

    public DefaultCustomComponentAction(@Nonnull Supplier<? extends JComponent> producer) {
        myProducer = producer;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        //do nothing
    }

    @Nonnull
    @Override
    public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
        return myProducer.get();
    }
}
