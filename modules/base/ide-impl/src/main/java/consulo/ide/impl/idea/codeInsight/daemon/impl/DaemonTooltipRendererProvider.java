// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.ide.impl.idea.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import consulo.ide.impl.idea.codeInsight.hint.LineTooltipRenderer;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;
import consulo.language.editor.impl.internal.markup.ErrorStripTooltipRendererProvider;
import consulo.language.editor.impl.internal.markup.TrafficTooltipRenderer;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author max
 */
public class DaemonTooltipRendererProvider implements ErrorStripTooltipRendererProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DaemonTooltipRendererProvider.class);

    private final Project myProject;
    private final Editor myEditor;

    public DaemonTooltipRendererProvider(Project project, Editor editor) {
        myProject = project;
        myEditor = editor;
    }

    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull Collection<? extends RangeHighlighter> highlighters) {
        LineTooltipRenderer bigRenderer = null;
        List<HighlightInfoImpl> infos = new SmartList<>();
        Collection<LocalizeValue> tooltips = new HashSet<>(); //do not show same tooltip twice
        for (RangeHighlighter marker : highlighters) {
            Object tooltipObject = marker.getErrorStripeTooltip();
            if (tooltipObject instanceof HighlightInfoImpl info) {
                if (info.getToolTip().isNotEmpty() && tooltips.add(info.getToolTip())) {
                    infos.add(info);
                }
            }
            else if (tooltipObject instanceof LocalizeValue tooltip) {
                if (tooltips.add(tooltip)) {
                    if (bigRenderer == null) {
                        bigRenderer = new DaemonTooltipRenderer(tooltip.get(), new Object[]{highlighters});
                    }
                    else {
                        bigRenderer.addBelow(tooltip.get());
                    }
                }
            }
            else if (tooltipObject != null) {
                LOG.warn("Expected HighlightInfo or LocalizeValue, got {}", tooltipObject.getClass());
            }
        }
        if (!infos.isEmpty()) {
            // show errors first
            Lists.quickSort(
                infos,
                (o1, o2) -> {
                    int i = SeverityRegistrarImpl.getSeverityRegistrar(myProject).compare(o2.getSeverity(), o1.getSeverity());
                    if (i != 0) {
                        return i;
                    }
                    return o1.getToolTip().compareTo(o2.getToolTip());
                }
            );
            HighlightInfoComposite composite = HighlightInfoComposite.create(infos);
            LocalizeValue tooltip = composite.getToolTip();
            DaemonTooltipRenderer myRenderer;
            if (Registry.is("ide.tooltip.show.with.actions")) {
                TooltipAction action = TooltipActionProvider.calcTooltipAction(composite, myEditor);
                myRenderer = new DaemonTooltipWithActionRenderer(
                    tooltip.get(),
                    action,
                    0,
                    action == null ? new Object[]{tooltip.get()} : new Object[]{tooltip.get(), action}
                );
            }
            else {
                myRenderer = new DaemonTooltipRenderer(tooltip.get(), new Object[]{highlighters});
            }
            bigRenderer = myRenderer;
        }
        return bigRenderer;
    }

    @Nonnull
    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull String text) {
        return new DaemonTooltipRenderer(text, new Object[]{text});
    }

    @Nonnull
    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull String text, int width) {
        return new DaemonTooltipRenderer(text, width, new Object[]{text});
    }

    @Nonnull
    @Override
    public TooltipRenderer calcTooltipRenderer(@Nonnull String text, @Nullable TooltipAction action, int width) {
        if (action != null || Registry.is("ide.tooltip.show.with.actions")) {
            return new DaemonTooltipWithActionRenderer(
                text,
                action,
                width,
                action == null ? new Object[]{text} : new Object[]{text, action}
            );
        }

        return ErrorStripTooltipRendererProvider.super.calcTooltipRenderer(text, action, width);
    }

    @Nonnull
    @Override
    public TrafficTooltipRenderer createTrafficTooltipRenderer(@Nonnull Runnable onHide, @Nonnull Editor editor) {
        return new TrafficTooltipRendererImpl(onHide, editor);
    }
}