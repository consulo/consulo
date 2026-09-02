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
package consulo.sandboxPlugin.ide.fileEditor;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.language.editor.navigation.NavigationContexts;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.sandboxPlugin.lang.moduleAware.SandFlagEnv;
import consulo.sandboxPlugin.lang.moduleAware.SandViewContext;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.ui.NotificationType;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Shows which context the file is viewed under — but only when it matters: the arrival
 * environment differs from the file's own default context and the file actually contains
 * variant declarations. Trivial navigations stay banner-free.
 */
@ExtensionImpl
public class SandContextEditorNotificationProvider implements EditorNotificationProvider {
    private final Project myProject;

    @Inject
    public SandContextEditorNotificationProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "sand-view-context";
    }

    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(VirtualFile file, FileEditor fileEditor, Supplier<EditorNotificationBuilder> builderFactory) {
        if (file.getFileType() != SandFileType.INSTANCE) {
            return null;
        }

        SandViewContext context = NavigationContexts.findContext(fileEditor.getUserData(NavigationContexts.NAVIGATION_CONTEXTS), SandViewContext.class);
        if (context == null || file.equals(context.navigationSource())) {
            return null;
        }

        Set<String> defaultEnvironment = SandFlagEnv.moduleFlags(myProject, file);
        if (context.environment().equals(defaultEnvironment)) {
            return null;
        }

        if (!hasVariants(file)) {
            return null;
        }

        List<String> sorted = new ArrayList<>(context.environment());
        sorted.sort(null);
        String environment = sorted.isEmpty() ? "<no flags>" : String.join(", ", sorted);

        EditorNotificationBuilder builder = builderFactory.get();
        builder.withText(LocalizeValue.localizeTODO(
            "Viewing in context [" + environment + "] — navigated from " + context.navigationSource().getName()));
        builder.withType(NotificationType.INFO);
        return builder;
    }

    @RequiredReadAction
    private boolean hasVariants(VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
        if (psiFile == null) {
            return false;
        }
        return psiFile.getNode().findChildByType(SandElements.IF_DIRECTIVE) != null
            || psiFile.getNode().findChildByType(SandElements.IFNDEF_DIRECTIVE) != null;
    }
}
