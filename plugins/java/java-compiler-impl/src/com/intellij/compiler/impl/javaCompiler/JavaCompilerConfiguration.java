package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.consulo.lombok.annotations.ProjectService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private static final String DEFAULT_COMPILER = "JavacCompiler";

  @NotNull
  private final Project myProject;

  private BackendCompiler myBackendCompilerCache;

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
}
