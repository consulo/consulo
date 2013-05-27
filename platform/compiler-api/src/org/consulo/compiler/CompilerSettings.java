package org.consulo.compiler;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.options.Configurable;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:58/24.05.13
 */
public interface CompilerSettings extends PersistentStateComponent<Element>{
  CompilerSettings EMPTY = new CompilerSettings() {

    @Override
    public Configurable createConfigurable() {
      return null;
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
  @Nullable
  Configurable createConfigurable();
}
