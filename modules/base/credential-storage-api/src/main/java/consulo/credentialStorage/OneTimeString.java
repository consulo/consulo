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
package consulo.credentialStorage;

import consulo.util.lang.CharArrayCharSequence;

/**
 * clearable only if specified explicitly.
 * <p>
 * Case
 * 1) you create OneTimeString manually on user input.
 * 2) you store it in CredentialStore
 * 3) you consume it... BUT native credentials store do not store credentials immediately - write is postponed, so, will be an critical error.
 * <p>
 * so, currently - only credentials store implementations should set this flag on get.
 *
 * TODO not done!
 */
public class OneTimeString extends CharArrayCharSequence {
    public OneTimeString(String str) {
        super(str.toCharArray());
    }
}
