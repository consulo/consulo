// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.readerMode;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.language.editor.readerMode.ReaderModeProvider.ReaderMode;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Use it to override file &lt;--&gt; reader mode matching
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ReaderModeMatcher {
    /**
     * It's triggered on Reader Mode to check if file matches mode specified.
     *
     * @return null if unable to decide
     */
    @Nullable
    Boolean matches(Project project, VirtualFile file, @Nullable Editor editor, ReaderMode mode);
}
