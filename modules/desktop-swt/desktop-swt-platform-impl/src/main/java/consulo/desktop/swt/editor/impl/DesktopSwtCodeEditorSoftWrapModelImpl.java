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
package consulo.desktop.swt.editor.impl;

import com.intellij.openapi.editor.impl.softwrap.SoftWrapPainter;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapsStorage;
import com.intellij.openapi.editor.impl.softwrap.mapping.CachingSoftWrapDataMapper;
import consulo.editor.impl.CodeEditorBase;
import consulo.editor.impl.CodeEditorSoftWrapModelBase;
import consulo.editor.impl.softwrap.mapping.SoftWrapApplianceManager;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtCodeEditorSoftWrapModelImpl extends CodeEditorSoftWrapModelBase {
  public DesktopSwtCodeEditorSoftWrapModelImpl(@Nonnull CodeEditorBase editor) {
    super(editor);
  }

  @Override
  protected SoftWrapApplianceManager createSoftWrapApplianceManager(@Nonnull SoftWrapsStorage storage,
                                                                    @Nonnull CodeEditorBase editor,
                                                                    @Nonnull SoftWrapPainter painter,
                                                                    CachingSoftWrapDataMapper dataMapper) {
    return new SoftWrapApplianceManager(storage, editor, painter, dataMapper) {
    };
  }
}
