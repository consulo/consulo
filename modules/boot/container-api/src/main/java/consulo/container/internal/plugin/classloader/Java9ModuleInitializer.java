package consulo.container.internal.plugin.classloader;

import consulo.container.internal.ContainerLogger;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

/**
 * @author VISTALL
 * @implNote Since each plugin loaded in own plugin classloader, and own module layer, different modules can't export to top module on hierarchy
 * (or from different module layer).
 * That why we hack impl method for exports, and trying add exports to module from different classloader. Dirty, but for now it's only way for it.
 * We can't create one module layer for all plugins in runtime.
 * Also we can't call some addExports from target module (due call stack check)
 * @since 2019-11-19
 */
public class Java9ModuleInitializer {
    private static Method ourAddExportsImpl;

    static {
        try {
            ourAddExportsImpl = Module.class.getDeclaredMethod("implAddExports", String.class, Module.class);
            ourAddExportsImpl.setAccessible(true);
        }
        catch (Exception ignored) {
        }
    }

    private static ModuleFinder moduleFinderOf(List<File> files) {
        Path[] paths = new Path[files.size()];
        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            paths[i] = file.toPath();
        }

        return ModuleFinder.of(paths);
    }

    public static ModuleLayer initializeBaseModules(List<File> files, ClassLoader targetClassLoader, ContainerLogger containerLogger, Java9ModuleProcessor processor) {
        ModuleFinder moduleFinder = moduleFinderOf(files);

        Set<String> toResolve = new LinkedHashSet<>();

        Set<ModuleReference> findAll = moduleFinder.findAll();

        for (ModuleReference moduleReference : findAll) {
            ModuleDescriptor moduleDescriptor = moduleReference.descriptor();

            String moduleName = moduleDescriptor.name();

            toResolve.add(moduleName);
        }

        ModuleLayer bootModuleLayer = ModuleLayer.boot();

        Configuration confBootModuleLayer = bootModuleLayer.configuration();

        Configuration configuration = Configuration.resolve(moduleFinder, List.of(confBootModuleLayer), ModuleFinder.of(), new ArrayList<>(toResolve));

        ModuleLayer.Controller controller = ModuleLayer.defineModules(configuration, List.of(bootModuleLayer), s -> targetClassLoader);

        ArrayList<Java9ModuleProcessor.Opens> toOpenMap = new ArrayList<>();
        processor.process(toOpenMap);

        for (Java9ModuleProcessor.Opens opens : toOpenMap) {
            Module fromModule = bootModuleLayer.findModule(opens.fromModuleName).get();

            Module toModule = controller.layer().findModule(opens.toModuleName).get();

            fromModule.addOpens(opens.packageName, toModule);
        }

        Module containerApi = bootModuleLayer.findModule("consulo.container.api").get();
        Module ideImpl = controller.layer().findModule("consulo.ide.impl").get();
        Module applicationApi = controller.layer().findModule("consulo.application.api").get();
        Module applicationImpl = controller.layer().findModule("consulo.application.impl").get();
        Module componentImpl = controller.layer().findModule("consulo.component.impl").get();
        Module proxy = controller.layer().findModule("consulo.proxy").get();
        Module externalServiceImpl = controller.layer().findModule("consulo.external.service.impl").get();

        containerApi.addExports("consulo.container", applicationImpl);
        containerApi.addExports("consulo.container", componentImpl);
        containerApi.addExports("consulo.container", ideImpl);
        containerApi.addExports("consulo.container", proxy);

        containerApi.addExports("consulo.container.internal", applicationApi);
        containerApi.addExports("consulo.container.internal", externalServiceImpl);
        
        containerApi.addExports("consulo.container.internal.plugin", applicationApi);
        containerApi.addExports("consulo.container.internal.plugin", proxy);
        containerApi.addExports("consulo.container.internal.plugin", externalServiceImpl);

        containerApi.addExports("consulo.container.internal.plugin.classloader", applicationApi);
        containerApi.addExports("consulo.container.internal.plugin.classloader", ideImpl);
        containerApi.addExports("consulo.container.internal.plugin.classloader", proxy);
        containerApi.addExports("consulo.container.internal.plugin.classloader", applicationImpl);

        containerApi.addExports("consulo.container.internal.plugin", componentImpl);
        containerApi.addExports("consulo.container.internal.plugin", applicationImpl);

        return controller.layer();
    }

    public static ModuleLayer initializeEtcModules(List<ModuleLayer> moduleLayers, List<File> files, ClassLoader targetClassLoader) {
        ModuleFinder moduleFinder = moduleFinderOf(files);

        Set<String> toResolve = new LinkedHashSet<>();

        Set<ModuleReference> findAll = moduleFinder.findAll();

        for (ModuleReference moduleReference : findAll) {
            ModuleDescriptor moduleDescriptor = moduleReference.descriptor();

            String moduleName = moduleDescriptor.name();

            toResolve.add(moduleName);
        }

        List<Configuration> layerConfiguration = new ArrayList<>(moduleLayers.size());
        for (ModuleLayer moduleLayer : moduleLayers) {
            layerConfiguration.add(moduleLayer.configuration());
        }

        Configuration configuration = Configuration.resolve(moduleFinder, layerConfiguration, ModuleFinder.of(), new ArrayList<>(toResolve));

        ModuleLayer.Controller controller = ModuleLayer.defineModules(configuration, moduleLayers, s -> targetClassLoader);

        Map<String, Module> insideModules = new HashMap<>();
        for (Module module : controller.layer().modules()) {
            insideModules.put(module.getName(), module);
        }

        for (ModuleLayer moduleLayer : moduleLayers) {
            Set<Module> modules = moduleLayer.modules();
            for (Module module : modules) {
                ModuleDescriptor descriptor = module.getDescriptor();

                Set<ModuleDescriptor.Exports> exports = descriptor.exports();
                for (ModuleDescriptor.Exports export : exports) {
                    for (String targetModule : export.targets()) {
                        Module insideModule = insideModules.get(targetModule);
                        if (insideModule != null) {
                            try {
                                if (ourAddExportsImpl != null) {
                                    ourAddExportsImpl.invoke(module, export.source(), insideModule);
                                }
                            }
                            catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        }
        return controller.layer();
    }
}
