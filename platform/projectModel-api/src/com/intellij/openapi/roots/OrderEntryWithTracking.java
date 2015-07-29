package com.intellij.openapi.roots;

import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 22.08.14
 */
public interface OrderEntryWithTracking {
  @Nullable
  Object getEqualObject();
}
