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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.io.File;

public class PatchNameChecker {
    public static final int MAX = 100;
    private static final int MAX_PATH = 255;

    public static LocalizeValue validateName(String name) {
        String fileName = new File(name).getName();
        if (StringUtil.isEmptyOrSpaces(fileName)) {
            return VcsLocalize.patchCreationEmptyNameError();
        }
        else if (name.length() > MAX_PATH) {
            return VcsLocalize.patchCreationNameTooLongError();
        }
        return LocalizeValue.empty();
    }
}
