// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl;

import consulo.language.LanguageExtension;
import consulo.container.plugin.PluginIds;

public final class LanguageWordBoundaryFilter extends LanguageExtension<WordBoundaryFilter> {
  public static final LanguageWordBoundaryFilter INSTANCE = new LanguageWordBoundaryFilter();

  private LanguageWordBoundaryFilter() {
    super(PluginIds.CONSULO_BASE + ".wordBoundaryFilter", new WordBoundaryFilter());
  }
}
