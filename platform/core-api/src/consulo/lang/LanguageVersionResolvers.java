package consulo.lang;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;

/**
 * @author VISTALL
 * @since 18:04/30.05.13
 */
public class LanguageVersionResolvers extends LanguageExtension<LanguageVersionResolver<? extends Language>> {
  public static final LanguageVersionResolvers INSTANCE = new LanguageVersionResolvers();

  private LanguageVersionResolvers() {
    super("com.intellij.lang.versionResolver", LanguageVersionResolver.DEFAULT);
  }
}
