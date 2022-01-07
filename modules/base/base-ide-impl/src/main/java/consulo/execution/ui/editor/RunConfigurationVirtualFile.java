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
package consulo.execution.ui.editor;

import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import com.intellij.openapi.vfs.VirtualFileWithoutContent;
import com.intellij.testFramework.LightVirtualFile;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationVirtualFile extends LightVirtualFile implements VirtualFileWithoutContent, IdeDocumentHistoryImpl.SkipFromDocumentHistory {
  public RunConfigurationVirtualFile() {
    super("Run Configurations", RunConfigurationFileType.INSTANCE, "");
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof RunConfigurationVirtualFile;
  }
}
