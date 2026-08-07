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
public abstract non-sealed class AddAction<E> extends ToolbarAction<E> {
    public AddAction() {
        super(CommonLocalize.buttonAdd(), PlatformIconGroup.generalAdd());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        doAdd(e);
    }

    @RequiredUIAccess
    protected void doAdd(AnActionEvent e) {
        throw new AbstractMethodError(getClass().getName() + " must implement doAdd() or override actionPerformed()");
    }
}
