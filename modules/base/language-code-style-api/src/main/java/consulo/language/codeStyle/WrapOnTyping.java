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
package consulo.language.codeStyle;

import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.localize.LocalizeValue;

/**
* @author UNV
* @since 2026-09-17
*/
public enum WrapOnTyping {
    DEFAULT(-1, CodeStyleLocalize.wrappingWrapOnTypingDefault()) {
        @Override
        public LocalizeValue getDisplayNameFor(CodeStyleSettings settings) {
            return CodeStyleLocalize.settingsDefaultValuePrefix(
                settings.WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN
                    ? CodeStyleLocalize.settingsDefaultValueYes()
                    : CodeStyleLocalize.settingsDefaultValueNo()
            );
        }
    },
    NO_WRAP(0, CodeStyleLocalize.wrappingWrapOnTypingNoWrap()),
    WRAP(1, CodeStyleLocalize.wrappingWrapOnTypingWrap());

    private final int myValue;
    private final LocalizeValue myDisplayName;

    WrapOnTyping(int value, LocalizeValue displayName) {
        myValue = value;
        myDisplayName = displayName;
    }

    public LocalizeValue getDisplayNameFor(CodeStyleSettings settings) {
        return myDisplayName;
    }

    public boolean is(int value) {
        return myValue == value;
    }

    public int getValue() {
        return myValue;
    }

    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    public static WrapOnTyping fromValue(int value) {
        for (WrapOnTyping wrapOnTyping : WrapOnTyping.values()) {
            if (wrapOnTyping.is(value)) {
                return wrapOnTyping;
            }
        }
        return NO_WRAP;
    }
}
