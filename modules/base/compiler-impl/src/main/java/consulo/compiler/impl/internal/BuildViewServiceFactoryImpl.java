/*
 * Copyright 2013-2021 consulo.io
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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.UUID;

/**
 * @author VISTALL
 * @since 28/11/2021
 */
@Singleton
@ServiceImpl
public class BuildViewServiceFactoryImpl implements BuildViewServiceFactory {
    @Nonnull
    @Override
    public BuildViewService createBuildViewService(Project project, UUID sessionId, String contentName) {
        return new BuildViewServiceImpl(project, sessionId, contentName);
    }
}
