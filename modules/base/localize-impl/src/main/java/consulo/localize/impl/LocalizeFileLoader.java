package consulo.localize.impl;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeFileLoader {
  private String myLocaleId;
  private String myFilePath;
  private ClassLoader myClassLoader;

  public LocalizeFileLoader(String localeId, String filePath, ClassLoader classLoader) {
    myLocaleId = localeId;
    myFilePath = filePath;
    myClassLoader = classLoader;
  }
}
