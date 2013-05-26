package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.projectRoot.TextConfigurable;
import org.consulo.compiler.CompilerProvider;
import org.consulo.compiler.CompilerSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 16:26/26.05.13
 */
public class JavaCompilerProvider implements CompilerProvider<JavaCompiler> {
  @NotNull
  @Override
  public JavaCompiler createCompiler(Project project) {
    return new JavaCompiler(project);
  }

  @Override
  public CompilerSettings createSettings(@NotNull Project project) {
    return new CompilerSettings() {
      @Override
      public Configurable createConfigurable() {
        return new TextConfigurable<String>("", "", "", "", null);
      }

      @Nullable
      @Override
      public Element getState() {
        return null;
      }

      @Override
      public void loadState(Element state) {

      }
    };
  }
}
