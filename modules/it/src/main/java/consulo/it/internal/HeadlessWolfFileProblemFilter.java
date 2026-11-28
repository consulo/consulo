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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.wolfAnalyzer.WolfFileProblemFilter;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Problem file filter used by the problem solver integration tests: files whose name starts with {@code headless-wolf} are highlighted.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessWolfFileProblemFilter implements WolfFileProblemFilter {
    public static final String PREFIX = "headless-wolf";

    @Override
    public boolean isToBeHighlighted(VirtualFile file) {
        return file.getName().startsWith(PREFIX);
    }
}
