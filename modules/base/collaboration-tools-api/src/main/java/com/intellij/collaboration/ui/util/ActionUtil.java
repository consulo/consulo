// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public final class ActionUtil {
    private ActionUtil() {
    }

    public static @Nullable @Nls String getName(@Nonnull Action action) {
        Object value = action.getValue(Action.NAME);
        return value instanceof String s ? s : null;
    }

    public static void setName(@Nonnull Action action, @Nullable @Nls String name) {
        action.putValue(Action.NAME, name);
    }

    public static @Nonnull AnAction toAnAction(@Nonnull Action action) {
        String name = getName(action);
        return new DumbAwareAction(name != null ? name : "") {
            @Override
            public @Nonnull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(action.isEnabled());
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent event) {
                performAction(action, event);
            }
        };
    }

    @ApiStatus.Internal
    public static void performAction(@Nonnull Action action, @Nonnull AnActionEvent event) {
        ActionEvent actionEvent = new ActionEvent(
            event.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT),
            ActionEvent.ACTION_PERFORMED,
            "execute",
            event.getModifiers()
        );
        action.actionPerformed(actionEvent);
    }

    public static @Nonnull Action swingAction(@Nls @Nonnull String name, @Nonnull Consumer<ActionEvent> action) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        };
    }

    @ApiStatus.Internal
    public static @Nonnull DumbAwareAction iconAction(@Nonnull Icon icon, @Nonnull Consumer<AnActionEvent> action) {
        return new DumbAwareAction(icon) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                action.accept(e);
            }
        };
    }
}
