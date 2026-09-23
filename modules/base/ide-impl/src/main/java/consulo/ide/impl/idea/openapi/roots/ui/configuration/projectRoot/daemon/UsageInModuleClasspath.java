package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.module.Module;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.lang.StringUtil;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

/**
 * @author nik
 */
public class UsageInModuleClasspath extends ProjectStructureElementUsage {
    private final ModulesConfigurator myModulesConfigurator;
    private final ModuleProjectStructureElement myContainingElement;
    private final @Nullable DependencyScope myScope;
    private final ProjectStructureElement mySourceElement;
    private final Module myModule;

    public UsageInModuleClasspath(
        ModulesConfigurator modulesConfigurator,
        ModuleProjectStructureElement containingElement,
        ProjectStructureElement sourceElement,
        @Nullable DependencyScope scope
    ) {
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
        return myModule.hashCode() * 31 + mySourceElement.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof UsageInModuleClasspath that && myModule.equals(that.myModule) && mySourceElement.equals(that.mySourceElement);
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesModule();
    }

    @Override
    public void removeSourceElement() {
        if (mySourceElement instanceof LibraryProjectStructureElement) {
            //ModuleStructureConfigurable.getInstance(myModule.getProject())
            //  .removeLibraryOrderEntry(myModule, ((LibraryProjectStructureElement)mySourceElement).getLibrary());
        }
    }

    @Override
    public @Nullable String getPresentableLocationInElement() {
        return myScope != null && myScope != DependencyScope.COMPILE ? "[" + StringUtil.decapitalize(myScope.getDisplayName()) + "]" : null;
    }
}
