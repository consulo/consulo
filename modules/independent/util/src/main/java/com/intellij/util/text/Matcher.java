package com.intellij.util.text;

import com.intellij.psi.codeStyle.NameUtil;
import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Matcher extends NameUtil.Matcher {
  boolean matches(@Nonnull String name);
}
