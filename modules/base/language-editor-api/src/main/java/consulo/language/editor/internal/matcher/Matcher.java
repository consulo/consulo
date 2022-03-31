package consulo.language.editor.internal.matcher;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface Matcher extends NameUtil.Matcher {
  boolean matches(@Nonnull String name);
}
