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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;
import java.util.function.Supplier;

/**
 * Makes the {@linkplain AIChatInput boxes} chat messages are written in.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AIChatInputFactory {
    static AIChatInputFactory getInstance() {
        return Application.get().getInstance(AIChatInputFactory.class);
    }

    /**
     * @param commands the commands {@code /} completes, asked whenever it completes - the files of the project come
     *                 after {@code @}
     */
    @RequiredUIAccess
    AIChatInput create(Project project, LocalizeValue placeholder, Supplier<List<AICommand>> commands, Disposable parentDisposable);
}
