// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui.search;

import com.intellij.openapi.extensions.ExtensionPointName;

import javax.annotation.Nonnull;

/**
 * An extension allowing plugins to provide the data at runtime for the setting search to work on.
 *
 * @author peter
 */
public abstract class SearchableOptionContributor {
  public static final ExtensionPointName<SearchableOptionContributor> EP_NAME = ExtensionPointName.create("com.intellij.search.optionContributor");

  public abstract void processOptions(@Nonnull SearchableOptionProcessor processor);
}
