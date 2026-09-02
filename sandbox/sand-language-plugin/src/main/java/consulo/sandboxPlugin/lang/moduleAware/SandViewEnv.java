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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.navigation.NavigationContexts;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Set;

/**
 * The single flag environment an open file is <b>viewed</b> under — as opposed to
 * {@link SandFlagEnv#allContexts}, which is every environment the file exists in. The index
 * always carries all variants; this environment only drives presentation: inactive-branch
 * dimming, inspection scoping and caret-usage suppression. A navigation-stamped
 * {@link SandViewContext} on an open editor of the file wins; without one the file shows its
 * standalone view, the module's flag set.
 */
public final class SandViewEnv {
    private SandViewEnv() {
    }

    public static Set<String> viewEnv(Project project, VirtualFile file) {
        for (FileEditor fileEditor : FileEditorManager.getInstance(project).getEditors(file)) {
            SandViewContext context =
                NavigationContexts.findContext(fileEditor.getUserData(NavigationContexts.NAVIGATION_CONTEXTS), SandViewContext.class);
            if (context != null) {
                return context.environment();
            }
        }
        return SandFlagEnv.moduleFlags(project, file);
    }
}
