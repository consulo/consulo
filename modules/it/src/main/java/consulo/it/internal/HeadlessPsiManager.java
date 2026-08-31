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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.content.FileIndexFacade;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.psi.PsiModificationTracker;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * Headless {@code PsiManager}: the production {@code PsiManager} binding is {@code IdePsiManagerImpl}
 * in {@code ide-impl}, which is not on the test classpath. This mirrors it with just the core
 * {@link PsiManagerImpl} wiring, omitting the IDE-only resolve-cache scheduling.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
public class HeadlessPsiManager extends PsiManagerImpl {
    @Inject
    public HeadlessPsiManager(Application application,
                              Project project,
                              Provider<FileIndexFacade> fileIndexFacadeProvider,
                              PsiModificationTracker modificationTracker) {
        super(application, project, fileIndexFacadeProvider, modificationTracker);
    }
}
