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
package consulo.language.editor.ui.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiElement;
import consulo.navigation.TargetPresentation;
import consulo.navigation.TargetPresentationBuilder;

/**
 * Builds the standard presentation of a navigation target: symbol text, container in gray, the module
 * or library on the trailing edge, the file color as row background. Providers start from
 * {@link #presentationBuilder(PsiElement)} and override the parts they present differently.
 *
 * @author VISTALL
 * @since 2026-08-27
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PsiTargetPresentationFactory {
    @RequiredReadAction
    default TargetPresentation presentation(PsiElement element) {
        return presentationBuilder(element).build();
    }

    @RequiredReadAction
    TargetPresentationBuilder presentationBuilder(PsiElement element);
}
