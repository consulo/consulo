/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.inject.impl.internal;

import consulo.language.inject.MultiHostRegistrar;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.Language;


/**
 * stores injection registration info temporarily
 * from the {@link MultiHostRegistrar#startInjecting(Language)} call
 * to the moment of {@link MultiHostRegistrar#doneInjecting()}
 */
class PlaceInfo {
  
  final String prefix;
  
  final String suffix;
  
  final PsiLanguageInjectionHost host;
  
  final TextRange registeredRangeInsideHost;
  
  final LiteralTextEscaper<? extends PsiLanguageInjectionHost> myEscaper;
  
  final String myHostText;
  ProperTextRange rangeInDecodedPSI;
  TextRange rangeInHostElement;
  Segment newInjectionHostRange;

  PlaceInfo(String prefix, String suffix, PsiLanguageInjectionHost host, TextRange registeredRangeInsideHost) {
    this.prefix = prefix;
    this.suffix = suffix;
    this.host = host;
    this.registeredRangeInsideHost = registeredRangeInsideHost;
    myEscaper = host.createLiteralTextEscaper();
    myHostText = host.getText();
  }

  @Override
  public String toString() {
    return "Shred " +
           (prefix.isEmpty() ? "" : "prefix='" + prefix + "' ") +
           (suffix.isEmpty() ? "" : "suffix='" + suffix + "' ") +
           "in " + host + " with range " + host.getTextRange() + " (" + host.getClass() + ") " +
           "inside range " + registeredRangeInsideHost;
  }

  TextRange getRelevantRangeInsideHost() {
    return myEscaper.getRelevantTextRange().intersection(registeredRangeInsideHost);
  }
}
