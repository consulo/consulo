/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.util.ActionUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;

public class FocusOppositePaneAction extends AnAction implements DumbAware {
    protected final boolean myScrollToPosition;

    public FocusOppositePaneAction() {
        this(false);
    }

    public FocusOppositePaneAction(boolean scrollToPosition) {
        myScrollToPosition = scrollToPosition;
        ActionUtil.copyFrom(this, getActionId());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        throw new UnsupportedOperationException();
    }

    public void install(@Nonnull JComponent component) {
        registerCustomShortcutSet(getShortcutSet(), component);
    }

    @Nonnull
    private String getActionId() {
        return myScrollToPosition ? "Diff.FocusOppositePaneAndScroll" : "Diff.FocusOppositePane";
    }
}