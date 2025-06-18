/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.execution.coverage;

import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.ActiveGutterRenderer;
import consulo.codeEditor.markup.LineMarkerRenderer;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.document.Document;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.action.HideCoverageInfoAction;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontOptions;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontPanelFactory;
import consulo.ide.impl.idea.application.options.colors.NewColorAndFontPanel;
import consulo.ide.impl.idea.application.options.colors.SimpleEditorPreview;
import consulo.ide.impl.idea.codeInsight.hint.EditorFragmentComponent;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.coverage.actions.ShowCoveringTestsAction;
import consulo.ide.impl.idea.openapi.options.colors.pages.GeneralColorsPage;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ColoredSideBorder;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author ven
 */
public class CoverageLineMarkerRenderer implements LineMarkerRenderer, ActiveGutterRenderer {
    private static final int THICKNESS = 8;
    private final TextAttributesKey myKey;
    private final String myClassName;
    private final TreeMap<Integer, LineData> myLines;
    private final boolean myCoverageByTestApplicable;
    private final Function<Integer, Integer> myNewToOldConverter;
    private final Function<Integer, Integer> myOldToNewConverter;
    private final CoverageSuitesBundle myCoverageSuite;
    private final boolean mySubCoverageActive;

    protected CoverageLineMarkerRenderer(
        TextAttributesKey textAttributesKey,
        @Nullable String className,
        TreeMap<Integer, LineData> lines,
        boolean coverageByTestApplicable,
        Function<Integer, Integer> newToOldConverter,
        Function<Integer, Integer> oldToNewConverter,
        CoverageSuitesBundle coverageSuite,
        boolean subCoverageActive
    ) {
        myKey = textAttributesKey;
        myClassName = className;
        myLines = lines;
        myCoverageByTestApplicable = coverageByTestApplicable;
        myNewToOldConverter = newToOldConverter;
        myOldToNewConverter = oldToNewConverter;
        myCoverageSuite = coverageSuite;
        mySubCoverageActive = subCoverageActive;
    }

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
        TextAttributes color = editor.getColorsScheme().getAttributes(myKey);
        ColorValue bgColor = color.getBackgroundColor();
        if (bgColor == null) {
            bgColor = color.getForegroundColor();
        }
        if (editor.getSettings().isLineNumbersShown() || editor.getGutter().isAnnotationsShown()) {
            if (bgColor != null) {
                bgColor = bgColor.withAlpha(0.6f);
            }
        }
        if (bgColor != null) {
            g.setColor(TargetAWT.to(bgColor));
        }
        g.fillRect(r.x, r.y, r.width, r.height);
        LineData lineData = getLineData(editor.xyToLogicalPosition(new Point(0, r.y)).line);
        if (lineData != null && lineData.isCoveredByOneTest()) {
            TargetAWT.to(PlatformIconGroup.gutterUnique()).paintIcon(editor.getComponent(), g, r.x, r.y);
        }
    }

    public static CoverageLineMarkerRenderer getRenderer(
        int lineNumber,
        @Nullable String className,
        TreeMap<Integer, LineData> lines,
        boolean coverageByTestApplicable,
        @Nonnull CoverageSuitesBundle coverageSuite,
        Function<Integer, Integer> newToOldConverter,
        Function<Integer, Integer> oldToNewConverter,
        boolean subCoverageActive
    ) {
        return new CoverageLineMarkerRenderer(
            getAttributesKey(lineNumber, lines),
            className,
            lines,
            coverageByTestApplicable,
            newToOldConverter,
            oldToNewConverter,
            coverageSuite,
            subCoverageActive
        );
    }

    public static TextAttributesKey getAttributesKey(int lineNumber, TreeMap<Integer, LineData> lines) {
        return getAttributesKey(lines.get(lineNumber));
    }

    private static TextAttributesKey getAttributesKey(LineData lineData) {
        if (lineData != null) {
            switch (lineData.getStatus()) {
                case LineCoverage.FULL:
                    return CodeInsightColors.LINE_FULL_COVERAGE;
                case LineCoverage.PARTIAL:
                    return CodeInsightColors.LINE_PARTIAL_COVERAGE;
            }
        }

        return CodeInsightColors.LINE_NONE_COVERAGE;
    }

    @Override
    public boolean canDoAction(@Nonnull MouseEvent e) {
        Component component = e.getComponent();
        return component instanceof EditorGutterComponentEx gutter
            && e.getX() > gutter.getLineMarkerAreaOffset()
            && e.getX() < gutter.getIconAreaOffset();
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void doAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
        e.consume();
        JComponent comp = (JComponent) e.getComponent();
        JRootPane rootPane = comp.getRootPane();
        JLayeredPane layeredPane = rootPane.getLayeredPane();
        Point point = SwingUtilities.convertPoint(comp, THICKNESS, e.getY(), layeredPane);
        showHint(editor, point, editor.xyToLogicalPosition(e.getPoint()).line);
    }

    @RequiredUIAccess
    private void showHint(Editor editor, Point point, int lineNumber) {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(createActionsToolbar(editor, lineNumber), BorderLayout.NORTH);

        LineData lineData = getLineData(lineNumber);
        final Editor uEditor;
        if (lineData != null && lineData.getStatus() != LineCoverage.NONE && !mySubCoverageActive) {
            EditorFactory factory = EditorFactory.getInstance();
            Document doc = factory.createDocument(getReport(editor, lineNumber));
            doc.setReadOnly(true);
            uEditor = factory.createEditor(doc, editor.getProject());
            panel.add(
                EditorFragmentComponent.createEditorFragmentComponent(uEditor, 0, doc.getLineCount(), false, false),
                BorderLayout.CENTER
            );
        }
        else {
            uEditor = null;
        }

        LightweightHintImpl hint = new LightweightHintImpl(panel) {
            @Override
            public void hide() {
                if (uEditor != null) {
                    EditorFactory.getInstance().releaseEditor(uEditor);
                }
                super.hide();
            }
        };
        HintManagerImpl.getInstanceImpl().showEditorHint(
            hint,
            editor,
            point,
            HintManagerImpl.HIDE_BY_ANY_KEY | HintManagerImpl.HIDE_BY_TEXT_CHANGE
                | HintManagerImpl.HIDE_BY_OTHER_HINT | HintManagerImpl.HIDE_BY_SCROLLING,
            -1,
            false,
            new HintHint(editor.getContentComponent(), point)
        );
    }

    private String getReport(Editor editor, int lineNumber) {
        LineData lineData = getLineData(lineNumber);

        Document document = editor.getDocument();
        Project project = editor.getProject();
        assert project != null;

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        assert psiFile != null;

        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int lineEndOffset = document.getLineEndOffset(lineNumber);

        return myCoverageSuite.getCoverageEngine()
            .generateBriefReport(editor, psiFile, lineNumber, lineStartOffset, lineEndOffset, lineData);
    }

    @RequiredUIAccess
    protected JComponent createActionsToolbar(Editor editor, int lineNumber) {
        JComponent editorComponent = editor.getComponent();

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        GotoPreviousCoveredLineAction prevAction = new GotoPreviousCoveredLineAction(editor, lineNumber);
        GotoNextCoveredLineAction nextAction = new GotoNextCoveredLineAction(editor, lineNumber);

        group.add(prevAction);
        group.add(nextAction);

        prevAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)),
            editorComponent
        );
        nextAction.registerCustomShortcutSet(
            new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)),
            editorComponent
        );

        LineData lineData = getLineData(lineNumber);
        if (myCoverageByTestApplicable) {
            group.add(new ShowCoveringTestsAction(myClassName, lineData));
        }
        AnAction byteCodeViewAction = ActionManager.getInstance().getAction("ByteCodeViewer");
        if (byteCodeViewAction != null) {
            group.add(byteCodeViewAction);
        }
        group.add(new EditCoverageColorsAction(editor, lineNumber));
        group.add(new HideCoverageInfoAction());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.FILEHISTORY_VIEW_TOOLBAR, group.build(), true);
        JComponent toolbarComponent = toolbar.getComponent();

        ColorValue background = ((EditorEx) editor).getBackgroundColor();
        ColorValue foreground = editor.getColorsScheme().getColor(EditorColors.CARET_COLOR);
        toolbarComponent.setBackground(TargetAWT.to(background));
        Color awtForeground = TargetAWT.to(foreground);
        toolbarComponent.setBorder(new ColoredSideBorder(
            awtForeground,
            awtForeground,
            lineData == null || lineData.getStatus() == LineCoverage.NONE || mySubCoverageActive ? awtForeground : null,
            awtForeground,
            1
        ));
        toolbar.updateActionsImmediately();
        return toolbarComponent;
    }

    public void moveToLine(int lineNumber, Editor editor) {
        int firstOffset = editor.getDocument().getLineStartOffset(lineNumber);
        editor.getCaretModel().moveToOffset(firstOffset);
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

        editor.getScrollingModel().runActionOnScrollingFinished(() -> {
            Point p = editor.visualPositionToXY(editor.offsetToVisualPosition(firstOffset));
            EditorGutterComponentEx editorComponent = (EditorGutterComponentEx) editor.getGutter();
            JLayeredPane layeredPane = editorComponent.getComponent().getRootPane().getLayeredPane();
            p = SwingUtilities.convertPoint(editorComponent.getComponent(), THICKNESS, p.y, layeredPane);
            showHint(editor, p, lineNumber);
        });
    }

    @Nullable
    public LineData getLineData(int lineNumber) {
        return myLines != null
            ? myLines.get(myNewToOldConverter != null ? myNewToOldConverter.apply(lineNumber) : lineNumber)
            : null;
    }

    public ColorValue getErrorStripeColor(Editor editor) {
        return editor.getColorsScheme().getAttributes(myKey).getErrorStripeColor();
    }

    @Nonnull
    @Override
    public Position getPosition() {
        return Position.LEFT;
    }

    private class GotoPreviousCoveredLineAction extends BaseGotoCoveredLineAction {
        public GotoPreviousCoveredLineAction(Editor editor, int lineNumber) {
            super(editor, lineNumber);
            copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_PREVIOUS_OCCURENCE));
            getTemplatePresentation().setTextValue(ExecutionCoverageLocalize.coveragePreviousMark());
        }

        @Override
        protected boolean hasNext(int idx, List<Integer> list) {
            return idx > 0;
        }

        @Override
        protected int next(int idx) {
            return idx - 1;
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            super.update(e);
            String nextChange = getNextChange();
            if (nextChange != null) {
                e.getPresentation().setTextValue(ExecutionCoverageLocalize.coveragePreviousPlace(nextChange));
            }
        }
    }

    private class GotoNextCoveredLineAction extends BaseGotoCoveredLineAction {
        public GotoNextCoveredLineAction(Editor editor, int lineNumber) {
            super(editor, lineNumber);
            copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_NEXT_OCCURENCE));
            getTemplatePresentation().setTextValue(ExecutionCoverageLocalize.coverageNextMark());
        }

        @Override
        protected boolean hasNext(int idx, List<Integer> list) {
            return idx < list.size() - 1;
        }

        @Override
        protected int next(int idx) {
            return idx + 1;
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            super.update(e);
            String nextChange = getNextChange();
            if (nextChange != null) {
                e.getPresentation().setTextValue(ExecutionCoverageLocalize.coverageNextPlace(nextChange));
            }
        }
    }

    private abstract class BaseGotoCoveredLineAction extends AnAction {
        private final Editor myEditor;
        private final int myLineNumber;

        public BaseGotoCoveredLineAction(Editor editor, int lineNumber) {
            myEditor = editor;
            myLineNumber = lineNumber;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Integer lineNumber = getLineEntry();
            if (lineNumber != null) {
                moveToLine(lineNumber, myEditor);
            }
        }

        protected abstract boolean hasNext(int idx, List<Integer> list);

        protected abstract int next(int idx);

        @Nullable
        private Integer getLineEntry() {
            ArrayList<Integer> list = new ArrayList<>(myLines.keySet());
            Collections.sort(list);
            LineData data = getLineData(myLineNumber);
            int currentStatus = data != null ? data.getStatus() : LineCoverage.NONE;
            int idx = list.indexOf(myNewToOldConverter != null ? myNewToOldConverter.apply(myLineNumber) : myLineNumber);
            while (hasNext(idx, list)) {
                int index = next(idx);
                LineData lineData = myLines.get(list.get(index));
                idx = index;
                if (lineData != null && lineData.getStatus() != currentStatus) {
                    Integer line = list.get(idx);
                    if (myOldToNewConverter != null) {
                        int newLine = myOldToNewConverter.apply(line);
                        if (newLine != 0) {
                            return newLine;
                        }
                    }
                    else {
                        return line;
                    }
                }
            }
            return null;
        }

        @Nullable
        protected String getNextChange() {
            Integer entry = getLineEntry();
            if (entry != null) {
                LineData lineData = getLineData(entry);
                if (lineData != null) {
                    switch (lineData.getStatus()) {
                        case LineCoverage.NONE:
                            return ExecutionCoverageLocalize.coverageNextChangeUncovered().get();
                        case LineCoverage.PARTIAL:
                            return ExecutionCoverageLocalize.coverageNextChangePartialCovered().get();
                        case LineCoverage.FULL:
                            return ExecutionCoverageLocalize.coverageNextChangeFullyCovered().get();
                    }
                }
            }
            return null;
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getLineEntry() != null);
        }
    }

    private class EditCoverageColorsAction extends AnAction {
        private final Editor myEditor;
        private final int myLineNumber;

        private EditCoverageColorsAction(Editor editor, int lineNumber) {
            super(
                ExecutionCoverageLocalize.coverageEditColorsActionName(),
                ExecutionCoverageLocalize.coverageEditColorsDescription(),
                PlatformIconGroup.generalGearplain()
            );
            myEditor = editor;
            myLineNumber = lineNumber;
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setVisible(getLineData(myLineNumber) != null);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            ColorAndFontOptions colorAndFontOptions = new ColorAndFontOptions() {
                @Override
                protected List<ColorAndFontPanelFactory> createPanelFactories() {
                    final GeneralColorsPage colorsPage = new GeneralColorsPage(Application.get());
                    ColorAndFontPanelFactory panelFactory = new ColorAndFontPanelFactory() {
                        @Nonnull
                        @Override
                        @RequiredUIAccess
                        public NewColorAndFontPanel createPanel(@Nonnull ColorAndFontOptions options) {
                            SimpleEditorPreview preview = new SimpleEditorPreview(options, colorsPage);
                            return NewColorAndFontPanel.create(preview, colorsPage.getDisplayName(), options, null, colorsPage);
                        }

                        @Nonnull
                        @Override
                        public String getPanelDisplayName() {
                            return ExecutionCoverageLocalize.configurableNameEditorColorsPage(
                                getDisplayName(),
                                colorsPage.getDisplayName()
                            ).get();
                        }
                    };
                    return Collections.singletonList(panelFactory);
                }
            };
            Configurable[] configurables = colorAndFontOptions.buildConfigurables();
            try {
                SearchableConfigurable general = colorAndFontOptions.findSubConfigurable(GeneralColorsPage.class);
                if (general != null) {
                    LineData lineData = getLineData(myLineNumber);
                    ShowSettingsUtil.getInstance().editConfigurable(
                        myEditor.getProject(),
                        general,
                        general.enableSearch(getAttributesKey(lineData).getExternalName())
                    );
                }
            }
            finally {
                for (Configurable configurable : configurables) {
                    configurable.disposeUIResources();
                }
                colorAndFontOptions.disposeUIResources();
            }
        }
    }
}
