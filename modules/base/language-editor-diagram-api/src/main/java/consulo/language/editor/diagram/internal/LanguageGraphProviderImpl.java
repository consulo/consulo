/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.diagram.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.diagram.GraphBuilder;
import consulo.diagram.GraphProvider;
import consulo.language.editor.diagram.LanguageGraphProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@SuppressWarnings({"ExtensionImplIsNotAnnotated", "unchecked"})
public class LanguageGraphProviderImpl implements GraphProvider<PsiElement> {
    private final LanguageGraphProvider myLanguageGraphProvider;

    public LanguageGraphProviderImpl(LanguageGraphProvider languageGraphProvider) {
        myLanguageGraphProvider = languageGraphProvider;
    }

    
    @Override
    public String getId() {
        return myLanguageGraphProvider.getId();
    }

    
    @Override
    public String getName(PsiElement element) {
        return myLanguageGraphProvider.getName(element);
    }

    @RequiredReadAction
    
    @Override
    public GraphBuilder createBuilder(PsiElement element) {
        return myLanguageGraphProvider.createBuilder(element);
    }

    @RequiredReadAction
    
    @Override
    public String getURL(PsiElement value) {
        return myLanguageGraphProvider.getURL(value);
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement restoreFromURL(ComponentManager project, String path) {
        return myLanguageGraphProvider.restoreFromURL((Project) project, path);
    }

    @Override
    public @Nullable PsiElement findSupportedElement(DataContext context) {
        PsiElement element = context.getData(PsiElement.KEY);
        if (element == null) {
            return null;
        }

        boolean supported = myLanguageGraphProvider.isSupported(element);
        if (supported) {
            return element;
        }

        return null;
    }
}
