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
package consulo.ui.ex.action;

import consulo.localize.LocalizeValue;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.coroutine.UIAction;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.*;
import consulo.util.concurrent.coroutine.step.CodeExecution;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-06-28
 */
public abstract class AsyncToggleAction extends AnAction implements Toggleable, AnActionWithAsyncUpdate {
    public AsyncToggleAction() {
    }

    public AsyncToggleAction(String text) {
        super(text);
    }

    public AsyncToggleAction(String text, String description, Image icon) {
        super(text, description, icon);
    }

    public AsyncToggleAction(LocalizeValue text) {
        super(text);
    }

    public AsyncToggleAction(LocalizeValue text, LocalizeValue description, Image icon) {
        super(text, description, icon);
    }

    public AsyncToggleAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    /**
     * Sets the selected state of the action to the specified value.
     *
     * @param e     the action event which caused the state change.
     * @param state the new selected state of the action.
     */
    @RequiredUIAccess
    public abstract void setSelected(AnActionEvent e, boolean state);

    public abstract CoroutineStep<Object, Boolean> isSelectedAsync(AnActionEvent e);

    @Override
    @RequiredUIAccess
    public final void actionPerformed(AnActionEvent e) {
        CoroutineContext context = e.getRequiredData(CoroutineContext.KEY);

        CoroutineScope scope = new CoroutineScope(context);
        scope.putCopyableUserData(UIAccess.KEY, UIAccess.current());

        Coroutine.first(isSelectedAsync(e))
            .then(UIAction.apply((state) -> {
                setSelected(e, Objects.requireNonNull(state));
                Presentation presentation = e.getPresentation();
                Toggleable.setSelected(presentation, state);
                return null;
            }))
            .runAsync(scope, null);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        return Coroutine.first(isSelectedAsync(e))
            .then(CodeExecution.consume((selected) -> {
                Toggleable.setSelected(presentation, selected);

                if (e.isFromContextMenu()) {
                    //force to show check marks instead of toggled icons in context menu
                    presentation.setIcon(null);
                }
            }));
    }
}
