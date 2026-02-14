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
package consulo.language.ignore;

import jakarta.annotation.Nullable;

/**
 * Available syntax list.
 */
public enum Syntax {
    GLOB,
    REGEXP;

    private static final String KEY = "syntax:";

    @Nullable
    public static Syntax find(@Nullable String name) {
        if (name == null) {
            return null;
        }
        try {
            return Syntax.valueOf(name.toUpperCase());
        }
        catch (IllegalArgumentException iae) {
            return null;
        }
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    /**
     * Returns {@link mobi.hsz.idea.gitignore.psi.IgnoreTypes#SYNTAX} element presentation.
     *
     * @return element presentation
     */
    public String getPresentation() {
        return KEY + " " + toString();
    }
}