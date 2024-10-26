/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.template;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TemplateOptionalProcessor {
    @Nonnull
    KeyWithDefaultValue<Boolean> getKey();

    void processText(final Project project, final Template template, final Document document, final RangeMarker templateRange, final Editor editor);

    @Nonnull
    LocalizeValue getOptionText();

    default boolean isEnabled(@Nonnull Template template) {
        return template.getOption(getKey());
    }

    default void setEnabled(@Nonnull Template template, boolean value) {
        template.setOption(getKey(), value);
    }

    default boolean isVisible(@Nonnull Template template) {
        return template.containsOption(getKey());
    }
}
