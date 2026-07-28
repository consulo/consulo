// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.readerMode;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.project.Project;

/**
 * Use it to provide an activity that should be applied for editor in Read Mode
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ReaderModeProvider {
    /**
     * It's triggered on Reader Mode turning on or turning off.
     * <p>
     * If {@code fileIsOpenAlready} is true then provider should apply changes only for already opened files,
     * otherwise, if it's false, it should apply changes for every opening file
     */
    default void applyModeChanged(Project project, Editor editor, boolean readerMode, boolean fileIsOpenAlready) {
    }

    enum ReaderMode {
        LIBRARIES,
        READ_ONLY,
        LIBRARIES_AND_READ_ONLY
    }
}
