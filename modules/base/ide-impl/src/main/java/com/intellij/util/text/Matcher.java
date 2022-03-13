package com.intellij.util.text;

import consulo.ide.impl.psi.codeStyle.NameUtil;
import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Matcher extends NameUtil.Matcher {
  boolean matches(@Nonnull String name);
}
