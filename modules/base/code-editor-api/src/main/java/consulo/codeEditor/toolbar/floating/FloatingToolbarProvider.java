// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.codeEditor.toolbar.floating;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;

@ExtensionAPI(ComponentScope.PROJECT)
public interface FloatingToolbarProvider {
    default boolean isAutoHideable() {
        return true;
    }

    ActionGroup getActionGroup();

    @RequiredUIAccess
    default Coroutine<?, Boolean> isApplicableAsync(DataContext dataContext) {
        return Coroutine.first(CodeExecution.supply(() -> true));
    }

    @RequiredUIAccess
    default void register(DataContext dataContext, FloatingToolbarComponent component, Disposable parentDisposable) {
    }

    @RequiredUIAccess
    default void onHiddenByEsc(DataContext dataContext) {
    }
}
