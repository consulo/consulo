// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.documentation.InlineDocumentation;
import consulo.language.editor.documentation.InlineDocumentationProvider;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageState;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public class DocRenderItemImpl implements DocRenderItem {
    private final Editor myEditor;
    private final RangeHighlighter myHighlighter;

    private CustomFoldRegion myFoldRegion;
    private String myTextToRender;

    DocRenderItemImpl(Editor editor, TextRange textRange, @Nullable String textToRender) {
        myEditor = editor;
        myTextToRender = textToRender;
        myHighlighter = editor.getMarkupModel()
            .addRangeHighlighter(null, textRange.getStartOffset(), textRange.getEndOffset(), 0, HighlighterTargetArea.EXACT_RANGE);
        updateIcon(null);
    }

    @Override
    public Editor getEditor() {
        return myEditor;
    }

    @Override
    public RangeHighlighter getHighlighter() {
        return myHighlighter;
    }

    @Override
    public @Nullable CustomFoldRegion getFoldRegion() {
        return myFoldRegion;
    }

    @Override
    public @Nullable String getTextToRender() {
        return myTextToRender;
    }

    @Override
    public @Nullable GutterIconRenderer calcFoldingGutterIconRenderer() {
        if (!(myHighlighter.getGutterIconRenderer() instanceof MyGutterIconRenderer highlighterIconRenderer)) {
            return null;
        }
        return new MyGutterIconRenderer(PlatformIconGroup.gutterJavadocedit(), highlighterIconRenderer.isIconVisible());
    }

    void updateIcon(@Nullable List<Runnable> foldingTasks) {
        boolean iconEnabled = DocRenderDummyLineMarkerProvider.isGutterIconEnabled();
        boolean iconExists = myHighlighter.getGutterIconRenderer() != null;
        if (iconEnabled != iconExists) {
            myHighlighter.setGutterIconRenderer(
                iconEnabled ? new MyGutterIconRenderer(PlatformIconGroup.gutterJavadocread(), false) : null);
            CustomFoldRegion region = myFoldRegion;
            if (region != null && region.getRenderer() instanceof DocRenderer renderer) {
                renderer.update(false, false, foldingTasks);
            }
        }
    }

    @Override
    public void setIconVisible(boolean visible) {
        if (myHighlighter.getGutterIconRenderer() instanceof MyGutterIconRenderer iconRenderer) {
            iconRenderer.setIconVisible(visible);
            int y = myEditor.visualLineToY(((RealEditor) myEditor).offsetToVisualLine(myHighlighter.getStartOffset()));
            repaintGutter(y);
        }
        CustomFoldRegion region = myFoldRegion;
        if (region != null && region.getGutterIconRenderer() instanceof MyGutterIconRenderer inlayIconRenderer) {
            inlayIconRenderer.setIconVisible(visible);
            repaintGutter(myEditor.offsetToXY(region.getStartOffset()).y);
        }
    }

    private void repaintGutter(int startY) {
        JComponent gutter = ((EditorGutterComponentEx) myEditor.getGutter()).getComponent();
        gutter.repaint(0, startY, gutter.getWidth(), startY + myEditor.getLineHeight());
    }

    void setTextToRender(@Nullable String textToRender) {
        myTextToRender = textToRender;
    }

    public boolean isValid() {
        return myHighlighter.isValid()
            && myHighlighter.getStartOffset() < myHighlighter.getEndOffset()
            && matchesFoldRegion();
    }

    private boolean matchesFoldRegion() {
        CustomFoldRegion region = myFoldRegion;
        if (region == null) {
            return true;
        }
        Document document = myHighlighter.getDocument();
        return region.isValid()
            && document.getLineNumber(myHighlighter.getStartOffset()) == document.getLineNumber(region.getStartOffset())
            && document.getLineNumber(myHighlighter.getEndOffset()) == document.getLineNumber(region.getEndOffset());
    }

    boolean remove(Collection<Runnable> foldingTasks) {
        myHighlighter.dispose();
        CustomFoldRegion region = myFoldRegion;
        if (region != null && region.isValid()) {
            foldingTasks.add(() -> region.getEditor().getFoldingModel().removeFoldRegion(region));
            return true;
        }
        return false;
    }

    @Override
    public void toggle() {
        if (!isValid()) {
            return;
        }
        toggle(null);
    }

    /**
     * When rendering is globally disabled, the pass doesn't compute the HTML, so it is generated on demand here.
     */
    private void generateHtmlInBackgroundAndToggle() {
        Project project = myEditor.getProject();
        if (project == null) {
            return;
        }
        ReadAction.<String>nonBlocking(() -> DocRenderPassFactory.calcText(findInlineDocumentation(project)))
            .coalesceBy(this)
            .finishOnUiThread(Application::getDefaultModalityState, html -> {
                myTextToRender = html;
                toggle();
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    @RequiredReadAction
    private @Nullable InlineDocumentation findInlineDocumentation(Project project) {
        if (!myHighlighter.isValid()) {
            return null;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument());
        if (file == null) {
            return null;
        }
        TextRange range = TextRange.create(myHighlighter.getStartOffset(), myHighlighter.getEndOffset());
        for (InlineDocumentationProvider provider : project.getApplication().getExtensionList(InlineDocumentationProvider.class)) {
            InlineDocumentation documentation = provider.findInlineDocumentation(file, range);
            if (documentation != null) {
                return documentation;
            }
        }
        return null;
    }

    boolean toggle(@Nullable Collection<Runnable> foldingTasks) {
        FoldingModel foldingModel = myEditor.getFoldingModel();
        CustomFoldRegion region = myFoldRegion;
        if (region == null) {
            if (myTextToRender == null && foldingTasks == null) {
                generateHtmlInBackgroundAndToggle();
                return false;
            }
            if (myTextToRender == null) {
                return false;
            }
            Document document = myHighlighter.getDocument();
            int foldStartLine = document.getLineNumber(myHighlighter.getStartOffset());
            int foldEndLine = document.getLineNumber(myHighlighter.getEndOffset());
            Runnable foldingTask = () -> myFoldRegion = foldingModel.addCustomLinesFolding(foldStartLine, foldEndLine, new DocRenderer(this));
            if (foldingTasks == null) {
                foldingModel.runBatchFoldingOperation(foldingTask, true, false);
            }
            else {
                foldingTasks.add(foldingTask);
            }
        }
        else {
            Runnable foldingTask = () -> {
                int startOffset = region.getStartOffset();
                int endOffset = region.getEndOffset();
                foldingModel.removeFoldRegion(region);
                for (FoldRegion r : myEditor.getFoldingModel().getAllFoldRegions()) {
                    if (r.getStartOffset() >= startOffset && r.getEndOffset() <= endOffset) {
                        r.setExpanded(true);
                    }
                }
                myFoldRegion = null;
            };
            if (foldingTasks == null) {
                foldingModel.runBatchFoldingOperation(foldingTask, true, false);
            }
            else {
                foldingTasks.add(foldingTask);
            }
            if (!DocRenderManager.isDocRenderingEnabled(myEditor)) {
                // the value won't be updated by DocRenderPass on document modification, so we shouldn't cache the value
                myTextToRender = null;
            }
        }
        return true;
    }

    private final class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
        private final ImageState<Boolean> myIconVisible;
        private final Image myIcon;

        MyGutterIconRenderer(Image icon, boolean iconVisible) {
            myIconVisible = new ImageState<>(iconVisible);
            myIcon = Image.stated(myIconVisible, visible -> visible ? icon : ImageEffects.empty(icon.getWidth(), icon.getHeight()));
        }

        boolean isIconVisible() {
            return myIconVisible.getState();
        }

        void setIconVisible(boolean visible) {
            myIconVisible.setState(visible);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MyGutterIconRenderer;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public Image getIcon() {
            return myIcon;
        }

        @Override
        public GutterIconRenderer.Alignment getAlignment() {
            return GutterIconRenderer.Alignment.RIGHT;
        }

        @Override
        public boolean isNavigateAction() {
            return true;
        }

        @Override
        public LocalizeValue getTooltipValue() {
            AnAction action = ActionManager.getInstance().getAction("ToggleRenderedDocPresentation");
            if (action == null) {
                return LocalizeValue.empty();
            }
            String actionText = action.getTemplatePresentation().getText();
            if (actionText == null) {
                return LocalizeValue.empty();
            }
            return LocalizeValue.of(KeymapUtil.createTooltipText(actionText, action));
        }

        @Override
        public @Nullable AnAction getClickAction() {
            return new DocRenderer.ToggleRenderingAction(DocRenderItemImpl.this);
        }

        @Override
        public @Nullable ActionGroup getPopupMenuActions() {
            return ActionManager.getInstance().getAction("DocCommentGutterIconContextMenu") instanceof ActionGroup group
                ? group
                : null;
        }
    }
}
