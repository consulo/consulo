// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.index.io.KeyDescriptor;
import consulo.language.psi.PsiElement;

import javax.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface StubIndexExtension<Key, Psi extends PsiElement> {
  ExtensionPointName<StubIndexExtension> EP_NAME = ExtensionPointName.create(StubIndexExtension.class);

  @Nonnull
  StubIndexKey<Key, Psi> getKey();

  int getVersion();

  @Nonnull
  KeyDescriptor<Key> getKeyDescriptor();

  int getCacheSize();
}