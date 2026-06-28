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
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-06-28
 */
public abstract class AsyncActionGroup extends ActionGroup {
    protected AsyncActionGroup() {
    }

    protected AsyncActionGroup(String shortName, boolean popup) {
        super(shortName, popup);
    }

    protected AsyncActionGroup(String text, String description, Image icon) {
        super(text, description, icon);
    }

    protected AsyncActionGroup(LocalizeValue text) {
        super(text);
    }

    protected AsyncActionGroup(LocalizeValue text, boolean popup) {
        super(text, popup);
    }

    protected AsyncActionGroup(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    protected AsyncActionGroup(LocalizeValue text, LocalizeValue description, Image icon) {
        super(text, description, icon);
    }

    @Override
    public final AnAction[] getChildren(AnActionEvent e) {
        throw new UnsupportedOperationException("Must call async version");
    }

    public abstract Coroutine<?, List<AnAction>> getChildrenAsync(AnActionEvent e);
}
