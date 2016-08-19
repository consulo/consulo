package consulo.lang;

import com.intellij.lang.LanguageExtension;

/**
 * @author VISTALL
 * @since 13:19/25.08.13
 */
public class LanguageVersionDefines extends LanguageExtension<LanguageVersion> {
  public static final LanguageVersionDefines INSTANCE = new LanguageVersionDefines();

  private LanguageVersionDefines() {
    super("com.intellij.lang.defineVersion");
  }
}
