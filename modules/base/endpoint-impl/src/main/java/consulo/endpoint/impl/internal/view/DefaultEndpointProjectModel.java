package consulo.endpoint.impl.internal.view;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointModuleEntity;
import consulo.endpoint.EndpointProjectModel;
import consulo.endpoint.ModuleEndpointFilter;
import consulo.endpoint.internal.DefaultEndpointModule;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.psi.PsiFile;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ExtensionImpl(order = "last")
public final class DefaultEndpointProjectModel implements EndpointProjectModel {
    public static final String MODULE_QUERY_TAG = "module";

    private final Project myProject;

    @Inject
    public DefaultEndpointProjectModel(Project project) {
        myProject = project;
    }

    @Override
    public LocalizeValue getModuleDisplayName() {
        return EndpointLocalize.frameworksFiltersModule();
    }

    @Override
    public String getModuleQueryTag() {
        return MODULE_QUERY_TAG;
    }

    @Override
    public LocalizeValue getSelectModulesTitle() {
        return EndpointLocalize.frameworksFiltersModuleTitle();
    }

    @Override
    public LocalizeValue getGroupByModuleTitleShort() {
        return EndpointLocalize.endpointsViewGroupByModuleShort();
    }

    @Override
    public LocalizeValue getGroupByModuleTitleFull() {
        return EndpointLocalize.endpointsViewGroupByModuleFull();
    }

    @Override
    public Collection<EndpointModuleEntity> getModuleEntities() {
        Module[] modules = ModuleManager.getInstance(myProject).getModules();
        List<EndpointModuleEntity> entities = new ArrayList<>(modules.length);
        for (Module module : modules) {
            entities.add(new DefaultEndpointModule(module));
        }
        return entities;
    }

    @Override
    public boolean isTestModule(EndpointModuleEntity entity) {
        if (!(entity instanceof DefaultEndpointModule defaultModule)) {
            return false;
        }

        ModuleRootManager rootManager = ModuleRootManager.getInstance(defaultModule.getModule());
        return rootManager.getContentFolders(LanguageContentFolderScopes.test()).length > 0
            && rootManager.getContentFolders(LanguageContentFolderScopes.production()).length == 0;
    }

    @Override
    public EndpointFilter createFilter(EndpointModuleEntity entity, boolean fromLibraries, boolean fromTests) {
        if (!(entity instanceof DefaultEndpointModule defaultModule)) {
            throw new IllegalArgumentException("Unsupported module entity: " + entity);
        }
        return new ModuleEndpointFilter(defaultModule.getModule(), fromLibraries, fromTests);
    }

    @Override
    public @Nullable EndpointModuleEntity getModuleEntityForFile(PsiFile file) {
        Module module = ModuleUtilCore.findModuleForFile(file);
        return module == null ? null : new DefaultEndpointModule(module);
    }
}
