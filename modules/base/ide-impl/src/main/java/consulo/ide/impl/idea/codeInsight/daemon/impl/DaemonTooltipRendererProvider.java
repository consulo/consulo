// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.ide.impl.idea.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import consulo.ide.impl.idea.codeInsight.hint.LineTooltipRenderer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.language.editor.impl.internal.markup.TrafficTooltipRenderer;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.project.Project;
import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author max
 */
public class DaemonTooltipRendererProvider implements ErrorStripTooltipRendererProvider {
  private final Project myProject;
  private final Editor myEditor;

  public DaemonTooltipRendererProvider(final Project project, Editor editor) {
    myProject = project;
    myEditor = editor;
  }

  @Override
  public TooltipRenderer calcTooltipRenderer(@Nonnull final Collection<? extends RangeHighlighter> highlighters) {
    LineTooltipRenderer bigRenderer = null;
    List<HighlightInfoImpl> infos = new SmartList<>();
    Collection<String> tooltips = new HashSet<>(); //do not show same tooltip twice
    for (RangeHighlighter marker : highlighters) {
      final Object tooltipObject = marker.getErrorStripeTooltip();
      if (tooltipObject == null) continue;
      if (tooltipObject instanceof HighlightInfoImpl) {
        HighlightInfoImpl info = (HighlightInfoImpl)tooltipObject;
        if (info.getToolTip() != null && tooltips.add(info.getToolTip())) {
          infos.add(info);
        }
      }
      else {
        final String text = tooltipObject.toString();
        if (tooltips.add(text)) {
          if (bigRenderer == null) {
            bigRenderer = new DaemonTooltipRenderer(text, new Object[]{highlighters});
          }
          else {
            bigRenderer.addBelow(text);
          }
        }
      }
    }
    if (!infos.isEmpty()) {
      // show errors first
      ContainerUtil.quickSort(infos, (o1, o2) -> {
        int i = SeverityRegistrarImpl.getSeverityRegistrar(myProject).compare(o2.getSeverity(), o1.getSeverity());
        if (i != 0) return i;
        return o1.getToolTip().compareTo(o2.getToolTip());
      });
      final HighlightInfoComposite composite = HighlightInfoComposite.create(infos);
      String toolTip = composite.getToolTip();
      DaemonTooltipRenderer myRenderer;
      if (Registry.is("ide.tooltip.show.with.actions")) {
        TooltipAction action = TooltipActionProvider.calcTooltipAction(composite, myEditor);
        myRenderer = new DaemonTooltipWithActionRenderer(toolTip, action, 0, action == null ? new Object[]{toolTip} : new Object[]{toolTip, action});
      }
      else {
        myRenderer = new DaemonTooltipRenderer(toolTip, new Object[]{highlighters});
      }
      if (bigRenderer != null) {
        myRenderer.addBelow(bigRenderer.getText());
      }
      bigRenderer = myRenderer;
    }
    return bigRenderer;
  }

  @Nonnull
  @Override
  public TooltipRenderer calcTooltipRenderer(@Nonnull final String text) {
    return new DaemonTooltipRenderer(text, new Object[]{text});
  }

  @Nonnull
  @Override
  public TooltipRenderer calcTooltipRenderer(@Nonnull final String text, final int width) {
    return new DaemonTooltipRenderer(text, width, new Object[]{text});
  }

  @Nonnull
  @Override
  public TooltipRenderer calcTooltipRenderer(@Nonnull String text, @Nullable TooltipAction action, int width) {
    if (action != null || Registry.is("ide.tooltip.show.with.actions")) {
      return new DaemonTooltipWithActionRenderer(text, action, width, action == null ? new Object[]{text} : new Object[]{text, action});
    }

    return ErrorStripTooltipRendererProvider.super.calcTooltipRenderer(text, action, width);
  }

  @Nonnull
  @Override
  public TrafficTooltipRenderer createTrafficTooltipRenderer(@Nonnull Runnable onHide, @Nonnull Editor editor) {
    return new TrafficTooltipRendererImpl(onHide, editor);
  }
}