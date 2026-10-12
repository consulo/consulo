/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.codeEditor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Handles files dropped onto an editor or the editor area.
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface FileDropHandler {
    /**
     * @return true when the drop is handled and must not reach the remaining handlers, nor be opened as files
     */
    @RequiredUIAccess
    boolean handleDrop(FileDropEvent event);
}
