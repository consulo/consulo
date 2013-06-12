package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.components.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.consulo.lombok.annotations.ProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 10:31/27.05.13
 */
@ProjectService
@State(
  name = "JavaCompilerConfiguration",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class JavaCompilerConfiguration implements PersistentStateComponent<JavaCompilerConfiguration.JavaCompilerConfigurationState> {
  public static class JavaCompilerConfigurationState {
    public String myCompilerClassName = DEFAULT_COMPILER;
  }

  public static final String DEFAULT_COMPILER = "JavacCompiler";

  @NotNull
  private final Project myProject;

  private BackendCompiler myBackendCompilerCache;

  private final ProcessorConfigProfile myDefaultProcessorsProfile = new ProcessorConfigProfileImpl("Default");
  private final List<ProcessorConfigProfile> myModuleProcessorProfiles = new ArrayList<ProcessorConfigProfile>();

  // the map is calculated by module processor profiles list for faster access to module settings
  private Map<Module, ProcessorConfigProfile> myProcessorsProfilesMap = null;

  public JavaCompilerConfiguration(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  public BackendCompiler getActiveCompiler() {
    if(myBackendCompilerCache == null) {
      myBackendCompilerCache = findCompiler(DEFAULT_COMPILER);
    }
    return myBackendCompilerCache;
  }

  public void setActiveCompiler(@NotNull BackendCompiler key) {
    myBackendCompilerCache = key;
  }

  @Nullable
  public BackendCompiler findCompiler(@NotNull String className) {
    for (BackendCompilerEP ep : BackendCompiler.EP_NAME.getExtensions(myProject)) {
      if (className.equals(ep.getInstance(myProject).getClass().getSimpleName())) {
        return ep.getInstance(myProject);
      }
    }
    return null;
  }

  @Nullable
  @Override
  public JavaCompilerConfigurationState getState() {
    JavaCompilerConfigurationState mySettings = new JavaCompilerConfigurationState();
    mySettings.myCompilerClassName = myBackendCompilerCache.getClass().getSimpleName();
    return mySettings;
  }

  @Override
  public void loadState(JavaCompilerConfigurationState state) {
    myBackendCompilerCache = findCompiler(state.myCompilerClassName);
  }

  @NotNull
  public ProcessorConfigProfile getAnnotationProcessingConfiguration(Module module) {
    Map<Module, ProcessorConfigProfile> map = myProcessorsProfilesMap;
    if (map == null) {
      map = new HashMap<Module, ProcessorConfigProfile>();
      final Map<String, Module> namesMap = new HashMap<String, Module>();
      for (Module m : ModuleManager.getInstance(module.getProject()).getModules()) {
        namesMap.put(m.getName(), m);
      }
      if (!namesMap.isEmpty()) {
        for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
          for (String name : profile.getModuleNames()) {
            final Module mod = namesMap.get(name);
            if (mod != null) {
              map.put(mod, profile);
            }
          }
        }
      }
      myProcessorsProfilesMap = map;
    }
    final ProcessorConfigProfile profile = map.get(module);
    return profile != null? profile : myDefaultProcessorsProfile;
  }

  public boolean isAnnotationProcessorsEnabled() {
    if (myDefaultProcessorsProfile.isEnabled()) {
      return true;
    }
    for (ProcessorConfigProfile profile : myModuleProcessorProfiles) {
      if (profile.isEnabled()) {
        return true;
      }
    }
    return false;
  }
}
