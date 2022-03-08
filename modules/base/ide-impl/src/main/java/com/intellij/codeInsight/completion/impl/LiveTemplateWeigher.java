// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.impl;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElement;
import consulo.application.util.registry.Registry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public class LiveTemplateWeigher extends LookupElementWeigher {
  public LiveTemplateWeigher() {
    super("templates", Registry.is("ide.completion.show.live.templates.on.top"), false);
  }

  @Nullable
  @Override
  public Comparable weigh(@Nonnull LookupElement element) {
    return element instanceof LiveTemplateLookupElement;
  }
}
