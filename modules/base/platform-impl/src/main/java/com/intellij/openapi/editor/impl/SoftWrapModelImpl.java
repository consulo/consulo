// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.ex.SoftWrapModelEx;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapPainter;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapsStorage;
import com.intellij.openapi.editor.impl.softwrap.mapping.CachingSoftWrapDataMapper;
import com.intellij.openapi.editor.impl.softwrap.mapping.DesktopSoftWrapApplianceManager;
import consulo.annotation.DeprecationInfo;
import consulo.editor.impl.CodeEditorBase;
import consulo.editor.impl.CodeEditorSoftWrapModelBase;
import consulo.editor.impl.softwrap.mapping.SoftWrapApplianceManager;

import javax.annotation.Nonnull;

/**
 * Default {@link SoftWrapModelEx} implementation.
 * <p/>
 * Works as a mix of {@code GoF Facade and Bridge}, i.e. delegates the processing to the target sub-components and provides
 * utility methods built on top of sub-components API.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 */
@Deprecated
@DeprecationInfo("Desktop only")
@SuppressWarnings("deprecation")
public class SoftWrapModelImpl extends CodeEditorSoftWrapModelBase {
  SoftWrapModelImpl(@Nonnull CodeEditorBase editor) {
    super(editor);
  }

  @Override
  protected SoftWrapApplianceManager createSoftWrapApplianceManager(@Nonnull SoftWrapsStorage storage,
                                                                    @Nonnull CodeEditorBase editor,
                                                                    @Nonnull SoftWrapPainter painter,
                                                                    CachingSoftWrapDataMapper dataMapper) {
    return new DesktopSoftWrapApplianceManager(storage, editor, painter, dataMapper);
  }
}
