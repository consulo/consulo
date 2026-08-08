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
package consulo.ui.ex.toolbar;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public abstract non-sealed class RemoveAction<E> extends ToolbarAction<E> {
    public RemoveAction() {
        super(CommonLocalize.buttonRemove(), PlatformIconGroup.generalRemove());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        E value = getSelectedValue(e);
        if (value != null) {
            doRemove(value, e);
        }
    }

    @RequiredUIAccess
    protected void doRemove(E value, AnActionEvent e) {
        throw new AbstractMethodError(getClass().getName() + " must implement doRemove() or override actionPerformed()");
    }
}
