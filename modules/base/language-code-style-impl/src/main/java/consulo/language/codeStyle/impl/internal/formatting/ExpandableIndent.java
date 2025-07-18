/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.codeStyle.impl.internal.formatting;

import consulo.language.codeStyle.internal.IndentImpl;
import jakarta.annotation.Nonnull;

public class ExpandableIndent extends IndentImpl {
    private boolean myEnforceIndent;

    public ExpandableIndent(@Nonnull Type type) {
        this(type, false);
    }

    public ExpandableIndent(@Nonnull Type type, boolean relativeToDirectParent) {
        super(type, false, 0, relativeToDirectParent, true);
        myEnforceIndent = false;
    }

    @Override
    public boolean isEnforceIndentToChildren() {
        return myEnforceIndent;
    }

    void enforceIndent() {
        myEnforceIndent = true;
    }

    @Override
    public String toString() {
        return "SmartIndent (" + getType() + ")";
    }
}
