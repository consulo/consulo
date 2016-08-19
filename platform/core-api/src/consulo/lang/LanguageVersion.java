package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:59/30.05.13
 */
public interface LanguageVersion<T extends Language> {
  Key<LanguageVersion<? extends Language>> KEY = Key.create("LANGUAGE_VERSION");

  @NotNull
  @NonNls
  String getName();

  @NonNls
  T getLanguage();
}
