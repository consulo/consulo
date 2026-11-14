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
package consulo.ai.impl.internal.chat;

import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;

/**
 * Text an agent wrote, for a label or a button - an underscore or an ampersand in it is text, not a mnemonic.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIChatTexts {
    static LocalizeValue plain(String text) {
        return LocalizeValue.of(escape(text));
    }

    static String escape(String text) {
        return StringUtil.escapeMnemonics(text);
    }

    private AIChatTexts() {
    }
}
