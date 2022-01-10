// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.util.io.KeyDescriptor;
import javax.annotation.Nonnull;

public interface StubIndexExtension<Key, Psi extends PsiElement> {
  ExtensionPointName<StubIndexExtension<?, ?>> EP_NAME = ExtensionPointName.create("com.intellij.stubIndex");

  @Nonnull
  StubIndexKey<Key, Psi> getKey();

  int getVersion();

  @Nonnull
  KeyDescriptor<Key> getKeyDescriptor();

  int getCacheSize();
}