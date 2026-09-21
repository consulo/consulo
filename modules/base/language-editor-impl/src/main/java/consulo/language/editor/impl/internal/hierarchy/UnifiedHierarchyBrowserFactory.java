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
package consulo.language.editor.impl.internal.hierarchy;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.hierarchy.HierarchyModel;
import consulo.language.editor.internal.hierarchy.HierarchyBrowser;
import consulo.language.editor.internal.hierarchy.HierarchyBrowserFactory;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-09-19
 */
@Singleton
@ServiceImpl
public class UnifiedHierarchyBrowserFactory implements HierarchyBrowserFactory {
    @Override
    @RequiredUIAccess
    public @Nullable HierarchyBrowser createBrowser(Project project, HierarchyModel<? extends PsiElement> model) {
        return null;
    }
}
