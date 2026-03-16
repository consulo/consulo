package consulo.ide.impl.idea.model.search.impl;


// from kotlin
public abstract class InjectionInfo {
  private InjectionInfo() {
  }

  public static class NoInjection extends InjectionInfo {
    public static NoInjection INSTANCE = new NoInjection();

    private NoInjection() {
    }
  }

  public static class IncludeInjections extends InjectionInfo {
    public static IncludeInjections INSTANCE = new IncludeInjections();

    private IncludeInjections() {
    }
  }

  public static class InInjection extends InjectionInfo {
    private LanguageInfo languageInfo;

    public InInjection(LanguageInfo languageInfo) {
      super();
      this.languageInfo = languageInfo;
    }

    
    public final LanguageInfo getLanguageInfo() {
      return languageInfo;
    }
  }
}
