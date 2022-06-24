package consulo.ide.impl.idea.codeInsight.editorActions.fillParagraph;

import consulo.language.OldLanguageExtension;
import consulo.container.plugin.PluginIds;

/**
 * User : ktisha
 */
public class LanguageFillParagraphExtension extends OldLanguageExtension<ParagraphFillHandler> {

  public static final LanguageFillParagraphExtension INSTANCE = new LanguageFillParagraphExtension();

  public LanguageFillParagraphExtension() {
    super(PluginIds.CONSULO_BASE + ".codeInsight.fillParagraph", new ParagraphFillHandler());
  }
}
