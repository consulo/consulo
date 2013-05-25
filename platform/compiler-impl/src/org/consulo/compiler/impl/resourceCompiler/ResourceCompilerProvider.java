package org.consulo.compiler.impl.resourceCompiler;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.MalformedPatternException;
import com.intellij.compiler.impl.resourceCompiler.ResourceCompiler;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.consulo.compiler.CompilerProvider;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

/**
 * @author VISTALL
 * @since 20:16/24.05.13
 */
public class ResourceCompilerProvider implements CompilerProvider<ResourceCompiler> {
  @NotNull
  @Override
  public ResourceCompiler createCompiler(Project project) {
    return new ResourceCompiler(project, new CompilerConfiguration(){
      @Nullable
      @Override
      public String getBytecodeTargetLevel(Module module) {
        return null;
      }

      @NotNull
      @Override
      public AnnotationProcessingConfiguration getAnnotationProcessingConfiguration(Module module) {
        return null;
      }

      @Override
      public boolean isAnnotationProcessorsEnabled() {
        return false;
      }

      @Override
      public boolean isExcludedFromCompilation(VirtualFile virtualFile) {
        return false;
      }

      @Override
      public boolean isResourceFile(VirtualFile virtualFile) {
        return false;
      }

      @Override
      public boolean isResourceFile(String path) {
        return false;
      }

      @Override
      public void addResourceFilePattern(String namePattern) throws MalformedPatternException {

      }

      @Override
      public String[] getResourceFilePatterns() {
        return new String[0];
      }

      @Override
      public void removeResourceFilePatterns() {

      }

      @Override
      public void convertPatterns() {

      }

      @Override
      public boolean isAddNotNullAssertions() {
        return false;
      }

      @Override
      public void setAddNotNullAssertions(boolean enabled) {

      }

      @Override
      public ExcludedEntriesConfiguration getExcludedEntriesConfiguration() {
        return null;
      }
    });
  }

  @Override
  public CompilerSettings createSettings(@NotNull Project project) {
    return new ResourceCompilerSettings(project);
  }
}
