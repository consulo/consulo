package consulo.application.util.matcher;

import javax.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public interface Matcher {
  boolean matches(@Nonnull String name);
}
