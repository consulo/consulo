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
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * The production helper lives in ide-impl which is excluded here. The one required piece
 * is the per-document markup model — document change propagation queries it — backed by
 * document user data like the production registry; the rest of the interface defaults.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessCodeEditorInternalHelper implements CodeEditorInternalHelper {
    private static final Key<MarkupModelEx> DOCUMENT_MARKUP = Key.create("headless.document.markup");

    @Override
    public @Nullable MarkupModelEx forDocument(Document document, @Nullable Project project, boolean create) {
        MarkupModelEx model = document.getUserData(DOCUMENT_MARKUP);
        if (model == null && create) {
            model = document.putUserDataIfAbsent(DOCUMENT_MARKUP, new MarkupModelImpl((DocumentEx) document));
        }
        return model;
    }
}
