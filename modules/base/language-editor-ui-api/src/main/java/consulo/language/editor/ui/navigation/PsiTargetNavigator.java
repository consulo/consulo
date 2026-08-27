// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;

import java.util.function.Consumer;

/**
 * Builds and shows a popup of navigation targets, or navigates straight to the single target.
 * <p>
 * Targets are collected and their presentations built off the UI thread, so every terminal method
 * returns immediately and completes later. Obtained from {@link PsiTargetNavigationService}.
 */
public interface PsiTargetNavigator<T extends PsiElement> {
    PsiTargetNavigator<T> presentationProvider(TargetPresentationProvider<? super T> provider);

    /**
     * When left unset and an {@link #updater(TargetUpdaterTask)} is present, the updater's caption for the
     * collected size is used instead.
     */
    PsiTargetNavigator<T> title(LocalizeValue title);

    /**
     * Enables the pin action, which moves the targets into a Find Usages tab under this title.
     */
    PsiTargetNavigator<T> findUsagesTitle(LocalizeValue title);

    /**
     * Shown as an error hint at the invocation point when the collection ends up empty, which otherwise
     * shows nothing at all.
     */
    PsiTargetNavigator<T> emptyText(LocalizeValue emptyText);

    /**
     * Feeds targets into the popup after it is shown, for searches that stream their results.
     */
    PsiTargetNavigator<T> updater(TargetUpdaterTask<T> updater);

    PsiTargetNavigator<T> selection(T element);

    PsiTargetNavigator<T> processor(PsiElementProcessor<T> processor);

    /**
     * Returns as soon as the collection is handed off - targets and their presentations are built off the
     * ui thread, and the popup is shown once they are ready.
     */
    @RequiredUIAccess
    void navigate(ComponentEvent<?> event, Project project);

    @RequiredUIAccess
    void navigate(Editor editor, Project project);

    @RequiredUIAccess
    void createPopup(Project project, Consumer<ListPopup> consumer);
}
