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
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.language.editor.codeVision.CodeVisionHost;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Code vision lenses are collected by {@link CodeVisionPass}, so an invalidation resets the modification stamp of the
 * editors and restarts the daemon.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@ServiceImpl
@Singleton
public class CodeVisionHostImpl implements CodeVisionHost {
    private final Project myProject;

    @Inject
    public CodeVisionHostImpl(Project project) {
        myProject = project;
    }

    @Override
    public void invalidateProvider(LensInvalidateSignal signal) {
        Editor editor = signal.editor();
        if (editor != null) {
            CodeVisionPassFactory.resetModificationStamp(editor);
        }
        else {
            for (Editor each : EditorFactory.getInstance().getAllEditors()) {
                if (each.getProject() == myProject) {
                    CodeVisionPassFactory.resetModificationStamp(each);
                }
            }
        }
        DaemonCodeAnalyzerInternal.getInstanceEx(myProject).restart("CodeVisionHost.invalidateProvider");
    }
}
