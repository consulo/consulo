package com.intellij.codeInspection;

import consulo.language.LanguageExtension;
import consulo.container.plugin.PluginIds;

public class LanguageInspectionSuppressors extends LanguageExtension<InspectionSuppressor> {
  public static final LanguageInspectionSuppressors INSTANCE = new LanguageInspectionSuppressors();

  private LanguageInspectionSuppressors() {
    super(PluginIds.CONSULO_BASE + ".lang.inspectionSuppressor");
  }

}