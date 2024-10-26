/*
 * Copyright 2013-2024 consulo.io
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
import consulo.language.editor.template.context.TemplateContextType;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;

import java.io.Closeable;

/**
 * @author VISTALL
 * @since 2024-09-15
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface LiveTemplateContributor {
    interface Factory {
        Builder newBuilder(String id,
                           String abbreviation,
                           String value,
                           @Nonnull LocalizeValue description);
    }

    interface Builder extends Closeable {
        @Nonnull
        Builder withVariable(String name, String expression, String defaultValue, boolean alwaysStopAt);

        @Nonnull
        Builder withReformat();

        @Nonnull
        Builder withTabShortcut();

        @Nonnull
        Builder withEnterShortcut();

        @Nonnull
        Builder withSpaceShortcut();

        @Nonnull
        Builder withOption(@Nonnull KeyWithDefaultValue<Boolean> key, boolean value);

        @Nonnull
        default Builder withContext(Class<? extends TemplateContextType> context) {
            return withContext(context, true);
        }

        @Nonnull
        Builder withContext(Class<? extends TemplateContextType> context, boolean enabled);

        @Nonnull
        default Builder withContextsOf(Class<? extends TemplateContextType> context) {
            return withContextsOf(context, true);
        }

        @Nonnull
        Builder withContextsOf(Class<? extends TemplateContextType> context, boolean enabled);

        void close();
    }

    void contribute(@Nonnull Factory factory);

    @Nonnull
    String groupId();

    @Nonnull
    LocalizeValue groupName();
}
