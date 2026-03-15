package consulo.externalSystem.service.project;


/**
 * @author Denis Zhdanov
 * @since 8/12/11 12:34 PM
 */
public interface Named {

  /**
   * please use {@link #getExternalName()} or {@link #getInternalName()} instead
   */
  
  @Deprecated
  String getName();

  /**
   * please use {@link #setExternalName(String)} or {@link #setInternalName(String)} instead
   */
  @Deprecated
  void setName(String name);

  
  String getExternalName();
  void setExternalName(String name);

  
  String getInternalName();
  void setInternalName(String name);
}
