package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 10:31/27.05.13
 */
public class JavaCompilerSettings implements CompilerSettings {
  private final Map<BackendCompiler, CompilerSettings> myCompilers = new LinkedHashMap<BackendCompiler, CompilerSettings>();

  public JavaCompilerSettings(@NotNull Project project) {
    for (JavaBackendCompilerProvider provider : JavaBackendCompilerProvider.EP_NAME.getExtensions()) {
      final CompilerSettings settings = provider.createSettings(project);

      myCompilers.put(provider.createCompiler(project), settings == null ? CompilerSettings.EMPTY : settings);
    }
  }

  @Nullable
  @Override
  public Configurable createConfigurable() {
    return new JavaConfigurable(this);
  }

  public BackendCompiler getSelectedCompiler() {
    return myCompilers.keySet().iterator().next();
  }

  @NotNull
  public Map<BackendCompiler, CompilerSettings> getCompilers() {
    return myCompilers;
  }
}
