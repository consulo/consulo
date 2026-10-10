// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.parameterInfo;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.util.EditorUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.internal.inlay.param.ParameterHintRenderer;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.parameterInfo.*;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.undoRedo.ProjectUndoManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static consulo.language.editor.impl.internal.parameterInfo.ParameterInfoTaskRunnerUtil.runTask;
import consulo.language.editor.internal.parameterInfo.ParameterHandlerPopupProxy;
import consulo.language.editor.internal.parameterInfo.ParameterHandlerPopupProxyFactory;
import consulo.language.editor.internal.parameterInfo.ParameterInfoAnchor;
import consulo.language.editor.internal.parameterInfo.ParameterInfoModel;

public class ParameterInfoController extends UserDataHolderBase implements Disposable {
    private static final Logger LOG = Logger.getInstance(ParameterInfoController.class);
    private static final String WHITESPACE = " \t";

    private static final String LOADING_TAG = "loading";
    private static final String COMPONENT_TAG = "component";

    private final Project myProject;

    private final Editor myEditor;

    private final RangeMarker myLbraceMarker;
    private final ParameterHandlerPopupProxy myPopup;
    private final ParameterInfoState myState;
    private boolean myKeepOnHintHidden;

    private final CaretListener myEditorCaretListener;

    private final ParameterInfoHandler<PsiElement, Object> myHandler;

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
                if (controller.myKeepOnHintHidden || controller.myPopup.isVisible() || Application.get().isHeadlessEnvironment()) {
                    return controller;
                }
                Disposer.dispose(controller);
                //noinspection AssignmentToForLoopParameter
                --i;
            }
        }

        return null;
    }

    private static List<ParameterInfoController> getAllControllers(Editor editor) {
        List<ParameterInfoController> array = editor.getUserData(ALL_CONTROLLERS_KEY);
        if (array == null) {
            array = new ArrayList<>();
            editor.putUserData(ALL_CONTROLLERS_KEY, array);
        }
        return array;
    }

    public static boolean existsForEditor(Editor editor) {
        return !getAllControllers(editor).isEmpty();
    }

    public static boolean existsWithVisibleHintForEditor(Editor editor, boolean anyHintType) {
        return getAllControllers(editor).stream().anyMatch(c -> c.isHintShown(anyHintType));
    }

    @RequiredUIAccess
    public static boolean hideOnEscape(Editor editor) {
        boolean hidden = false;
        for (ParameterInfoController controller : new ArrayList<>(getAllControllers(editor))) {
            if (controller.myPopup.isVisible()) {
                controller.hideHint();
                if (!controller.myKeepOnHintHidden) {
                    Disposer.dispose(controller);
                }
                hidden = true;
            }
        }
        return hidden;
    }

    public boolean isHintShown(boolean anyType) {
        return myPopup.isVisible() && (!mySingleParameterInfo || anyType);
    }

    @RequiredUIAccess
    public ParameterInfoController(
        Project project,
        Editor editor,
        int lbraceOffset,
        Object[] descriptors,
        Object highlighted,
        PsiElement parameterOwner,
        ParameterInfoHandler handler,
        boolean showHint,
        boolean requestFocus
    ) {
        myProject = project;
        myEditor = editor;
        myHandler = handler;
        myLbraceMarker = editor.getDocument().createRangeMarker(lbraceOffset, lbraceOffset);
        myState = new ParameterInfoState(descriptors);
        myPopup = project.getApplication().getInstance(ParameterHandlerPopupProxyFactory.class).create(editor);
        myKeepOnHintHidden = !showHint;
        mySingleParameterInfo = !showHint;

        myState.setParameterOwner(parameterOwner);
        myState.setHighlighted(highlighted);

        List<ParameterInfoController> allControllers = getAllControllers(myEditor);
        allControllers.add(this);

        myEditorCaretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent e) {
                if (!ProjectUndoManager.getInstance(myProject).isUndoOrRedoInProgress()) {
                    syncUpdateOnCaretMove();
                    rescheduleUpdate();
                }
            }
        };
        myEditor.getCaretModel().addCaretListener(myEditorCaretListener);

        myEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent e) {
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
                Lookup lookup = (Lookup) evt.getNewValue();
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
        myState.setObjects(descriptors);
    }

    private void syncUpdateOnCaretMove() {
        myHandler.syncUpdateOnCaretMove(new MyLazyUpdateParameterInfoContext());
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
        if (myPopup.isVisible()) {
            hideHint();
        }

        mySingleParameterInfo = singleParameterInfo && myKeepOnHintHidden;

        boolean hideByTextChange = !singleParameterInfo && myKeepOnHintHidden;

        PsiElement parameterOwner = myState.getParameterOwner();
        ParameterInfoAnchor anchor = parameterOwner == null ? null : ReadAction.compute(() -> anchorOf(parameterOwner));

        myPopup.show(buildModel(), anchor, requestFocus, hideByTextChange);

        updateComponent();
    }

    @RequiredUIAccess
    private ParameterInfoModel buildModel() {
        LocalizeValue switchHint = getSwitchHint();
        return ReadAction.compute(() -> myState.buildModel(myHandler, mySingleParameterInfo, switchHint));
    }

    private LocalizeValue getSwitchHint() {
        if (myEditor instanceof EditorWindow || myState.getObjects().length <= 1 || !myHandler.supportsOverloadSwitching()) {
            return LocalizeValue.empty();
        }

        String upShortcut = KeymapUtil.getFirstKeyboardShortcutText(IdeActions.ACTION_METHOD_OVERLOAD_SWITCH_UP);
        String downShortcut = KeymapUtil.getFirstKeyboardShortcutText(IdeActions.ACTION_METHOD_OVERLOAD_SWITCH_DOWN);
        if (upShortcut.isEmpty() && downShortcut.isEmpty()) {
            return LocalizeValue.empty();
        }

        if (upShortcut.isEmpty() || downShortcut.isEmpty()) {
            return CodeInsightLocalize.parameterInfoSwitchOverloadShortcutsSingle(upShortcut.isEmpty() ? downShortcut : upShortcut);
        }
        return CodeInsightLocalize.parameterInfoSwitchOverloadShortcuts(upShortcut, downShortcut);
    }

    @RequiredReadAction
    private static ParameterInfoAnchor anchorOf(PsiElement element) {
        return new ParameterInfoAnchor(element.getTextRange(), StringUtil.containsAnyChar(element.getText(), "\n\r"));
    }

    @RequiredUIAccess
    private void adjustPositionForLookup(Lookup lookup) {
        if (myEditor.isDisposed()) {
            Disposer.dispose(this);
            return;
        }

        if (!myPopup.isVisible()) {
            if (!myKeepOnHintHidden) {
                Disposer.dispose(this);
            }
            return;
        }

        myPopup.adjustForLookup(lookup);
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
        if (!myKeepOnHintHidden && !myPopup.isVisible() && !Application.get().isHeadlessEnvironment()
            || myEditor instanceof EditorWindow editorWindow && !editorWindow.isValid()) {
            Disposer.dispose(this);
            return;
        }

        PsiFile file = PsiUtilBase.getPsiFileInEditor(myEditor, myProject);
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
                        anchor -> {
                            boolean knownParameter = (myState.getObjects().length == 1 || myState.getHighlighted() != null)
                                && myState.getCurrentParameterIndex() != -1;
                            if (mySingleParameterInfo && !knownParameter && myPopup.isVisible()) {
                                hideHint();
                            }
                            if (myKeepOnHintHidden && knownParameter && !myPopup.isVisible()) {
                                AutoPopupController.getInstance(myProject).autoPopupParameterInfo(myEditor, null);
                            }
                            if (!myDisposed && (myPopup.isVisible() && !myEditor.isDisposed() || Application.get().isHeadlessEnvironment())) {
                                ParameterInfoModel model = buildModel();
                                if (Application.get().isHeadlessEnvironment()) {
                                    return;
                                }
                                myPopup.update(model, anchor);
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
        Consumer<PsiElement> elementForUpdatingConsumer
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

    private void executeUpdateParameterInfo(
        PsiElement elementForUpdating,
        MyUpdateParameterInfoContext context,
        @Nullable Consumer<ParameterInfoAnchor> continuation
    ) {
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
                    return anchorOf(elementForUpdating);
                }
                catch (IndexNotReadyException e) {
                    DumbService.getInstance(myProject).showDumbModeNotification(
                        CodeInsightLocalize.parameterInfoIndexingModeNotSupported()
                    );
                }
                return null;
            }).withDocumentsCommitted(myProject).expireWhen(
                () -> !myKeepOnHintHidden && !myPopup.isVisible() && !Application.get().isHeadlessEnvironment() ||
                    getCurrentOffset() != context.getOffset() ||
                    !elementForUpdating.isValid()
            ).expireWith(this),
            anchor -> {
                if (anchor != null && continuation != null) {
                    context.applyUIChanges();
                    continuation.accept(anchor);
                }
            },
            null,
            myEditor
        );
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

        if (!myPopup.isVisible()) {
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
        List<Inlay<? extends ParameterHintRenderer>> inlays =
            hostEditor.getInlayModel().getInlineElementsInRange(hostWhitespaceStart, hostWhitespaceEnd, ParameterHintRenderer.class);
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
                PsiElement parameterOwner = myState.getParameterOwner();
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
    private static int getParameterIndex(PsiElement[] parameters, IElementType delimiter, int offset) {
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
    private static int getParameterNavigationOffset(PsiElement parameter, CharSequence text) {
        int rangeStart = parameter.getTextRange().getStartOffset();
        int rangeEnd = parameter.getTextRange().getEndOffset();
        int offset = CharArrayUtil.shiftBackward(text, rangeEnd - 1, WHITESPACE) + 1;
        return offset > rangeStart ? offset : CharArrayUtil.shiftForward(text, rangeEnd, WHITESPACE);
    }

    @RequiredReadAction
    public static <E extends PsiElement> @Nullable E findArgumentList(PsiFile file, int offset, int lbraceOffset) {
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
        return myState.getObjects();
    }

    public @Nullable Object getHighlighted() {
        return myState.getHighlighted();
    }

    public void setPreservedOnHintHidden(boolean value) {
        myKeepOnHintHidden = value;
    }

    @TestOnly
    public static void waitForDelayedActions(Editor editor, long timeout, TimeUnit unit) throws TimeoutException {
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
                enabled[i] = myState.isEnabled(i);
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
        public Editor getEditor() {
            return myEditor;
        }

        @Override
        public void removeHint() {
            Application.get().invokeLater(() -> {
                if (!myPopup.isVisible()) {
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
            myState.setParameterOwner(o);
        }

        @Override
        public PsiElement getParameterOwner() {
            return myState.getParameterOwner();
        }

        @Override
        public void setHighlightedParameter(Object method) {
            myState.setHighlighted(method);
        }

        @Override
        public Object getHighlightedParameter() {
            return myState.getHighlighted();
        }

        @Override
        public void setCurrentParameter(int index) {
            myState.setCurrentParameterIndex(index);
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
            return myState.getObjects();
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
            PsiElement ourOwner = myState.getParameterOwner();
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
                    PsiElement parameterOwner = controller.myState.getParameterOwner();
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
                myState.setEnabled(index, enabled[index]);
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
        myPopup.hide();
    }

    private class MyDeleteParameterInfoContext implements DeleteParameterInfoContext {
        @Override
        public PsiElement getParameterOwner() {
            return myState.getParameterOwner();
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
