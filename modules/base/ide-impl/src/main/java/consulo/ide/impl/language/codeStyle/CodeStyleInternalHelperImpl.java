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
package consulo.ide.impl.language.codeStyle;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.codeStyle.internal.CodeStyleInternalHelper;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.AWTConstants;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 30-Apr-22
 */
@Singleton
@ServiceImpl
public class CodeStyleInternalHelperImpl implements CodeStyleInternalHelper {
    @Override
    public int nextTabStop(int x, Object editor, int tabSize) {
        return EditorImplUtil.nextTabStop(x, (Editor) editor, tabSize);
    }

    @Override
    public int nextTabStop(int x, Object editor) {
        return EditorImplUtil.nextTabStop(x, (Editor) editor);
    }

    @Override
    public int charWidth(char c, @AWTConstants.FontStyle int fontType, Object editor) {
        return EditorImplUtil.charWidth(c, fontType, (Editor) editor);
    }

    @Override
    public int getSpaceWidth(@AWTConstants.FontStyle int fontType, Object editor) {
        return EditorImplUtil.getSpaceWidth(fontType, (Editor) editor);
    }

    @RequiredUIAccess
    @Override
    public void showDetectIndentSettings(@Nullable Project project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "preferences.sourceCode", "detect indent");
    }
}
