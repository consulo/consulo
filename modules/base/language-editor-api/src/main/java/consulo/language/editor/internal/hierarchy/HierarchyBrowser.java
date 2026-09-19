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
package consulo.language.editor.internal.hierarchy;

import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.language.editor.hierarchy.HierarchyKind;
import consulo.language.editor.hierarchy.HierarchyModel;
import consulo.language.editor.hierarchy.HierarchyViewType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ex.content.Content;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * One open hierarchy, as everything outside the frontend sees it. The actions on a hierarchy toolbar, in
 * its context menu and in the keymap are written against this and never against the widget behind it, so a
 * frontend is free to draw a hierarchy however it likes.
 * <p>
 * Reachable from a {@link consulo.dataContext.DataContext} under {@link #KEY}, and from the
 * {@link Content} it was put into under the same key.
 * <p>
 * Not plugin-facing.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public interface HierarchyBrowser extends Disposable {
    Key<HierarchyBrowser> KEY = Key.create(HierarchyBrowser.class);

    Project getProject();

    HierarchyModel<? extends PsiElement> getModel();

    default HierarchyKind getKind() {
        return getModel().getKind();
    }

    /**
     * View currently shown, or null while nothing has been shown yet.
     */
    @Nullable
    HierarchyViewType getCurrentViewType();

    /**
     * Where the model would actually send the given view - see {@link HierarchyModel#correctViewType}.
     */
    @RequiredReadAction
    HierarchyViewType correctViewType(HierarchyViewType viewType);

    /**
     * Whether this hierarchy offers the given view at all, and offers it right now.
     */
    @RequiredReadAction
    boolean isViewTypeEnabled(HierarchyViewType viewType);

    @RequiredReadAction
    void changeViewType(HierarchyViewType viewType);

    @RequiredReadAction
    void changeToDefaultView(boolean requestFocus);

    /**
     * Element this hierarchy was opened on, or null once it has gone invalid.
     */
    @Nullable
    PsiElement getHierarchyBase();

    @Nullable
    PsiElement getSelectedElement();

    /**
     * Every element the hierarchy has actually built so far. Loads nothing further.
     */
    PsiElement[] getAvailableElements();

    @RequiredReadAction
    boolean isApplicableElement(PsiElement element);

    @RequiredReadAction
    boolean canBeBase(PsiElement element);

    @RequiredReadAction
    LocalizeValue getBaseOnThisText(PsiElement element);

    /**
     * Tab this hierarchy lives in, so that it can retitle itself as the view changes. Null for a hierarchy
     * embedded somewhere other than the hierarchy tool window.
     */
    void setContent(@Nullable Content content);

    Component getUIComponent();
}
