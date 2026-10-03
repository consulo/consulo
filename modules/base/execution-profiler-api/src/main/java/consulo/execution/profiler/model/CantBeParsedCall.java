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
package consulo.execution.profiler.model;

import consulo.execution.profiler.BaseCallStackElement;
import org.jspecify.annotations.Nullable;

/**
 * A frame whose text could not be parsed, kept as written.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CantBeParsedCall extends BaseCallStackElement {
    private final String myText;

    public CantBeParsedCall(String text) {
        myText = text;
    }

    public String getText() {
        return myText;
    }

    @Override
    public String fullName() {
        return myText;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof CantBeParsedCall that && myText.equals(that.myText);
    }

    @Override
    public int hashCode() {
        return myText.hashCode();
    }
}
