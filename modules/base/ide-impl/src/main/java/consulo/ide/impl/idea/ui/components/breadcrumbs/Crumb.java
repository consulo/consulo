// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.components.breadcrumbs;

import consulo.language.psi.PsiElement;
import consulo.ide.impl.idea.ui.breadcrumbs.BreadcrumbsProvider;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Sergey.Malenkov
 */
public interface Crumb {
  default Image getIcon() {
    return null;
  }

  default String getText() {
    return toString();
  }

  /**
   * @return synchronously calculated tooltip text
   */
  @Nullable
  default String getTooltip() {
    return null;
  }

  /**
   * @return a list of actions for context menu
   */
  @Nonnull
  default List<? extends Action> getContextActions() {
    return Collections.emptyList();
  }

  class Impl implements Crumb {
    private final Image icon;
    private final String text;
    private final String tooltip;

    @Nonnull
    private final List<? extends Action> actions;

    public Impl(@Nonnull BreadcrumbsProvider provider, @Nonnull PsiElement element) {
      this(provider.getElementIcon(element), provider.getElementInfo(element), provider.getElementTooltip(element), provider.getContextActions(element));
    }

    public Impl(Image icon, String text, String tooltip, Action... actions) {
      this(icon, text, tooltip, actions == null || actions.length == 0 ? Collections.emptyList() : Arrays.asList(actions));
    }

    public Impl(Image icon, String text, String tooltip, @Nonnull List<? extends Action> actions) {
      this.icon = icon;
      this.text = text;
      this.tooltip = tooltip;
      this.actions = actions;
    }

    @Override
    public Image getIcon() {
      return icon;
    }

    @Override
    public String getTooltip() {
      return tooltip;
    }

    @Nonnull
    @Override
    public List<? extends Action> getContextActions() {
      return actions;
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
