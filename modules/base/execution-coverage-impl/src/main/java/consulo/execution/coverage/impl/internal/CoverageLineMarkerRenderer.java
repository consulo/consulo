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
package consulo.execution.coverage.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.execution.coverage.CoveragePresentation;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.action.HideCoverageInfoAction;
import consulo.execution.coverage.data.CoverageLine;
import consulo.execution.coverage.data.LineStatus;
import consulo.execution.coverage.impl.internal.action.ShowCoveringTestsAction;
import consulo.execution.coverage.internal.ExecutionCoverageInternal;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ColoredSideBorder;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * @author ven
 */
public class CoverageLineMarkerRenderer implements ActiveGutterRenderer, LineMarkerPresentationProvider {
    private static final int THICKNESS = 8;
    private final TextAttributesKey myKey;
    private final String myClassName;
    private final SortedMap<Integer, CoverageLine> myLines;
    private final boolean myCoverageByTestApplicable;
    private final Int2IntFunction myNewToOldConverter;
    private final Int2IntFunction myOldToNewConverter;
    private final CoverageSuitesBundle myCoverageSuite;
    private final boolean mySubCoverageActive;

    protected CoverageLineMarkerRenderer(
        TextAttributesKey textAttributesKey,
        @Nullable String className,
        SortedMap<Integer, CoverageLine> lines,
        boolean coverageByTestApplicable,
        Int2IntFunction newToOldConverter,
        Int2IntFunction oldToNewConverter,
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
    public Set<EditorGutterArea> getUsedAreas() {
        return Set.of(EditorGutterArea.LEFT_FREE_PAINTERS);
    }

    @Override
    public List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context) {
        TextAttributes attributes = context.getAttributes(myKey);
        ColorValue color = attributes.getBackgroundColor();
        if (color == null) {
            color = attributes.getForegroundColor();
        }
        // line numbers and annotations compete for the same strip, so fade the status behind them
        if (color != null && (context.isLineNumbersShown() || context.isAnnotationsShown())) {
            color = color.withAlpha(0.6f);
        }

        int line = context.startLine();
        CoverageLine lineData = getLineData(line);
        Image icon = lineData != null && lineData.isCoveredBySingleTest() ? PlatformIconGroup.gutterUnique() : null;

        return List.of(new CoveragePresentation(line, context.endLine(), color, icon, line));
    }

    public static CoverageLineMarkerRenderer getRenderer(
        int lineNumber,
        @Nullable String className,
        SortedMap<Integer, CoverageLine> lines,
        boolean coverageByTestApplicable,
        CoverageSuitesBundle coverageSuite,
        Int2IntFunction newToOldConverter,
        Int2IntFunction oldToNewConverter,
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

    public static TextAttributesKey getAttributesKey(int lineNumber, SortedMap<Integer, CoverageLine> lines) {
        return getAttributesKey(lines.get(lineNumber));
    }

    private static TextAttributesKey getAttributesKey(CoverageLine lineData) {
        return CoverageLine.getStatus(lineData).getAttributesKey();
    }

    @Override
    public boolean canDoAction(LineMarkerPresentation presentation, InputDetails details) {
        return presentation instanceof CoveragePresentation;
    }

    @Override
    public @Nullable String getTooltipText() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void doAction(Editor editor, LineMarkerPresentation presentation, InputDetails details) {
        int y = details.getPosition().y();
        JComponent gutter = ((EditorGutterComponentEx) editor.getGutter()).getComponent();
        JRootPane rootPane = gutter.getRootPane();
        if (rootPane == null) {
            return;
        }
        JLayeredPane layeredPane = rootPane.getLayeredPane();
        Point point = SwingUtilities.convertPoint(gutter, THICKNESS, y, layeredPane);
        showHint(editor, point, editor.xyToLogicalPosition(new Point(0, y)).line);
    }

    @Override
    public void doAction(Editor editor, MouseEvent e) {
        // ActiveGutterRenderer is still implemented, so this abstract method must stay. It is now
        // unreachable because canDoAction(MouseEvent) falls back to the default false.
    }

    @RequiredUIAccess
    private void showHint(Editor editor, Point point, int lineNumber) {
        ActionToolbar toolbar = createActionsToolbar(editor, lineNumber);
        long modificationStamp = editor.getDocument().getModificationStamp();
        toolbar.updateActionsAsync().whenCompleteAsync(
            (actions, throwable) -> {
                if (editor.isDisposed() || !editor.getComponent().isShowing()
                    || editor.getDocument().getModificationStamp() != modificationStamp) {
                    return;
                }
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(toolbar.getComponent(), BorderLayout.NORTH);

                CoverageLine lineData = getLineData(lineNumber);
                String reportText = CoverageLine.isSomewhatCovered(lineData) && !mySubCoverageActive ? getReport(editor, lineNumber) : null;
                ExecutionCoverageInternal.getInstance().showCoverageHit(panel, editor, point, lineData, reportText);
            },
            UIAccess.current()
        );
    }

    @RequiredReadAction
    private String getReport(Editor editor, int lineNumber) {
        CoverageLine lineData = getLineData(lineNumber);

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
    protected ActionToolbar createActionsToolbar(Editor editor, int lineNumber) {
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

        CoverageLine lineData = getLineData(lineNumber);
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
        toolbar.setTargetComponent(editor.getComponent());
        JComponent toolbarComponent = toolbar.getComponent();

        ColorValue background = ((EditorEx) editor).getBackgroundColor();
        ColorValue foreground = editor.getColorsScheme().getColor(EditorColors.CARET_COLOR);
        toolbarComponent.setBackground(TargetAWT.to(background));
        Color awtForeground = TargetAWT.to(foreground);
        toolbarComponent.setBorder(new ColoredSideBorder(
            awtForeground,
            awtForeground,
            CoverageLine.isNotCovered(lineData) || mySubCoverageActive ? awtForeground : null,
            awtForeground,
            1
        ));
        return toolbar;
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

    public @Nullable CoverageLine getLineData(int lineNumber) {
        return myLines != null
            ? myLines.get(myNewToOldConverter != null ? myNewToOldConverter.applyAsInt(lineNumber) : lineNumber)
            : null;
    }

    public ColorValue getErrorStripeColor(Editor editor) {
        return editor.getColorsScheme().getAttributes(myKey).getErrorStripeColor();
    }

    @Override
    public Position getPosition() {
        return Position.LEFT;
    }

    private class GotoPreviousCoveredLineAction extends BaseGotoCoveredLineAction {
        public GotoPreviousCoveredLineAction(Editor editor, int lineNumber) {
            super(editor, lineNumber);
            copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_PREVIOUS_OCCURENCE));
            getTemplatePresentation().setText(ExecutionCoverageLocalize.coveragePreviousMark());
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
        public void update(AnActionEvent e) {
            super.update(e);
            LocalizeValue nextChange = getNextChange();
            if (nextChange.isNotEmpty()) {
                e.getPresentation().setText(ExecutionCoverageLocalize.coveragePreviousPlace(nextChange));
            }
        }
    }

    private class GotoNextCoveredLineAction extends BaseGotoCoveredLineAction {
        public GotoNextCoveredLineAction(Editor editor, int lineNumber) {
            super(editor, lineNumber);
            copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_NEXT_OCCURENCE));
            getTemplatePresentation().setText(ExecutionCoverageLocalize.coverageNextMark());
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
        public void update(AnActionEvent e) {
            super.update(e);
            LocalizeValue nextChange = getNextChange();
            if (nextChange.isNotEmpty()) {
                e.getPresentation().setText(ExecutionCoverageLocalize.coverageNextPlace(nextChange));
            }
        }
    }

    private abstract class BaseGotoCoveredLineAction extends LegacyAnAction {
        private final Editor myEditor;
        private final int myLineNumber;

        public BaseGotoCoveredLineAction(Editor editor, int lineNumber) {
            myEditor = editor;
            myLineNumber = lineNumber;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Integer lineNumber = getLineEntry();
            if (lineNumber != null) {
                moveToLine(lineNumber, myEditor);
            }
        }

        protected abstract boolean hasNext(int idx, List<Integer> list);

        protected abstract int next(int idx);

        private @Nullable Integer getLineEntry() {
            List<Integer> list = new ArrayList<>(myLines.keySet());
            Collections.sort(list);
            CoverageLine data = getLineData(myLineNumber);
            LineStatus currentStatus = CoverageLine.getStatus(data);
            int idx = list.indexOf(myNewToOldConverter != null ? myNewToOldConverter.apply(myLineNumber) : myLineNumber);
            while (hasNext(idx, list)) {
                int index = next(idx);
                CoverageLine lineData = myLines.get(list.get(index));
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

        protected LocalizeValue getNextChange() {
            Integer entry = getLineEntry();
            if (entry != null) {
                CoverageLine lineData = getLineData(entry);
                if (lineData != null) {
                    return lineData.getStatus().getDisplayName();
                }
            }
            return LocalizeValue.empty();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getLineEntry() != null);
        }
    }

    private class EditCoverageColorsAction extends LegacyAnAction {
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
        public void update(AnActionEvent e) {
            e.getPresentation().setVisible(getLineData(myLineNumber) != null);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Project project = myEditor.getProject();

            ExecutionCoverageInternal.getInstance()
                .showColorsSettings(project, getLineData(myLineNumber), CoverageLineMarkerRenderer::getAttributesKey);
        }
    }
}
