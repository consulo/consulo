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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.language.codeStyle.internal.CodeStyleInternalHelper;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.AWTConstants;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-11-05
 */
@ServiceImpl
@Singleton
public class HeadlessCodeStyleInternalHelper implements CodeStyleInternalHelper {
    @Override
    public int nextTabStop(int x, Object editor, int tabSize) {
        return 0;
    }

    @Override
    public int nextTabStop(int x, Object editor) {
        return 0;
    }

    @Override
    public int charWidth(char c, @AWTConstants.FontStyle int fontType, Object editor) {
        return 0;
    }

    @Override
    public int getSpaceWidth(@AWTConstants.FontStyle int fontType, Object editor) {
        return 0;
    }

    @RequiredUIAccess
    @Override
    public void showDetectIndentSettings(Project project) {

    }
}
