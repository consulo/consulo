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
package consulo.language.editor.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.stub.IndexOption;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * A context object produced by a {@link NavigationContextCollector} that the platform can present: after a navigation the
 * target editor carries the collected contexts under {@link NavigationContexts#NAVIGATION_CONTEXTS}, and an editor
 * notification shows the first effective one together with a way to reopen the file without any context.
 */
public interface NavigationContext {
    VirtualFile getNavigationSource();

    LocalizeValue getPresentableText();

    /**
     * @return whether viewing {@code file} under this context differs from viewing it on its own; the notification is
     * only shown for effective contexts
     */
    @RequiredReadAction
    boolean isEffectiveFor(Project project, VirtualFile file);

    /**
     * The module-aware options, by provider id, {@code file} should be viewed under when reached with this context,
     * or {@code null} when the context does not switch the file's variant.
     */
    @RequiredReadAction
    default @Nullable Map<String, IndexOption> getViewOptions(Project project, VirtualFile file) {
        return null;
    }
}
