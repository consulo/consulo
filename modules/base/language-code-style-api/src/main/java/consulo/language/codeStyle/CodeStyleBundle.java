/*
 * Copyright 2013-2022 consulo.io
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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author VISTALL
 * @since 12-Mar-22
 */
@Deprecated
@DeprecationInfo("Use CodeStyleLocalize")
@MigratedExtensionsTo(CodeStyleLocalize.class)
public class CodeStyleBundle extends AbstractBundle {
    public static final String BUNDLE = "consulo.language.codeStyle.CodeStyleBundle";

    private static final CodeStyleBundle ourInstance = new CodeStyleBundle();

    private CodeStyleBundle() {
        super(BUNDLE);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key) {
        return ourInstance.getMessage(key);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return ourInstance.getMessage(key, params);
    }
}
