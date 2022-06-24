package consulo.language.editor.inspection;

import consulo.language.OldLanguageExtension;
import consulo.container.plugin.PluginIds;

public class LanguageInspectionSuppressors extends OldLanguageExtension<InspectionSuppressor> {
  public static final LanguageInspectionSuppressors INSTANCE = new LanguageInspectionSuppressors();

  private LanguageInspectionSuppressors() {
    super(PluginIds.CONSULO_BASE + ".lang.inspectionSuppressor");
  }

}