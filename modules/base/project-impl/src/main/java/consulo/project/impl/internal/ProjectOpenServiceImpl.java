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
package consulo.project.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.ProjectOpenService;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.LocalFileSystem;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-01-29
 */
@ServiceImpl
@Singleton
public class ProjectOpenServiceImpl implements ProjectOpenService {
    private final ApplicationConcurrency myApplicationConcurrency;
    private final ProgressBuilderFactory myProgressBuilderFactory;

    @Inject
    public ProjectOpenServiceImpl(ApplicationConcurrency applicationConcurrency,
                                  ProgressBuilderFactory progressBuilderFactory) {
        myApplicationConcurrency = applicationConcurrency;
        myProgressBuilderFactory = progressBuilderFactory;
    }

    @Nonnull
    @Override
    public CompletableFuture<Project> openProjectAsync(
        @Nonnull Path filePath,
        @Nonnull UIAccess uiAccess,
        @Nonnull ProjectOpenContext context) {

        myProgressBuilderFactory.newProgressBuilder(null, LocalizeValue.localizeTODO("Opening project..."))
            .cancelable()
            .execute(uiAccess, initial -> {
                return initial.then(CodeExecution.apply((o, continuation) -> {
                  return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(filePath);
                }));
            });

        return CompletableFuture.failedFuture(null);
    }
}
