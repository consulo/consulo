/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeStyle;

import consulo.annotation.component.ActionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.ide.impl.idea.util.LineSeparator;

/**
 * @author Nikolai Matveev
 */
@ActionImpl(id = "ConvertToWindowsLineSeparators")
public class ConvertToWindowsLineSeparatorsAction extends AbstractConvertLineSeparatorsAction {
    public ConvertToWindowsLineSeparatorsAction() {
        super(ApplicationLocalize.comboboxCrlfWindows(), LineSeparator.CRLF);
    }
}
