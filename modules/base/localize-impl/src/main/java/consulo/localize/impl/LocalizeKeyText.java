/*
 * Copyright 2013-2020 consulo.io
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
package consulo.localize.impl;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-05-20
 * <p>
 * TODO [VISTALL] condition support
 */
public class LocalizeKeyText implements Supplier<String> {
    private final String myText;

    public LocalizeKeyText(String text) {
        myText = text;
    }

    public String getText() {
        return myText;
    }

    @Override
    public String get() {
        return myText;
    }
}
