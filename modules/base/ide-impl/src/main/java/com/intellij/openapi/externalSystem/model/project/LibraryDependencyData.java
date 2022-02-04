package com.intellij.openapi.externalSystem.model.project;

import javax.annotation.Nonnull;

/**
 * Not thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 8/10/11 6:46 PM
 */
public class LibraryDependencyData extends AbstractDependencyData<LibraryData> implements Named {
  
  @Nonnull
  private final LibraryLevel myLevel;
  
  public LibraryDependencyData(@Nonnull ModuleData ownerModule, @Nonnull LibraryData library, @Nonnull LibraryLevel level) {
    super(ownerModule, library);
    myLevel = level;
  }

  @Nonnull
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
