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

import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.platform.base.icon.PlatformIconGroup;

/**
 * The view types the platform ships. A language supplies these from its model rather than declaring its
 * own, so the toolbar toggles, the tab titles and the persisted state agree across languages. Declaring a
 * new one is allowed where a language genuinely shows something these do not cover.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public final class StandardHierarchyViewTypes {
    public static final HierarchyViewType CLASS = HierarchyViewType.builder("class")
        .presentableName(LanguageEditorLocalize.actionViewClassHierarchy())
        .description(LanguageEditorLocalize.actionDescriptionViewClassHierarchy())
        .icon(PlatformIconGroup.hierarchyClasshierarchy())
        .contentTitle(LanguageEditorLocalize::titleHierarchyClass)
        .build();

    public static final HierarchyViewType SUPERTYPES = HierarchyViewType.builder("supertypes")
        .presentableName(LanguageEditorLocalize.actionViewSupertypesHierarchy())
        .description(LanguageEditorLocalize.actionDescriptionViewSupertypesHierarchy())
        .icon(PlatformIconGroup.hierarchySupertypes())
        .contentTitle(LanguageEditorLocalize::titleHierarchySupertypes)
        .build();

    public static final HierarchyViewType SUBTYPES = HierarchyViewType.builder("subtypes")
        .presentableName(LanguageEditorLocalize.actionViewSubtypesHierarchy())
        .description(LanguageEditorLocalize.actionDescriptionViewSubtypesHierarchy())
        .icon(PlatformIconGroup.hierarchySubtypes())
        .contentTitle(LanguageEditorLocalize::titleHierarchySubtypes)
        .build();

    public static final HierarchyViewType METHOD = HierarchyViewType.builder("method")
        .presentableName(LanguageEditorLocalize.actionBrowseMethodHierarchy())
        .contentTitle(LanguageEditorLocalize::titleHierarchyMethod)
        .build();

    public static final HierarchyViewType CALLER = HierarchyViewType.builder("callers")
        .presentableName(LanguageEditorLocalize.actionCallerMethodsHierarchy())
        .icon(PlatformIconGroup.hierarchySupertypes())
        .contentTitle(LanguageEditorLocalize::titleHierarchyCallersOf)
        .build();

    public static final HierarchyViewType CALLEE = HierarchyViewType.builder("callees")
        .presentableName(LanguageEditorLocalize.actionCalleeMethodsHierarchy())
        .icon(PlatformIconGroup.hierarchySubtypes())
        .contentTitle(LanguageEditorLocalize::titleHierarchyCalleesOf)
        .build();

    private StandardHierarchyViewTypes() {
    }
}
