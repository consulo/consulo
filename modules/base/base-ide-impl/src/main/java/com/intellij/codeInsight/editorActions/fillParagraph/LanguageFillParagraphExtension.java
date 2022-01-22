package com.intellij.codeInsight.editorActions.fillParagraph;

import consulo.language.LanguageExtension;
import consulo.container.plugin.PluginIds;

/**
 * User : ktisha
 */
public class LanguageFillParagraphExtension extends LanguageExtension<ParagraphFillHandler> {

  public static final LanguageFillParagraphExtension INSTANCE = new LanguageFillParagraphExtension();

  public LanguageFillParagraphExtension() {
    super(PluginIds.CONSULO_BASE + ".codeInsight.fillParagraph", new ParagraphFillHandler());
  }
}
