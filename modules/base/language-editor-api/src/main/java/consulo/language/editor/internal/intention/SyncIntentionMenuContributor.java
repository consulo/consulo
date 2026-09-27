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
package consulo.language.editor.internal.intention;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * An {@link IntentionMenuContributor} that has to run on the UI thread, because what it collects the actions
 * from is the shown editor rather than the file. Contributors that only need the file implement
 * {@link IntentionMenuContributor} instead and are collected off the UI thread.
 *
 * @author VISTALL
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SyncIntentionMenuContributor {
    @RequiredUIAccess
    void collectActions(
        Editor hostEditor,
        PsiFile hostFile,
        IntentionsInfo intentions,
        int passIdToShowIntentionsFor,
        int offset
    );
}
