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
package consulo.fileEditor.internal;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorWindow;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-07
 */
public interface VetoActionCloseHandler {
    static boolean isClosable(@Nullable FileEditorComposite composite, Project project) {
        if (composite == null) {
            return true;
        }

        for (FileEditor editor : composite.getEditors()) {
            if (editor instanceof VetoActionCloseHandler handler && !handler.isClosable(project)) {
                return false;
            }
        }
        return true;
    }

    static boolean isClosable(FileEditorWindow window, VirtualFile file, Project project) {
        return isClosable(window.findFileComposite(file), project);
    }

    boolean isClosable(Project project);
}
