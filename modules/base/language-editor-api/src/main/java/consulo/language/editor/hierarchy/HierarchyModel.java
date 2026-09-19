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
package consulo.language.editor.hierarchy;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataSink;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a language knows about one hierarchy it has been asked to show: which views it offers, and how to
 * build the tree behind each of them. A model holds no widgets and is never asked to draw - the platform
 * owns the view, the toolbar, the scope control and the persisted state.
 * <p>
 * Element-sensitive methods are handed the live {@link PsiElement} rather than reading {@link #getTarget()},
 * because a refresh re-resolves the target and the stored one goes stale.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public interface HierarchyModel<E extends PsiElement> {
    HierarchyKind getKind();

    E getTarget();

    List<HierarchyViewType> getViewTypes();

    @RequiredReadAction
    HierarchyViewType getDefaultViewType();

    /**
     * Lets a model send a view type somewhere else before it is shown - opening an interface on its
     * subtypes rather than on itself, say.
     */
    @RequiredReadAction
    default HierarchyViewType correctViewType(HierarchyViewType viewType) {
        return viewType;
    }

    @RequiredReadAction
    default boolean isViewTypeEnabled(HierarchyViewType viewType) {
        return true;
    }

    /**
     * Whether the platform offers its scope control at all for this hierarchy.
     */
    default boolean isScopeSupported() {
        return false;
    }

    /**
     * Whether the chosen scope means anything for one particular view - a supertype walk ignores it.
     */
    default boolean isScopeApplicable(HierarchyViewType viewType) {
        return true;
    }

    default List<HierarchyOption> getOptions() {
        return List.of();
    }

    default List<HierarchyLegendEntry> getLegend() {
        return List.of();
    }

    /**
     * Context menu group, where a language wants its own rather than the one its kind names.
     */
    default String getPopupActionGroupId() {
        return getKind().getPopupActionGroupId();
    }

    /**
     * Builds one view. Answering null means this hierarchy cannot be shown for the request.
     */
    @RequiredReadAction
    @Nullable HierarchyTreeStructure createTreeStructure(HierarchyRequest<E> request);

    @RequiredReadAction
    boolean isApplicableElement(PsiElement element);

    /**
     * Whether a node may be re-opened as the base of its own hierarchy.
     */
    @RequiredReadAction
    default boolean canBeBase(PsiElement element) {
        return true;
    }

    @RequiredReadAction
    default LocalizeValue getBaseOnThisText(PsiElement element) {
        return LocalizeValue.empty();
    }

    @RequiredReadAction
    default LocalizeValue getContentDisplayName(HierarchyViewType viewType, PsiElement element) {
        String name = element instanceof PsiNamedElement namedElement ? namedElement.getName() : null;
        return name == null ? LocalizeValue.empty() : viewType.contentTitle(name);
    }

    /**
     * Publishes language-specific data keys for the current selection. Runs on the UI thread, so capture
     * the selection eagerly and hand anything that has to resolve PSI to {@link DataSink#lazy}.
     */
    default void uiDataSnapshot(DataSink sink, List<? extends HierarchyNodeDescriptor> selection) {
    }
}
