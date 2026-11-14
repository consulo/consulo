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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChatInput;
import consulo.ai.AIChatInputFactory;
import consulo.ai.AICommand;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceImpl
@Singleton
public class AIChatInputFactoryImpl implements AIChatInputFactory {
    @RequiredUIAccess
    @Override
    public AIChatInput create(Project project, LocalizeValue placeholder, Supplier<List<AICommand>> commands, Disposable parentDisposable) {
        return new AIChatInputImpl(project, placeholder, commands, parentDisposable);
    }
}
