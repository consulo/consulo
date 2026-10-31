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
package consulo.execution.runAnything.codeBlock;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.Language;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Runs a whole block of code of a document - a shell script in a terminal above all
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CodeBlockRunner {
    /**
     * @param languageName the language a block names, as written - a block may name a language no plugin knows, a shell above all
     * @param language     the language of that name, if any plugin knows it
     */
    boolean isApplicable(String languageName, @Nullable Language language);

    LocalizeValue getTitle();

    /**
     * @param code             the code of the block, its prompts left as written
     * @param workingDirectory where to run it, the default one of the runner when {@code null}
     */
    @RequiredUIAccess
    void run(Project project, String code, @Nullable VirtualFile workingDirectory);
}
