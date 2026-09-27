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
import consulo.codeEditor.Editor;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionsUI;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * The light bulb and its popup are the parts that cannot exist headlessly; the cached intentions the base
 * class keeps are still real, so a test can read what the daemon computed while nothing is shown.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessIntentionsUI extends IntentionsUI {
    @Inject
    public HeadlessIntentionsUI(Project project) {
        super(project);
    }

    @Override
    public void update(CachedIntentions cachedIntentions, boolean actionsChanged) {
    }

    @Override
    public void hide() {
    }

    @Override
    public void showHint(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
    }

    @Override
    public void showPopup(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
    }
}
