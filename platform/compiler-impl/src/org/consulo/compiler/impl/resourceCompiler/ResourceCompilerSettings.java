package org.consulo.compiler.impl.resourceCompiler;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.TextConfigurable;
import org.consulo.compiler.CompilerSettings;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 20:16/24.05.13
 */
public class ResourceCompilerSettings implements CompilerSettings<ResourceCompiler> {
  @Nullable
  @Override
  public Element getState() {
    return null;
  }

  @Override
  public void loadState(Element state) {

  }

  @Override
  public Configurable createConfigurable() {
    return new TextConfigurable<CompilerSettings<ResourceCompiler>>(this, "Test", "Test", "Test", AllIcons.Nodes.Module);
  }
}
