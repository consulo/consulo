// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal;

import consulo.application.util.ColoredItem;
import consulo.component.util.WeighedItem;
import consulo.content.scope.NamedScope;
import consulo.language.editor.FileColorManager;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.project.Project;

import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;

/**
 * @author gregsh
 */
public class DefaultSearchScopeProviders {
  private DefaultSearchScopeProviders() {
  }

  @Nonnull
  public static GlobalSearchScope wrapNamedScope(@Nonnull Project project, @Nonnull NamedScope namedScope, boolean colored) {
    GlobalSearchScope scope = GlobalSearchScopesCore.filterScope(project, namedScope);
    if (!colored && !(namedScope instanceof WeighedItem)) return scope;
    int weight = namedScope instanceof WeighedItem ? ((WeighedItem)namedScope).getWeight() : -1;
    Color color = !colored ? null :
                  //namedScope instanceof ColoredItem ? ((ColoredItem)namedScope).getColor() :
                  FileColorManager.getInstance(project).getScopeColor(namedScope.getScopeId());
    return new MyWeightedScope(scope, weight, TargetAWT.from(color));
  }

  private static class MyWeightedScope extends DelegatingGlobalSearchScope implements WeighedItem, ColoredItem {
    final int weight;
    final ColorValue color;

    MyWeightedScope(@Nonnull GlobalSearchScope scope, int weight, ColorValue color) {
      super(scope);
      this.weight = weight;
      this.color = color;
    }

    @Override
    public int getWeight() {
      return weight;
    }

    @Nullable
    @Override
    public ColorValue getColor() {
      return color;
    }
  }
}
