/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.spellchecker.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-03-26
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface SpellcheckerEngineManager {
    /**
     * @return id of the engine chosen by the user, or {@code null} for automatic selection
     */
    @Nullable String getSelectedEngineId();

    void setSelectedEngineId(@Nullable String engineId);

    /**
     * @return the selected engine, or {@link #getAutoSelectedEngine()} when the selection is automatic or the selected engine is not installed
     */
    @Nullable SpellcheckerEngine getActiveEngine();

    /**
     * @return the engine with the most features, then the most locales; on a tie the first registered one
     */
    @Nullable SpellcheckerEngine getAutoSelectedEngine();
}
