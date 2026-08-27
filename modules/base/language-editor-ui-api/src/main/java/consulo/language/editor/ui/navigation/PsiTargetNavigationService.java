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
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Replaces the removed {@code PsiElementListNavigator} and the target popup part of
 * {@code PopupNavigationUtil}.
 *
 * @author VISTALL
 * @since 2026-08-27
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PsiTargetNavigationService {
    /**
     * For targets that are cheap to collect - the supplier runs inside a single read action.
     */
    <T extends PsiElement> PsiTargetNavigator<T> newNavigator(@RequiredReadAction Supplier<Collection<T>> targets);

    /**
     * For searches that are too long to hold the read lock for, such as inheritor or overriding
     * method searches. The caller owns the step and therefore its own read chunking and progress.
     */
    <T extends PsiElement> PsiTargetNavigator<T> newNavigator(CoroutineStep<Void, Collection<T>> targets);
}
