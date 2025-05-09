// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.hint;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ParameterHintsPresentationManager;
import consulo.ide.impl.idea.ide.IdeTooltip;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.parameterInfo.*;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.Balloon.Position;
import consulo.undoRedo.ProjectUndoManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static consulo.ide.impl.idea.codeInsight.hint.ParameterInfoTaskRunnerUtil.runTask;

public class ParameterInfoController extends UserDataHolderBase implements Disposable {
    private static final Logger LOG = Logger.getInstance(ParameterInfoController.class);
    private static final String WHITESPACE = " \t";

    private static final String LOADING_TAG = "loading";
    private static final String COMPONENT_TAG = "component";

    private final Project myProject;
    @Nonnull
    private final Editor myEditor;

    private final RangeMarker myLbraceMarker;
    private LightweightHintImpl myHint;
    private final ParameterInfoComponent myComponent;
    private boolean myKeepOnHintHidden;

    private final CaretListener myEditorCaretListener;
    @Nonnull
    private final ParameterInfoHandler<PsiElement, Object> myHandler;
    private final MyBestLocationPointProvider myProvider;

    private final Alarm myAlarm = new Alarm();
    private static final int DELAY = 200;

    private boolean mySingleParameterInfo;
    private boolean myDisposed;

    /**
     * Keeps Vector of ParameterInfoController's in Editor
     */
    private static final Key<List<ParameterInfoController>> ALL_CONTROLLERS_KEY = Key.create("ParameterInfoController.ALL_CONTROLLERS_KEY");

    public static ParameterInfoController findControllerAtOffset(Editor editor, int offset) {
        List<ParameterInfoController> allControllers = getAllControllers(editor);
        for (int i = 0; i < allControllers.size(); ++i) {
            ParameterInfoController controller = allControllers.get(i);

            int lbraceOffset = controller.myLbraceMarker.getStartOffset();
            if (lbraceOffset == offset) {
                if (controller.myKeepOnHintHidden || controller.myHint.isVisible() || Application.get().isHeadlessEnvironment()) {
                    return controller;
                }
                Disposer.dispose(controller);
                //noinspection AssignmentToForLoopParameter
                --i;
            }
        }

        return null;
    }

    private static List<ParameterInfoController> getAllControllers(@Nonnull Editor editor) {
        List<ParameterInfoController> array = editor.getUserData(ALL_CONTROLLERS_KEY);
        if (array == null) {
            array = new ArrayList<>();
            editor.putUserData(ALL_CONTROLLERS_KEY, array);
        }
        return array;
    }

    public static boolean existsForEditor(@Nonnull Editor editor) {
        return !getAllControllers(editor).isEmpty();
    }

    public static boolean existsWithVisibleHintForEditor(@Nonnull Editor editor, boolean anyHintType) {
        return getAllControllers(editor).stream().anyMatch(c -> c.isHintShown(anyHintType));
    }

    public boolean isHintShown(boolean anyType) {
        return myHint.isVisible() && (!mySingleParameterInfo || anyType);
    }

    @RequiredUIAccess
    public ParameterInfoController(
        @Nonnull Project project,
        @Nonnull Editor editor,
        int lbraceOffset,
        Object[] descriptors,
        Object highlighted,
        PsiElement parameterOwner,
        @Nonnull ParameterInfoHandler handler,
        boolean showHint,
        boolean requestFocus
    ) {
        myProject = project;
        myEditor = editor;
        myHandler = handler;
        myProvider = new MyBestLocationPointProvider(editor);
        myLbraceMarker = editor.getDocument().createRangeMarker(lbraceOffset, lbraceOffset);
        myComponent = new ParameterInfoComponent(descriptors, editor, handler, requestFocus, true);
        myHint = createHint();
        myKeepOnHintHidden = !showHint;
        mySingleParameterInfo = !showHint;

        myHint.setSelectingHint(true);
        myComponent.setParameterOwner(parameterOwner);
        myComponent.setHighlightedParameter(highlighted);

        List<ParameterInfoController> allControllers = getAllControllers(myEditor);
        allControllers.add(this);

        myEditorCaretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(@Nonnull CaretEvent e) {
                if (!ProjectUndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
                    syncUpdateOnCaretMove();
                    rescheduleUpdate();
                }
            }
        };
        myEditor.getCaretModel().addCaretListener(myEditorCaretListener);

        myEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@Nonnull DocumentEvent e) {
                rescheduleUpdate();
            }
        }, this);

        MessageBusConnection connection = project.getMessageBus().connect(this);
        //connection.subscribe(ExternalParameterInfoChangesProvider.TOPIC, (e, offset) -> {
        //  if (e != null && (e != myEditor || myLbraceMarker.getStartOffset() != offset)) return;
        //  updateWhenAllCommitted();
        //});

        PropertyChangeListener lookupListener = evt -> {
            if (LookupManager.PROP_ACTIVE_LOOKUP.equals(evt.getPropertyName())) {
                Lookup lookup = (Lookup)evt.getNewValue();
                if (lookup != null) {
                    adjustPositionForLookup(lookup);
                }
            }
        };
        LookupManager.getInstance(project).addPropertyChangeListener(lookupListener, this);
        EditorUtil.disposeWithEditor(myEditor, this);

        if (showHint) {
            showHint(requestFocus, mySingleParameterInfo);
        }
        else {
            updateComponent();
        }
    }

    void setDescriptors(Object[] descriptors) {
        myComponent.setDescriptors(descriptors);
    }

    private void syncUpdateOnCaretMove() {
        myHandler.syncUpdateOnCaretMove(new MyLazyUpdateParameterInfoContext());
    }

    private LightweightHintImpl createHint() {
        JPanel wrapper = new WrapperPanel();
        wrapper.add(myComponent);
        return new LightweightHintImpl(wrapper);
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }
        myDisposed = true;
        hideHint();
        myHandler.dispose(new MyDeleteParameterInfoContext());
        List<ParameterInfoController> allControllers = getAllControllers(myEditor);
        allControllers.remove(this);
        myEditor.getCaretModel().removeCaretListener(myEditorCaretListener);
    }

    @RequiredUIAccess
    public void showHint(boolean requestFocus, boolean singleParameterInfo) {
        if (myHint.isVisible()) {
            JComponent myHintComponent = myHint.getComponent();
            myHintComponent.removeAll();
            hideHint();
            myHint = createHint();
        }

        mySingleParameterInfo = singleParameterInfo && myKeepOnHintHidden;

        int caretOffset = myEditor.getCaretModel().getOffset();
        Pair<Point, Short> pos =
            myProvider.getBestPointPosition(myHint, myComponent.getParameterOwner(), caretOffset, null, HintManager.ABOVE);
        HintHint hintHint = HintManagerImpl.getInstanceImpl().createHintHint(myEditor, pos.getFirst(), myHint, pos.getSecond());
        hintHint.setExplicitClose(true);
        hintHint.setRequestFocus(requestFocus);
        hintHint.setShowImmediately(true);
        hintHint.setBorderColor(ParameterInfoComponent.BORDER_COLOR);
        hintHint.setBorderInsets(JBUI.insets(4, 1, 4, 1));
        hintHint.setComponentBorder(JBUI.Borders.empty());

        int flags = HintManager.HIDE_BY_ESCAPE | HintManager.UPDATE_BY_SCROLLING;
        if (!singleParameterInfo && myKeepOnHintHidden) {
            flags |= HintManager.HIDE_BY_TEXT_CHANGE;
        }

        Editor editorToShow = myEditor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : myEditor;

        //update presentation of descriptors synchronously
        myComponent.update(mySingleParameterInfo);

        // is case of injection we need to calculate position for EditorWindow
        // also we need to show the hint in the main editor because of intention bulb
        HintManagerImpl.getInstanceImpl().showEditorHint(myHint, editorToShow, pos.getFirst(), flags, 0, false, hintHint);

        updateComponent();
    }

    private void adjustPositionForLookup(@Nonnull Lookup lookup) {
        if (myEditor.isDisposed()) {
            Disposer.dispose(this);
            return;
        }

        if (!myHint.isVisible()) {
            if (!myKeepOnHintHidden) {
                Disposer.dispose(this);
            }
            return;
        }

        IdeTooltip tooltip = myHint.getCurrentIdeTooltip();
        if (tooltip != null) {
            JRootPane root = myEditor.getComponent().getRootPane();
            if (root != null) {
                Point p = tooltip.getShowingPoint().getPoint(root.getLayeredPane());
                if (lookup.isPositionedAboveCaret()) {
                    if (Position.above == tooltip.getPreferredPosition()) {
                        myHint.pack();
                        myHint.updatePosition(Position.below);
                        myHint.updateLocation(p.x, p.y + tooltip.getPositionChangeY());
                    }
                }
                else {
                    if (Position.below == tooltip.getPreferredPosition()) {
                        myHint.pack();
                        myHint.updatePosition(Position.above);
                        myHint.updateLocation(p.x, p.y - tooltip.getPositionChangeY());
                    }
                }
            }
        }
    }

    private void rescheduleUpdate() {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(this::updateWhenAllCommitted, DELAY, Application.get().getModalityStateForComponent(myEditor.getComponent()));
    }

    private void updateWhenAllCommitted() {
        if (!myDisposed && !myProject.isDisposed()) {
            PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
                try {
                    DumbService.getInstance(myProject).withAlternativeResolveEnabled(this::updateComponent);
                }
                catch (IndexNotReadyException e) {
                    LOG.info(e);
                    Disposer.dispose(this);
                }
            });
        }
    }

    public void updateComponent() {
        if (!myKeepOnHintHidden && !myHint.isVisible() && !Application.get().isHeadlessEnvironment()
            || myEditor instanceof EditorWindow editorWindow && !editorWindow.isValid()) {
            Disposer.dispose(this);
            return;
        }

        PsiFile file = PsiUtilBase.getPsiFileInEditor(myEditor, myProject);
        int caretOffset = myEditor.getCaretModel().getOffset();
        int offset = getCurrentOffset();
        MyUpdateParameterInfoContext context = new MyUpdateParameterInfoContext(offset, file);
        executeFindElementForUpdatingParameterInfo(
            context,
            elementForUpdating -> {
                myHandler.processFoundElementForUpdatingParameterInfo(elementForUpdating, context);
                if (elementForUpdating != null) {
                    executeUpdateParameterInfo(
                        elementForUpdating,
                        context,
                        () -> {
                            boolean knownParameter = (myComponent.getObjects().length == 1 || myComponent.getHighlighted() != null)
                                && myComponent.getCurrentParameterIndex() != -1;
                            if (mySingleParameterInfo && !knownParameter && myHint.isVisible()) {
                                hideHint();
                            }
                            if (myKeepOnHintHidden && knownParameter && !myHint.isVisible()) {
                                AutoPopupController.getInstance(myProject).autoPopupParameterInfo(myEditor, null);
                            }
                            if (!myDisposed && (myHint.isVisible() && !myEditor.isDisposed()
                                && (myEditor.getComponent().getRootPane() != null || Application.get().isUnitTestMode())
                                || Application.get().isHeadlessEnvironment())) {
                                Model result = myComponent.update(mySingleParameterInfo);
                                result.project = myProject;
                                result.range = myComponent.getParameterOwner().getTextRange();
                                result.editor = myEditor;
                                //for (ParameterInfoListener listener : ParameterInfoListener.EP_NAME.getExtensionList()) {
                                //    listener.hintUpdated(result);
                                //}
                                if (Application.get().isHeadlessEnvironment()) {
                                    return;
                                }
                                IdeTooltip tooltip = myHint.getCurrentIdeTooltip();
                                short position = tooltip != null ? toShort(tooltip.getPreferredPosition()) : HintManager.ABOVE;
                                Pair<Point, Short> pos = myProvider.getBestPointPosition(
                                    myHint,
                                    elementForUpdating,
                                    caretOffset,
                                    myEditor.getCaretModel().getVisualPosition(),
                                    position
                                );
                                HintManagerImpl.getInstanceImpl()
                                    .adjustEditorHintPosition(myHint, myEditor, pos.getFirst(), pos.getSecond());
                            }
                        }
                    );
                }
                else {
                    hideHint();
                    if (!myKeepOnHintHidden) {
                        Disposer.dispose(this);
                    }
                }
            }
        );
    }

    private int getCurrentOffset() {
        int caretOffset = myEditor.getCaretModel().getOffset();
        CharSequence chars = myEditor.getDocument().getCharsSequence();
        return myHandler.isWhitespaceSensitive() ? caretOffset : CharArrayUtil.shiftBackward(chars, caretOffset - 1, WHITESPACE) + 1;
    }

    private void executeFindElementForUpdatingParameterInfo(
        UpdateParameterInfoContext context,
        @Nonnull Consumer<PsiElement> elementForUpdatingConsumer
    ) {
        runTask(
            myProject,
            ReadAction.nonBlocking(() -> myHandler.findElementForUpdatingParameterInfo(context))
                .withDocumentsCommitted(myProject)
                .expireWhen(() -> getCurrentOffset() != context.getOffset())
                .coalesceBy(this)
                .expireWith(this),
            elementForUpdatingConsumer,
            null,
            myEditor
        );
    }

    private void executeUpdateParameterInfo(PsiElement elementForUpdating, MyUpdateParameterInfoContext context, Runnable continuation) {
        PsiElement parameterOwner = context.getParameterOwner();
        if (parameterOwner != null && !parameterOwner.equals(elementForUpdating)) {
            context.removeHint();
            return;
        }

        runTask(
            myProject,
            ReadAction.nonBlocking(() -> {
                try {
                    myHandler.updateParameterInfo(elementForUpdating, context);
                    return elementForUpdating;
                }
                catch (IndexNotReadyException e) {
                    DumbService.getInstance(myProject).showDumbModeNotification(
                        CodeInsightLocalize.parameterInfoIndexingModeNotSupported().get()
                    );
                }
                return null;
            }).withDocumentsCommitted(myProject).expireWhen(
                () -> !myKeepOnHintHidden && !myHint.isVisible() && !Application.get().isHeadlessEnvironment() ||
                    getCurrentOffset() != context.getOffset() ||
                    !elementForUpdating.isValid()
            ).expireWith(this),
            element -> {
                if (element != null && continuation != null) {
                    context.applyUIChanges();
                    continuation.run();
                }
            },
            null,
            myEditor
        );
    }

    @HintManager.PositionFlags
    private static short toShort(Position position) {
        switch (position) {
            case above:
                return HintManager.ABOVE;
            case atLeft:
                return HintManager.LEFT;
            case atRight:
                return HintManager.RIGHT;
            default:
                return HintManager.UNDER;
        }
    }

    @RequiredReadAction
    static boolean hasPrevOrNextParameter(Editor editor, int lbraceOffset, boolean isNext) {
        ParameterInfoController controller = findControllerAtOffset(editor, lbraceOffset);
        return controller != null && controller.getPrevOrNextParameterOffset(isNext) != -1;
    }

    @RequiredReadAction
    static void prevOrNextParameter(Editor editor, int lbraceOffset, boolean isNext) {
        ParameterInfoController controller = findControllerAtOffset(editor, lbraceOffset);
        int newOffset = controller != null ? controller.getPrevOrNextParameterOffset(isNext) : -1;
        if (newOffset != -1) {
            controller.moveToParameterAtOffset(newOffset);
        }
    }

    private void moveToParameterAtOffset(int offset) {
        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        PsiElement argsList = findArgumentList(file, offset, -1);
        if (argsList == null && !CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION) {
            return;
        }

        if (!myHint.isVisible()) {
            AutoPopupController.getInstance(myProject).autoPopupParameterInfo(myEditor, null);
        }

        offset = adjustOffsetToInlay(offset);
        myEditor.getCaretModel().moveToOffset(offset);
        myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        myEditor.getSelectionModel().removeSelection();
        if (argsList != null) {
            executeUpdateParameterInfo(argsList, new MyUpdateParameterInfoContext(offset, file), null);
        }
    }

    private int adjustOffsetToInlay(int offset) {
        CharSequence text = myEditor.getDocument().getImmutableCharSequence();
        int hostWhitespaceStart = CharArrayUtil.shiftBackward(text, offset, WHITESPACE) + 1;
        int hostWhitespaceEnd = CharArrayUtil.shiftForward(text, offset, WHITESPACE);
        Editor hostEditor = myEditor;
        if (myEditor instanceof EditorWindow editorWindow) {
            hostEditor = editorWindow.getDelegate();
            hostWhitespaceStart = editorWindow.getDocument().injectedToHost(hostWhitespaceStart);
            hostWhitespaceEnd = editorWindow.getDocument().injectedToHost(hostWhitespaceEnd);
        }
        List<Inlay> inlays =
            ParameterHintsPresentationManager.getInstance().getParameterHintsInRange(hostEditor, hostWhitespaceStart, hostWhitespaceEnd);
        for (Inlay inlay : inlays) {
            int inlayOffset = inlay.getOffset();
            if (myEditor instanceof EditorWindow editorWindow) {
                if (editorWindow.getDocument().getHostRange(inlayOffset) == null) {
                    continue;
                }
                inlayOffset = editorWindow.getDocument().hostToInjected(inlayOffset);
            }
            return inlayOffset;
        }
        return offset;
    }

    @RequiredReadAction
    private int getPrevOrNextParameterOffset(boolean isNext) {
        if (!(myHandler instanceof ParameterInfoHandlerWithTabActionSupport handler)) {
            return -1;
        }
        IElementType delimiter = handler.getActualParameterDelimiterType();
        boolean noDelimiter = delimiter == TokenType.WHITE_SPACE;
        int caretOffset = myEditor.getCaretModel().getOffset();
        CharSequence text = myEditor.getDocument().getImmutableCharSequence();
        int offset = noDelimiter ? caretOffset : CharArrayUtil.shiftBackward(text, caretOffset - 1, WHITESPACE) + 1;
        int lbraceOffset = myLbraceMarker.getStartOffset();
        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(myEditor.getDocument());
        PsiElement argList = lbraceOffset < offset ? findArgumentList(file, offset, lbraceOffset) : null;
        if (argList == null) {
            return -1;
        }

        @SuppressWarnings("unchecked") PsiElement[] parameters = handler.getActualParameters(argList);
        int currentParameterIndex = getParameterIndex(parameters, delimiter, offset);
        if (CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION) {
            if (currentParameterIndex < 0 || currentParameterIndex >= parameters.length && parameters.length > 0) {
                return -1;
            }
            if (offset >= argList.getTextRange().getEndOffset()) {
                currentParameterIndex = isNext ? -1 : parameters.length;
            }
            int prevOrNextParameterIndex = currentParameterIndex + (isNext ? 1 : -1);
            if (prevOrNextParameterIndex < 0 || prevOrNextParameterIndex >= parameters.length) {
                PsiElement parameterOwner = myComponent.getParameterOwner();
                return parameterOwner != null && parameterOwner.isValid() ? parameterOwner.getTextRange().getEndOffset() : -1;
            }
            else {
                return getParameterNavigationOffset(parameters[prevOrNextParameterIndex], text);
            }
        }
        else {
            int prevOrNextParameterIndex =
                isNext && currentParameterIndex < parameters.length - 1 ? currentParameterIndex + 1 : !isNext && currentParameterIndex > 0 ? currentParameterIndex - 1 : -1;
            return prevOrNextParameterIndex != -1 ? parameters[prevOrNextParameterIndex].getTextRange().getStartOffset() : -1;
        }
    }

    @RequiredReadAction
    private static int getParameterIndex(@Nonnull PsiElement[] parameters, @Nonnull IElementType delimiter, int offset) {
        for (int i = 0; i < parameters.length; i++) {
            PsiElement parameter = parameters[i];
            TextRange textRange = parameter.getTextRange();
            int startOffset = textRange.getStartOffset();
            if (offset < startOffset) {
                if (i == 0) {
                    return 0;
                }
                PsiElement elementInBetween = parameters[i - 1];
                int currOffset = elementInBetween.getTextRange().getEndOffset();
                while ((elementInBetween = PsiTreeUtil.nextLeaf(elementInBetween)) != null) {
                    if (currOffset >= startOffset) {
                        break;
                    }
                    ASTNode node = elementInBetween.getNode();
                    if (node != null && node.getElementType() == delimiter) {
                        return offset <= currOffset ? i - 1 : i;
                    }
                    currOffset += elementInBetween.getTextLength();
                }
                return i;
            }
            else if (offset <= textRange.getEndOffset()) {
                return i;
            }
        }
        return Math.max(0, parameters.length - 1);
    }

    @RequiredReadAction
    private static int getParameterNavigationOffset(@Nonnull PsiElement parameter, @Nonnull CharSequence text) {
        int rangeStart = parameter.getTextRange().getStartOffset();
        int rangeEnd = parameter.getTextRange().getEndOffset();
        int offset = CharArrayUtil.shiftBackward(text, rangeEnd - 1, WHITESPACE) + 1;
        return offset > rangeStart ? offset : CharArrayUtil.shiftForward(text, rangeEnd, WHITESPACE);
    }

    @Nullable
    @RequiredReadAction
    public static <E extends PsiElement> E findArgumentList(PsiFile file, int offset, int lbraceOffset) {
        if (file == null) {
            return null;
        }
        ParameterInfoHandler[] handlers = ShowParameterInfoHandler.getHandlers(
            file.getProject(),
            PsiUtilCore.getLanguageAtOffset(file, offset),
            file.getViewProvider().getBaseLanguage()
        );

        if (handlers != null) {
            for (ParameterInfoHandler handler : handlers) {
                if (handler instanceof ParameterInfoHandlerWithTabActionSupport parameterInfoHandler2) {
                    E e = ParameterInfoUtils.findArgumentList(file, offset, lbraceOffset, parameterInfoHandler2);
                    if (e != null) {
                        return e;
                    }
                }
            }
        }

        return null;
    }

    public Object[] getObjects() {
        return myComponent.getObjects();
    }

    public Object getHighlighted() {
        return myComponent.getHighlighted();
    }

    public void setPreservedOnHintHidden(boolean value) {
        myKeepOnHintHidden = value;
    }

    @TestOnly
    public static void waitForDelayedActions(@Nonnull Editor editor, long timeout, @Nonnull TimeUnit unit) throws TimeoutException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            List<ParameterInfoController> controllers = getAllControllers(editor);
            boolean hasPendingRequests = false;
            for (ParameterInfoController controller : controllers) {
                if (!controller.myAlarm.isEmpty()) {
                    hasPendingRequests = true;
                    break;
                }
            }
            if (hasPendingRequests) {
                LockSupport.parkNanos(10_000_000);
                UIUtil.dispatchAllInvocationEvents();
            }
            else {
                return;
            }

        }
        throw new TimeoutException();
    }

    /**
     * Returned Point is in layered pane coordinate system.
     * Second value is a {@link HintManager.PositionFlags position flag}.
     */
    @RequiredUIAccess
    static Pair<Point, Short> chooseBestHintPosition(
        Editor editor,
        VisualPosition pos,
        LightweightHintImpl hint,
        short preferredPosition,
        boolean showLookupHint
    ) {
        if (Application.get().isUnitTestMode() || Application.get().isHeadlessEnvironment()) {
            return Pair.pair(new Point(), HintManager.DEFAULT);
        }

        HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
        Dimension hintSize = hint.getComponent().getPreferredSize();
        JComponent editorComponent = editor.getComponent();
        JLayeredPane layeredPane = editorComponent.getRootPane().getLayeredPane();

        Point p1;
        Point p2;
        if (showLookupHint) {
            p1 = hintManager.getHintPosition(hint, editor, HintManager.UNDER);
            p2 = hintManager.getHintPosition(hint, editor, HintManager.ABOVE);
        }
        else {
            p1 = HintManagerImpl.getInstanceImpl().getHintPosition(hint, editor, pos, HintManager.UNDER);
            p2 = HintManagerImpl.getInstanceImpl().getHintPosition(hint, editor, pos, HintManager.ABOVE);
        }

        boolean p1Ok = p1.y + hintSize.height < layeredPane.getHeight();
        boolean p2Ok = p2.y >= 0;

        if (!showLookupHint) {
            if (preferredPosition != HintManager.DEFAULT) {
                if (preferredPosition == HintManager.ABOVE) {
                    if (p2Ok) {
                        return new Pair<>(p2, HintManager.ABOVE);
                    }
                }
                else if (preferredPosition == HintManager.UNDER) {
                    if (p1Ok) {
                        return new Pair<>(p1, HintManager.UNDER);
                    }
                }
            }
        }
        if (p1Ok) {
            return new Pair<>(p1, HintManager.UNDER);
        }
        if (p2Ok) {
            return new Pair<>(p2, HintManager.ABOVE);
        }

        int underSpace = layeredPane.getHeight() - p1.y;
        int aboveSpace = p2.y;
        return aboveSpace > underSpace ? new Pair<>(new Point(p2.x, 0), HintManager.UNDER) : new Pair<>(p1, HintManager.ABOVE);
    }

    public static boolean areParameterTemplatesEnabledOnCompletion() {
        return Registry.is("java.completion.argument.live.template") && !CodeInsightSettings.getInstance().SHOW_PARAMETER_NAME_HINTS_ON_COMPLETION;
    }

    private class MyUpdateParameterInfoContext implements UpdateParameterInfoContext {
        private final int myOffset;
        private final PsiFile myFile;
        private final boolean[] enabled;

        MyUpdateParameterInfoContext(int offset, PsiFile file) {
            myOffset = offset;
            myFile = file;

            enabled = new boolean[getObjects().length];
            for (int i = 0; i < enabled.length; i++) {
                enabled[i] = myComponent.isEnabled(i);
            }
        }

        @Override
        public int getParameterListStart() {
            return myLbraceMarker.getStartOffset();
        }

        @Override
        public int getOffset() {
            return myOffset;
        }

        @Override
        public Project getProject() {
            return myProject;
        }

        @Override
        public PsiFile getFile() {
            return myFile;
        }

        @Override
        @Nonnull
        public Editor getEditor() {
            return myEditor;
        }

        @Override
        public void removeHint() {
            Application.get().invokeLater(() -> {
                if (!myHint.isVisible()) {
                    return;
                }

                hideHint();
                if (!myKeepOnHintHidden) {
                    Disposer.dispose(ParameterInfoController.this);
                }
            });
        }

        @Override
        public void setParameterOwner(PsiElement o) {
            myComponent.setParameterOwner(o);
        }

        @Override
        public PsiElement getParameterOwner() {
            return myComponent.getParameterOwner();
        }

        @Override
        public void setHighlightedParameter(Object method) {
            myComponent.setHighlightedParameter(method);
        }

        @Override
        public Object getHighlightedParameter() {
            return myComponent.getHighlighted();
        }

        @Override
        public void setCurrentParameter(int index) {
            myComponent.setCurrentParameterIndex(index);
        }

        @Override
        public boolean isUIComponentEnabled(int index) {
            return enabled[index];
        }

        @Override
        public void setUIComponentEnabled(int index, boolean enabled) {
            this.enabled[index] = enabled;
        }

        @Override
        public Object[] getObjectsToView() {
            return myComponent.getObjects();
        }

        @Override
        public boolean isPreservedOnHintHidden() {
            return myKeepOnHintHidden;
        }

        @Override
        public void setPreservedOnHintHidden(boolean value) {
            myKeepOnHintHidden = value;
        }

        @Override
        @RequiredReadAction
        public boolean isInnermostContext() {
            PsiElement ourOwner = myComponent.getParameterOwner();
            if (ourOwner == null || !ourOwner.isValid()) {
                return false;
            }
            TextRange ourRange = ourOwner.getTextRange();
            if (ourRange == null) {
                return false;
            }
            List<ParameterInfoController> allControllers = getAllControllers(myEditor);
            for (ParameterInfoController controller : allControllers) {
                if (controller != ParameterInfoController.this) {
                    PsiElement parameterOwner = controller.myComponent.getParameterOwner();
                    if (parameterOwner != null && parameterOwner.isValid()) {
                        TextRange range = parameterOwner.getTextRange();
                        if (range != null && range.contains(myOffset) && ourRange.contains(range)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public boolean isSingleParameterInfo() {
            return mySingleParameterInfo;
        }

        @Override
        public UserDataHolderEx getCustomContext() {
            return ParameterInfoController.this;
        }

        @RequiredUIAccess
        void applyUIChanges() {
            UIAccess.assertIsUIThread();

            for (int index = 0, len = enabled.length; index < len; index++) {
                if (enabled[index] != myComponent.isEnabled(index)) {
                    myComponent.setEnabled(index, enabled[index]);
                }
            }
        }
    }

    private class MyLazyUpdateParameterInfoContext extends MyUpdateParameterInfoContext {
        private PsiFile myFile;

        private MyLazyUpdateParameterInfoContext() {
            super(myEditor.getCaretModel().getOffset(), null);
        }

        @Override
        public PsiFile getFile() {
            if (myFile == null) {
                myFile = PsiUtilBase.getPsiFileInEditor(myEditor, myProject);
            }
            return myFile;
        }
    }

    protected void hideHint() {
        myHint.hide();
        //for (ParameterInfoListener listener : ParameterInfoListener.EP_NAME.getExtensionList()) {
        //  listener.hintHidden(myProject);
        //}
    }

    public interface SignatureItemModel {
    }

    public static class RawSignatureItem implements SignatureItemModel {
        public final String htmlText;

        RawSignatureItem(String htmlText) {
            this.htmlText = htmlText;
        }
    }

    public static class SignatureItem implements SignatureItemModel {
        public final String text;
        public final boolean deprecated;
        public final boolean disabled;
        public final List<Integer> startOffsets;
        public final List<Integer> endOffsets;

        SignatureItem(String text, boolean deprecated, boolean disabled, List<Integer> startOffsets, List<Integer> endOffsets) {
            this.text = text;
            this.deprecated = deprecated;
            this.disabled = disabled;
            this.startOffsets = startOffsets;
            this.endOffsets = endOffsets;
        }
    }

    public static class Model {
        public final List<SignatureItemModel> signatures = new ArrayList<>();
        public int current = -1;
        public int highlightedSignature = -1;
        public TextRange range;
        public Editor editor;
        public Project project;
    }

    private static class MyBestLocationPointProvider {
        private final Editor myEditor;
        private int previousOffset = -1;
        private Point previousBestPoint;
        private Short previousBestPosition;

        MyBestLocationPointProvider(Editor editor) {
            myEditor = editor;
        }

        @Nonnull
        @RequiredUIAccess
        private Pair<Point, Short> getBestPointPosition(
            LightweightHintImpl hint,
            PsiElement list,
            int offset,
            VisualPosition pos,
            short preferredPosition
        ) {
            if (list != null) {
                TextRange range = list.getTextRange();
                TextRange rangeWithoutParens = TextRange.from(range.getStartOffset() + 1, Math.max(range.getLength() - 2, 0));
                if (!rangeWithoutParens.contains(offset)) {
                    offset = offset < rangeWithoutParens.getStartOffset()
                        ? rangeWithoutParens.getStartOffset()
                        : rangeWithoutParens.getEndOffset();
                    pos = null;
                }
            }
            if (previousOffset == offset) {
                return Pair.create(previousBestPoint, previousBestPosition);
            }

            boolean isMultiline = list != null && StringUtil.containsAnyChar(list.getText(), "\n\r");
            if (pos == null) {
                pos = EditorUtil.inlayAwareOffsetToVisualPosition(myEditor, offset);
            }
            Pair<Point, Short> position;

            if (!isMultiline) {
                position = chooseBestHintPosition(myEditor, pos, hint, preferredPosition, false);
            }
            else {
                Point p = HintManagerImpl.getInstanceImpl().getHintPosition(hint, myEditor, pos, HintManager.ABOVE);
                position = new Pair<>(p, HintManager.ABOVE);
            }
            previousBestPoint = position.getFirst();
            previousBestPosition = position.getSecond();
            previousOffset = offset;
            return position;
        }
    }

    private static class WrapperPanel extends JPanel {
        WrapperPanel() {
            super(new BorderLayout());
            setBorder(JBUI.Borders.empty());
        }

        // foreground/background/font are used to style the popup (HintManagerImpl.createHintHint)
        @Override
        public Color getForeground() {
            return getComponentCount() == 0 ? super.getForeground() : getComponent(0).getForeground();
        }

        @Override
        public Color getBackground() {
            return getComponentCount() == 0 ? super.getBackground() : getComponent(0).getBackground();
        }

        @Override
        public Font getFont() {
            return getComponentCount() == 0 ? super.getFont() : getComponent(0).getFont();
        }

        // for test purposes
        @Override
        public String toString() {
            return getComponentCount() == 0 ? "<empty>" : getComponent(0).toString();
        }
    }

    private class MyDeleteParameterInfoContext implements DeleteParameterInfoContext {
        @Override
        public PsiElement getParameterOwner() {
            return myComponent.getParameterOwner();
        }

        @Override
        public Editor getEditor() {
            return myEditor;
        }

        @Override
        public UserDataHolderEx getCustomContext() {
            return ParameterInfoController.this;
        }
    }
}
