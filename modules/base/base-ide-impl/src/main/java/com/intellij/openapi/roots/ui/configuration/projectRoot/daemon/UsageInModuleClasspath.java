package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.util.text.StringUtil;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class UsageInModuleClasspath extends ProjectStructureElementUsage {
  private final ModulesConfigurator myModulesConfigurator;
  private final ModuleProjectStructureElement myContainingElement;
  @Nullable
  private final DependencyScope myScope;
  private final ProjectStructureElement mySourceElement;
  private final Module myModule;

  public UsageInModuleClasspath(@Nonnull ModulesConfigurator modulesConfigurator,
                                @Nonnull ModuleProjectStructureElement containingElement,
                                ProjectStructureElement sourceElement,
                                @Nullable DependencyScope scope) {
    myModulesConfigurator = modulesConfigurator;
    myContainingElement = containingElement;
    myScope = scope;
    myModule = containingElement.getModule();
    mySourceElement = sourceElement;
  }


  @Override
  public ProjectStructureElement getSourceElement() {
    return mySourceElement;
  }

  @Override
  public ModuleProjectStructureElement getContainingElement() {
    return myContainingElement;
  }

  public Module getModule() {
    return myModule;
  }

  @Override
  public String getPresentableName() {
    return myModule.getName();
  }

  @Override
  public PlaceInProjectStructure getPlace() {
    return new PlaceInModuleClasspath(myModulesConfigurator, myModule, myContainingElement, mySourceElement);
  }

  @Override
  public int hashCode() {
    return myModule.hashCode()*31 + mySourceElement.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof UsageInModuleClasspath && myModule.equals(((UsageInModuleClasspath)obj).myModule)
          && mySourceElement.equals(((UsageInModuleClasspath)obj).mySourceElement);
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Module;
  }

  @Override
  public void removeSourceElement() {
    if (mySourceElement instanceof LibraryProjectStructureElement) {
      //ModuleStructureConfigurable.getInstance(myModule.getProject())
      //  .removeLibraryOrderEntry(myModule, ((LibraryProjectStructureElement)mySourceElement).getLibrary());
    }
  }

  @Nullable
  @Override
  public String getPresentableLocationInElement() {
    return myScope != null && myScope != DependencyScope.COMPILE ? "[" + StringUtil.decapitalize(myScope.getDisplayName()) + "]" : null;
  }
}
