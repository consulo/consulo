/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.plugins.whatsNew;

import consulo.fileEditor.history.SkipFromDocumentHistory;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.virtualFileSystem.light.TextLightVirtualFileBase;

/**
 * @author VISTALL
 * @since 2021-11-15
 */
public class WhatsNewVirtualFile extends TextLightVirtualFileBase implements VirtualFileWithoutContent, SkipFromDocumentHistory {
    public WhatsNewVirtualFile(LocalizeValue fileNameText) {
        super(fileNameText.get().replace("_", ""), WhatsNewVirtualFileType.INSTANCE, System.currentTimeMillis());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WhatsNewVirtualFile;
    }
}
