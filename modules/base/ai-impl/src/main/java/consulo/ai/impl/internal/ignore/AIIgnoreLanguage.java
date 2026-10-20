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
package consulo.ai.impl.internal.ignore;

import consulo.language.ignore.IgnoreFileType;
import consulo.language.ignore.IgnoreLanguage;

/**
 * {@code .aiignore} - files and directories the AI tools must not read.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public final class AIIgnoreLanguage extends IgnoreLanguage {
    public static final AIIgnoreLanguage INSTANCE = new AIIgnoreLanguage();

    private AIIgnoreLanguage() {
        super("AIIgnore", "aiignore");
    }

    @Override
    public IgnoreFileType getFileType() {
        return AIIgnoreFileType.INSTANCE;
    }
}
