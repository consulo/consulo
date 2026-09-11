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
package consulo.language.editor.impl.internal.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.language.editor.navigation.NavigationContext;
import consulo.language.editor.navigation.NavigationContexts;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.NotificationType;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

@ExtensionImpl
public final class NavigationContextEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
    private final Project myProject;

    @Inject
    public NavigationContextEditorNotificationProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "navigation-context";
    }

    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(VirtualFile file, FileEditor fileEditor, Supplier<EditorNotificationBuilder> builderFactory) {
        NavigationContext context = findEffectiveContext(fileEditor.getUserData(NavigationContexts.NAVIGATION_CONTEXTS), file);
        if (context == null) {
            return null;
        }
        EditorNotificationBuilder builder = builderFactory.get();
        builder.withText(LanguageEditorLocalize.navigationContextNotificationText(context.getPresentableText(), context.getNavigationSource().getName()));
        builder.withType(NotificationType.INFO);
        builder.withAction(LanguageEditorLocalize.navigationContextViewOriginal(), event -> viewOriginal(file, fileEditor));
        return builder;
    }

    @RequiredReadAction
    private @Nullable NavigationContext findEffectiveContext(@Nullable List<Object> contexts, VirtualFile file) {
        if (contexts == null) {
            return null;
        }
        for (Object candidate : contexts) {
            if (candidate instanceof NavigationContext context
                && !file.equals(context.getNavigationSource())
                && context.isEffectiveFor(myProject, file)) {
                return context;
            }
        }
        return null;
    }

    private void viewOriginal(VirtualFile file, FileEditor fileEditor) {
        int offset = fileEditor instanceof TextEditor textEditor ? textEditor.getEditor().getCaretModel().getOffset() : 0;
        ModuleAwareIndexOptions.setViewOptions(myProject, file, null);
        FileEditorManager.getInstance(myProject).closeFile(file);
        OpenFileDescriptorFactory.getInstance(myProject).newBuilder(file).offset(offset).build().navigate(true);
    }
}
