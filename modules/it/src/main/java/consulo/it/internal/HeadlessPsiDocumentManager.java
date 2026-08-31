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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.language.impl.internal.psi.DocumentCommitProcessor;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * The production {@code PsiDocumentManagerImpl} lives in {@code consulo.ide.impl} which is not part of the headless
 * application; index queries commit documents through this manager.
 */
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
@Singleton
public class HeadlessPsiDocumentManager extends PsiDocumentManagerBase {
    @Inject
    public HeadlessPsiDocumentManager(Project project, DocumentCommitProcessor documentCommitProcessor) {
        super(project, documentCommitProcessor);
    }
}
