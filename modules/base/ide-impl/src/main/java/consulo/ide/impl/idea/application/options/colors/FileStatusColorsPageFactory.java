/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.application.options.colors;

import consulo.application.localize.ApplicationLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusFactory;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FileStatusColorsPageFactory implements ColorAndFontPanelFactory {
    @Nonnull
    @Override
    @RequiredUIAccess
    public NewColorAndFontPanel createPanel(@Nonnull ColorAndFontOptions options) {
        return NewColorAndFontPanel.create(
            new PreviewPanel.Empty(),
            ApplicationLocalize.titleFileStatus(),
            options,
            collectFileTypes(),
            null
        );
    }

    @Nonnull
    @Override
    public LocalizeValue getPanelDisplayName() {
        return ApplicationLocalize.titleFileStatus();
    }

    private static Collection<String> collectFileTypes() {
        List<String> result = new ArrayList<>();
        FileStatus[] statuses = FileStatusFactory.getInstance().getAllFileStatuses();

        for (FileStatus status : statuses) {
            result.add(status.getText().get());
        }
        return result;
    }
}
