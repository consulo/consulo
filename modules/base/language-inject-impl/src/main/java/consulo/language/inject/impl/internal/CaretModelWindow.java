// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.inject.impl.internal;

import consulo.codeEditor.*;
import consulo.codeEditor.event.CaretActionListener;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.language.editor.inject.EditorWindow;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

class CaretModelWindow implements CaretModel {
    private final CaretModel myDelegate;
    private final EditorEx myHostEditor;
    private final EditorWindow myEditorWindow;
    private final Map<Caret, InjectedCaret> myInjectedCaretMap = new WeakHashMap<>();

    CaretModelWindow(CaretModel delegate, EditorWindow editorWindow) {
        myDelegate = delegate;
        myHostEditor = (EditorEx) editorWindow.getDelegate();
        myEditorWindow = editorWindow;
    }

    @Override
    public void moveCaretRelatively(int columnShift, int lineShift, boolean withSelection, boolean blockSelection, boolean scrollToCaret) {
        myDelegate.moveCaretRelatively(columnShift, lineShift, withSelection, blockSelection, scrollToCaret);
    }

    @Override
    public void moveToLogicalPosition(LogicalPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(pos);
        myDelegate.moveToLogicalPosition(hostPos);
    }

    @Override
    public void moveToVisualPosition(VisualPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        myDelegate.moveToLogicalPosition(hostPos);
    }

    @Override
    public void moveToOffset(int offset) {
        moveToOffset(offset, false);
    }

    @Override
    public void moveToOffset(int offset, boolean locateBeforeSoftWrap) {
        int hostOffset = myEditorWindow.getDocument().injectedToHost(offset);
        myDelegate.moveToOffset(hostOffset, locateBeforeSoftWrap);
    }

    @Override
    
    public LogicalPosition getLogicalPosition() {
        LogicalPosition hostPos = myDelegate.getLogicalPosition();
        return myEditorWindow.hostToInjected(hostPos);
    }

    @Override
    
    public VisualPosition getVisualPosition() {
        LogicalPosition logicalPosition = getLogicalPosition();
        return myEditorWindow.logicalToVisualPosition(logicalPosition);
    }

    @Override
    public int getOffset() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getOffset());
    }

    @Override
    public boolean isUpToDate() {
        return myDelegate.isUpToDate();
    }

    private final ListenerWrapperMap<CaretListener> myCaretListeners = new ListenerWrapperMap<>();

    @Override
    public void addCaretListener(final CaretListener listener) {
        CaretListener wrapper = new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent e) {
                if (!myEditorWindow.getDocument().isValid()) {
                    return; // injected document can be destroyed by now
                }
                Caret caret = e.getCaret();
                assert caret != null;
                CaretEvent event = new CaretEvent(createInjectedCaret(caret), myEditorWindow.hostToInjected(e.getOldPosition()), myEditorWindow.hostToInjected(e.getNewPosition()));
                listener.caretPositionChanged(event);
            }
        };
        myCaretListeners.registerWrapper(listener, wrapper);
        myDelegate.addCaretListener(wrapper);
    }

    @Override
    public void removeCaretListener(CaretListener listener) {
        CaretListener wrapper = myCaretListeners.removeWrapper(listener);
        if (wrapper != null) {
            myDelegate.removeCaretListener(wrapper);
        }
    }

    public void disposeModel() {
        for (CaretListener wrapper : myCaretListeners.wrappers()) {
            myDelegate.removeCaretListener(wrapper);
        }
        myCaretListeners.clear();
    }

    @Override
    public int getVisualLineStart() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineStart());
    }

    @Override
    public int getVisualLineEnd() {
        return myEditorWindow.getDocument().hostToInjected(myDelegate.getVisualLineEnd());
    }

    @Override
    public TextAttributes getTextAttributes() {
        return myDelegate.getTextAttributes();
    }

    @Override
    public boolean supportsMultipleCarets() {
        return myDelegate.supportsMultipleCarets();
    }

    @Override
    public int getMaxCaretCount() {
        return myDelegate.getMaxCaretCount();
    }

    
    @Override
    public Caret getCurrentCaret() {
        return createInjectedCaret(myDelegate.getCurrentCaret());
    }

    
    @Override
    public Caret getPrimaryCaret() {
        return createInjectedCaret(myDelegate.getPrimaryCaret());
    }

    @Override
    public int getCaretCount() {
        return myDelegate.getCaretCount();
    }

    
    @Override
    public List<Caret> getAllCarets() {
        List<Caret> hostCarets = myDelegate.getAllCarets();
        List<Caret> carets = new ArrayList<>(hostCarets.size());
        for (Caret hostCaret : hostCarets) {
            carets.add(createInjectedCaret(hostCaret));
        }
        return carets;
    }

    @Nullable
    @Override
    public Caret getCaretAt(VisualPosition pos) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        Caret caret = myDelegate.getCaretAt(myHostEditor.logicalToVisualPosition(hostPos));
        return createInjectedCaret(caret);
    }

    @Nullable
    @Override
    public Caret addCaret(VisualPosition pos) {
        return addCaret(pos, true);
    }

    @Nullable
    @Override
    public Caret addCaret(VisualPosition pos, boolean makePrimary) {
        LogicalPosition hostPos = myEditorWindow.injectedToHost(myEditorWindow.visualToLogicalPosition(pos));
        Caret caret = myDelegate.addCaret(myHostEditor.logicalToVisualPosition(hostPos));
        return createInjectedCaret(caret);
    }

    @Override
    public boolean removeCaret(Caret caret) {
        if (caret instanceof InjectedCaret) {
            caret = ((InjectedCaret) caret).myDelegate;
        }
        return myDelegate.removeCaret(caret);
    }

    @Override
    public void removeSecondaryCarets() {
        myDelegate.removeSecondaryCarets();
    }

    @Override
    public void setCaretsAndSelections(List<? extends CaretState> caretStates) {
        List<CaretState> convertedStates = convertCaretStates(caretStates);
        myDelegate.setCaretsAndSelections(convertedStates);
    }

    @Override
    public void setCaretsAndSelections(List<? extends CaretState> caretStates, boolean updateSystemSelection) {
        List<CaretState> convertedStates = convertCaretStates(caretStates);
        myDelegate.setCaretsAndSelections(convertedStates, updateSystemSelection);
    }

    private List<CaretState> convertCaretStates(List<? extends CaretState> caretStates) {
        List<CaretState> convertedStates = new ArrayList<>(caretStates.size());
        for (CaretState state : caretStates) {
            convertedStates.add(new CaretState(injectedToHost(state.getCaretPosition()), injectedToHost(state.getSelectionStart()), injectedToHost(state.getSelectionEnd())));
        }
        return convertedStates;
    }

    private LogicalPosition injectedToHost(@Nullable LogicalPosition position) {
        return position == null ? null : myEditorWindow.injectedToHost(position);
    }

    
    @Override
    public List<CaretState> getCaretsAndSelections() {
        List<CaretState> caretsAndSelections = myDelegate.getCaretsAndSelections();
        List<CaretState> convertedStates = new ArrayList<>(caretsAndSelections.size());
        for (CaretState state : caretsAndSelections) {
            convertedStates.add(new CaretState(hostToInjected(state.getCaretPosition()), hostToInjected(state.getSelectionStart()), hostToInjected(state.getSelectionEnd())));
        }
        return convertedStates;
    }

    private LogicalPosition hostToInjected(@Nullable LogicalPosition position) {
        return position == null ? null : myEditorWindow.hostToInjected(position);
    }

    @Contract("null -> null; !null -> !null")
    private InjectedCaret createInjectedCaret(Caret caret) {
        if (caret == null) {
            return null;
        }
        synchronized (myInjectedCaretMap) {
            InjectedCaret injectedCaret = myInjectedCaretMap.get(caret);
            if (injectedCaret == null) {
                injectedCaret = new InjectedCaret(myEditorWindow, caret);
                myInjectedCaretMap.put(caret, injectedCaret);
            }
            return injectedCaret;
        }
    }

    @Override
    public void runForEachCaret(CaretAction action) {
        myDelegate.runForEachCaret(caret -> action.perform(createInjectedCaret(caret)));
    }

    @Override
    public void runForEachCaret(CaretAction action, boolean reverseOrder) {
        myDelegate.runForEachCaret(caret -> action.perform(createInjectedCaret(caret)), reverseOrder);
    }

    @Override
    public void addCaretActionListener(CaretActionListener listener, Disposable disposable) {
        myDelegate.addCaretActionListener(listener, disposable);
    }

    @Override
    public void runBatchCaretOperation(Runnable runnable) {
        myDelegate.runBatchCaretOperation(runnable);
    }
}
