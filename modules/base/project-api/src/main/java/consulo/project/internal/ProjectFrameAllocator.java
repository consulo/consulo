/*
 * Copyright 2013-2024 consulo.io
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
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectFrameAllocator {
    @RequiredUIAccess
    Object allocateFrame(@Nonnull Project project, @Nonnull ProjectOpenContext context);

    <I, O extends Project> Coroutine<I, O> allocateFrame(@Nonnull ProjectOpenContext context, @Nonnull Coroutine<I, O> in);

    <I, O> Coroutine<I, O> initializeSteps(@Nonnull Project project, @Nonnull Coroutine<I, O> in);

    <I, O> Coroutine<I, O> postSteps(@Nonnull Project project, @Nonnull Coroutine<I, O> in);
}
