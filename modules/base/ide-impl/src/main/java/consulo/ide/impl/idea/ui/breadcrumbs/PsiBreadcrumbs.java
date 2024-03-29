/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.ide.impl.idea.ide.IdeTooltipManagerImpl;
import consulo.ide.impl.idea.ui.components.breadcrumbs.Breadcrumbs;
import consulo.ide.impl.idea.ui.components.breadcrumbs.Crumb;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.concurrent.Promise;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Sergey.Malenkov
 */
final class PsiBreadcrumbs extends Breadcrumbs {
  private final static Logger LOG = Logger.getInstance(PsiBreadcrumbs.class);
  private final Map<Crumb, Promise<String>> scheduledTooltipTasks = new HashMap<>();
  boolean above = EditorSettingsExternalizable.getInstance().isBreadcrumbsAbove();

  void updateBorder(int offset) {
    // do not use scaling here because this border is used to align breadcrumbs with a gutter
    setBorder(new EmptyBorder(0, offset, 0, 0));
  }

  @Override
  protected void paintMarker(Graphics2D g, int x, int y, int width, int height, Crumb crumb, int thickness) {
    super.paintMarker(g, x, y, width, above ? height : thickness, crumb, thickness);
  }

  @Override
  public Color getForeground() {
    if (!isForegroundSet()) {
      ColorValue foreground = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.LINE_NUMBERS_COLOR);
      if (foreground != null) return TargetAWT.to(foreground);
    }
    return super.getForeground();
  }

  @Override
  public Color getBackground() {
    if (!isBackgroundSet()) {
      ColorValue background = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.GUTTER_BACKGROUND);
      if (background != null) return TargetAWT.to(background);
    }
    return super.getBackground();
  }

  @Nullable
  @Override
  public String getToolTipText(MouseEvent event) {
    if (hovered == null) {
      return null;
    }

    if (!(hovered instanceof LazyTooltipCrumb) || !((LazyTooltipCrumb)hovered).needCalculateTooltip()) {
      return hovered.getTooltip();
    }

    final Crumb crumb = hovered;
    Promise<String> tooltipLazy;
    synchronized (scheduledTooltipTasks) {
      tooltipLazy = scheduledTooltipTasks.get(crumb);
      if (tooltipLazy == null) {
        Runnable removeFinishedTask = () -> {
          synchronized (scheduledTooltipTasks) {
            scheduledTooltipTasks.remove(crumb);
          }
        };
        final IdeTooltipManagerImpl tooltipManager = IdeTooltipManagerImpl.getInstanceImpl();
        final Component component = event == null ? null : event.getComponent();
        tooltipLazy = ReadAction.nonBlocking(crumb::getTooltip)
                                .expireWhen(() -> !tooltipManager.isProcessing(component))
                                .finishOnUiThread(Application::getAnyModalityState, toolTipText -> tooltipManager.updateShownTooltip(component)).submit(AppExecutorUtil.getAppExecutorService()).onError(throwable -> {
                  if (!(throwable instanceof CancellationException)) {
                    LOG.error("Exception in LazyTooltipCrumb", throwable);
                  }
                  removeFinishedTask.run();
                }).onSuccess(toolTipText -> removeFinishedTask.run());
        scheduledTooltipTasks.put(crumb, tooltipLazy);
      }
    }
    if (tooltipLazy.isSucceeded()) {
      try {
        return tooltipLazy.blockingGet(0);
      }
      catch (TimeoutException | ExecutionException e) {
        LOG.error(e);
      }
    }
    return getLazyTooltipProgressText();
  }

  @Nonnull
  private static String getLazyTooltipProgressText() {
    return UIBundle.message("crumbs.calculating.tooltip");
  }

  @Override
  protected ColorValue getForeground(Crumb crumb) {
    CrumbPresentation presentation = PsiCrumb.getPresentation(crumb);
    if (presentation == null) return super.getForeground(crumb);

    ColorValue background = super.getBackground(crumb);
    if (background != null) return super.getForeground(crumb);

    return presentation.getBackgroundColor(isSelected(crumb), isHovered(crumb), isAfterSelected(crumb));
  }

  @Override
  protected ColorValue getBackground(Crumb crumb) {
    CrumbPresentation presentation = PsiCrumb.getPresentation(crumb);
    if (presentation == null) return super.getBackground(crumb);

    ColorValue background = super.getBackground(crumb);
    if (background == null) return null;

    return presentation.getBackgroundColor(isSelected(crumb), isHovered(crumb), isAfterSelected(crumb));
  }

  @Override
  protected TextAttributes getAttributes(Crumb crumb) {
    TextAttributesKey key = getKey(crumb);
    return key == null ? null : EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key);
  }

  private TextAttributesKey getKey(Crumb crumb) {
    if (isHovered(crumb)) return EditorColors.BREADCRUMBS_HOVERED;
    if (isSelected(crumb)) return EditorColors.BREADCRUMBS_CURRENT;
    if (isAfterSelected(crumb)) return EditorColors.BREADCRUMBS_INACTIVE;
    return EditorColors.BREADCRUMBS_DEFAULT;
  }
}
