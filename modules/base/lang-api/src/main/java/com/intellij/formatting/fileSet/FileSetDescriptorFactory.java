// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.fileSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FileSetDescriptorFactory {

  private FileSetDescriptorFactory() {
  }

  @Nullable
  public static FileSetDescriptor createDescriptor(@Nonnull FileSetDescriptor.State state) {
    if (PatternDescriptor.PATTERN_TYPE.equals(state.type) && state.pattern != null) {
      return new PatternDescriptor(state.pattern);
    }
    if (NamedScopeDescriptor.NAMED_SCOPE_TYPE.equals(state.type) && state.name != null) {
      NamedScopeDescriptor descriptor = new NamedScopeDescriptor(state.name);
      if (state.pattern != null) {
        descriptor.setPattern(state.pattern);
      }
      return descriptor;
    }
    return null;
  }
}
