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
package consulo.versionControlSystem.action;

import consulo.codeEditor.EditorKeys;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.versionControlSystem.internal.VcsContextWrapper;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-07-10
 */
public abstract class AbstractAsyncVcsAction extends DumbAwareAction implements AnActionWithAsyncUpdate {
    protected AbstractAsyncVcsAction() {
    }

    protected AbstractAsyncVcsAction(LocalizeValue text) {
        super(text);
    }

    protected AbstractAsyncVcsAction(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    protected AbstractAsyncVcsAction(
        LocalizeValue text,
        LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return updateAsync(VcsContextWrapper.createInstanceOn(e, EditorKeys.EDITOR_SNAPSHOT), e.getPresentation());
    }

    public abstract Coroutine<?, ?> updateAsync(VcsContext context, Presentation presentation);

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        actionPerformed(VcsContextWrapper.createCachedInstanceOn(e));
    }

    @RequiredUIAccess
    protected abstract void actionPerformed(VcsContext e);
}