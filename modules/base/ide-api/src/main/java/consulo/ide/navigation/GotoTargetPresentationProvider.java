// Copyright 2000-2009 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.

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

package consulo.ide.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;
import consulo.navigation.TargetPresentation;
import org.jspecify.annotations.Nullable;

/**
 * Describes how a language presents its own elements in a goto-target popup. Called once per target
 * before the popup opens, under a read lock and off the ui thread, so what comes back has to be a
 * finished presentation rather than something the row asks questions of later.
 *
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface GotoTargetPresentationProvider {
    /**
     * What the popup as a whole looks like, which a single target cannot tell by itself - a list of
     * same-named methods needs their containers spelled out, a list of differently named ones does not.
     */
    interface Options {
        boolean hasDifferentNames();
    }

    @RequiredReadAction
    @Nullable
    TargetPresentation getPresentation(PsiElement element, Options options);
}
