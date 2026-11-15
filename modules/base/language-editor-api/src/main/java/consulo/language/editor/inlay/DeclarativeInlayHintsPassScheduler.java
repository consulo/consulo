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
package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.project.Project;

/**
 * Reschedules the declarative inlay hints pass. The pass is skipped while the PSI modification count of the project
 * stays the same, so providers whose hints depend on other data (e.g. an external process) must reset the stamp
 * when that data changes.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DeclarativeInlayHintsPassScheduler {
    static DeclarativeInlayHintsPassScheduler getInstance() {
        return Application.get().getInstance(DeclarativeInlayHintsPassScheduler.class);
    }

    /**
     * Resets the modification stamp of the editor and restarts the daemon, so the declarative inlay hints of the
     * editor are collected again.
     */
    void scheduleRecompute(Editor editor, Project project);

    /**
     * Resets the modification stamp of the editor: the next daemon run collects its declarative inlay hints again.
     */
    void resetModificationStamp(Editor editor);

    /**
     * Resets the modification stamp of all editors.
     */
    void resetModificationStamp();
}
