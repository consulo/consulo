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
import consulo.fileEditor.FileEditor;
import consulo.disposer.Disposable;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentBuilder;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentProvider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * A file level highlight is shown in a panel above a visible editor, so there is nothing to show headlessly.
 * The daemon still records the highlight; only its presentation is skipped.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessFileLevelHighlightComponentProvider implements FileLevelHighlightComponentProvider {
    @Override
    public @Nullable Disposable createComponent(FileEditor fileEditor, FileLevelHighlightComponentBuilder builder) {
        return null;
    }
}
