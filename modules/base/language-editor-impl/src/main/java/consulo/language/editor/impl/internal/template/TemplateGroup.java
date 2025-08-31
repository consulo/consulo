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

package consulo.language.editor.impl.internal.template;

import consulo.component.persist.scheme.CompoundScheme;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class TemplateGroup extends CompoundScheme<TemplateImpl> {

    private final String myReplace;

    public TemplateGroup(String name) {
        this(name, null);
    }

    public TemplateGroup(String name, @Nullable String replace) {
        super(name);
        myReplace = replace;
    }

    public String getReplace() {
        return myReplace;
    }

    @Nonnull
    @Override
    protected CompoundScheme<TemplateImpl> createNewInstance(String name) {
        return new TemplateGroup(name);
    }
}
