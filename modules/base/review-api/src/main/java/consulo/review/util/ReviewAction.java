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
package consulo.review.util;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.function.Consumer;

/**
 * An action owning its enabled state, so buttons and popups created for it follow the state without touching
 * the template presentation.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public abstract class ReviewAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public static ReviewAction create(LocalizeValue text, @RequiredUIAccess Consumer<? super AnActionEvent> actionPerformed) {
        return new ReviewAction(text) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    private final MutableObservableValue<Boolean> myEnabled = MutableObservableValue.of(true);

    protected ReviewAction(LocalizeValue text) {
        super(text);
    }

    public ObservableValue<Boolean> getEnabled() {
        return myEnabled;
    }

    public boolean isEnabled() {
        return myEnabled.get();
    }

    public void setEnabled(boolean enabled) {
        myEnabled.set(enabled);
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled());
    }
}
