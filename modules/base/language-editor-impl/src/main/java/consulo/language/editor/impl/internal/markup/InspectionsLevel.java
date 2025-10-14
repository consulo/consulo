/*
 * Copyright 2013-2020 consulo.io
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
package consulo.language.editor.impl.internal.markup;

import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-06-19
 */
public enum InspectionsLevel {
    NONE(CodeEditorLocalize.iwLevelNone()),
    ERRORS(CodeEditorLocalize.iwLevelErrors()),
    ALL(CodeEditorLocalize.iwLevelAll());

    private final LocalizeValue myTextValue;

    InspectionsLevel(@Nonnull LocalizeValue textValue) {
        myTextValue = textValue;
    }

    @Nonnull
    public LocalizeValue getTextValue() {
        return myTextValue;
    }
}
