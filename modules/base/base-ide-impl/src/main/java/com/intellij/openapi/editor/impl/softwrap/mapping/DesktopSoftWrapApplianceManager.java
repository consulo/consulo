// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl.softwrap.mapping;

import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.impl.SoftWrapEngine;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapPainter;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapsStorage;
import com.intellij.openapi.util.registry.Registry;
import consulo.annotation.DeprecationInfo;
import consulo.editor.impl.CodeEditorBase;
import consulo.editor.impl.softwrap.mapping.SoftWrapApplianceManager;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

/**
 * The general idea of soft wraps processing is to build a cache to use for quick document dimensions mapping
 * ({@code 'logical position -> visual position'}, {@code 'offset -> logical position'} etc) and update it incrementally
 * on events like document modification fold region(s) expanding/collapsing etc.
 * <p/>
 * This class encapsulates document parsing logic. It notifies {@link SoftWrapAwareDocumentParsingListener registered listeners}
 * about parsing and they are free to store necessary information for further usage.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 */
@Deprecated
@DeprecationInfo("desktop only")
@SuppressWarnings("deprecation")
public class DesktopSoftWrapApplianceManager extends SoftWrapApplianceManager {

  private static final Logger LOG = Logger.getInstance(DesktopSoftWrapApplianceManager.class);

  public DesktopSoftWrapApplianceManager(@Nonnull SoftWrapsStorage storage, @Nonnull CodeEditorBase editor, @Nonnull SoftWrapPainter painter, CachingSoftWrapDataMapper dataMapper) {
    super(storage, editor, painter, dataMapper);
  }

  @Override
  protected void doRecalculateSoftWraps0(@Nonnull IncrementalCacheUpdateEvent event, int endOffsetUpperEstimate) {
    if (myVisibleAreaWidth == QUICK_DUMMY_WRAPPING) {
      doRecalculateSoftWrapsRoughly(event);
    }
    else if (Registry.is("editor.old.soft.wrap.logic") && !IGNORE_OLD_SOFT_WRAP_LOGIC_REGISTRY_OPTION.isIn(myEditor)) {
      doRecalculateSoftWraps(event, endOffsetUpperEstimate);
    }
    else {
      new SoftWrapEngine((DesktopEditorImpl)myEditor, myPainter, myStorage, myDataMapper, event, myVisibleAreaWidth, myCustomIndentUsedLastTime ? myCustomIndentValueUsedLastTime : -1).generate();
    }
  }
}
