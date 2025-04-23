/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;

/**
 * Provides indent options for a PSI file thus allowing different PSI files having different indentation policies withing the same project.
 * The provider can also offer ad hoc actions to control the current indentation policy without opening settings.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class FileIndentOptionsProvider {
    public final static ExtensionPointName<FileIndentOptionsProvider> EP_NAME = ExtensionPointName.create(FileIndentOptionsProvider.class);

    /**
     * Retrieves indent options for PSI file.
     *
     * @param settings Code style settings for which indent options are calculated.
     * @param file     The file to retrieve options for.
     * @return Indent options or {@code null} if the provider can't retrieve them.
     */
    @Nullable
    public abstract IndentOptions getIndentOptions(@Nonnull CodeStyleSettings settings, @Nonnull PsiFile file);

    /**
     * Tells if the provider can be used when a complete file is reformatted.
     *
     * @return True by default
     */
    public boolean useOnFullReformat() {
        return true;
    }

    protected static void notifyIndentOptionsChanged(@Nonnull Project project, @Nullable PsiFile file) {
        CodeStyleSettingsManager.getInstance(project).fireCodeStyleSettingsChanged(file);
    }

    @Nullable
    public IndentStatusBarUIContributor getIndentStatusBarUiContributor(@Nonnull IndentOptions indentOptions) {
        return null;
    }
}
