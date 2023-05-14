package consulo.application.util.matcher;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public interface Matcher {
  boolean matches(@Nonnull String name);
}
