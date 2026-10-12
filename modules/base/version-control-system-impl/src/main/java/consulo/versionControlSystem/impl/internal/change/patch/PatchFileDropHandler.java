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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.FileDropEvent;
import consulo.codeEditor.FileDropHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.impl.internal.patch.PatchFileType;

import java.io.File;
import java.util.List;

@ExtensionImpl
public class PatchFileDropHandler implements FileDropHandler {
    @Override
    @RequiredUIAccess
    public boolean handleDrop(FileDropEvent event) {
        List<File> files = event.files();
        if (files.size() != 1 || !PatchFileType.isPatchFile(files.get(0))) {
            return false;
        }
        return ApplyPatchAction.showAndGetApplyPatch(event.project(), files.get(0));
    }
}
