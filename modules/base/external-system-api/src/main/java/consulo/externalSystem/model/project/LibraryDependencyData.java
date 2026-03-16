package consulo.externalSystem.model.project;

import consulo.externalSystem.service.project.Named;


/**
 * Not thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 8/10/11 6:46 PM
 */
public class LibraryDependencyData extends AbstractDependencyData<LibraryData> implements Named {
  
  
  private final LibraryLevel myLevel;
  
  public LibraryDependencyData(ModuleData ownerModule, LibraryData library, LibraryLevel level) {
    super(ownerModule, library);
    myLevel = level;
  }

  
  public LibraryLevel getLevel() {
    return myLevel;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myLevel.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    return myLevel.equals(((LibraryDependencyData)o).myLevel);
  }
}
