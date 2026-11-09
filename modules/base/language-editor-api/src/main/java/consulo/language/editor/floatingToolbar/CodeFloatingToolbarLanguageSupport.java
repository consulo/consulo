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
package consulo.language.editor.floatingToolbar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarActions;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import org.jspecify.annotations.Nullable;

/**
 * Extension point to configure and enable floating code toolbar for the specific language.
 *
 * @author VISTALL
 * @since 2026-11-09
 * @see consulo.codeEditor.toolbar.floating.CodeFloatingToolbar
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CodeFloatingToolbarLanguageSupport extends LanguageExtension {
    ExtensionPointCacheKey<CodeFloatingToolbarLanguageSupport, ByLanguageValue<CodeFloatingToolbarLanguageSupport>> KEY =
        ExtensionPointCacheKey.create("CodeFloatingToolbarLanguageSupport", LanguageOneToOne.build());

    static @Nullable CodeFloatingToolbarLanguageSupport forLanguage(Language language) {
        return Application.get().getExtensionPoint(CodeFloatingToolbarLanguageSupport.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * A minimal toolbar has no refactoring dropdowns.
     */
    default boolean isMinimal() {
        return false;
    }

    default boolean isIntentionsGroupHidden() {
        return false;
    }

    default boolean isConfigurationsGroupHidden() {
        return false;
    }

    default boolean isSelectionRequired() {
        return true;
    }

    /**
     * @return id of the action group to be shown in the toolbar
     */
    default String getActionGroupId() {
        return CodeFloatingToolbarActions.GROUP;
    }

    default boolean isToolbarAvailable() {
        return true;
    }
}
