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
package consulo.language.spellchecker.editor.impl.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import consulo.language.spellchecker.editor.SpellcheckerFeature;
import consulo.language.spellchecker.editor.SpellcheckerSeverities;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionImpl
public class StyleInspection extends TextProblemInspection {
    @Inject
    public StyleInspection(SpellcheckerEngineManager engineManager) {
        super(engineManager, SpellcheckerFeature.STYLE, SpellcheckerSeverities.STYLE_SUGGESTION);
    }

    @Override
    public String getShortName() {
        return "StyleInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return SpellCheckerLocalize.styleInspectionName();
    }

    @Override
    public LocalizeValue getDescription() {
        return SpellCheckerLocalize.styleInspectionDescription();
    }
}
