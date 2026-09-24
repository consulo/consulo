/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.debug.impl.internal.frame;

import consulo.application.ReadAction;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XStackFrameContainerEx;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.internal.ExectutionDebugInternal;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.language.editor.FileColorManager;
import consulo.language.editor.scope.NonProjectFilesScope;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.ComboBoxStyle;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The stack frames of a debug session, built of unified components - the counterpart of the swing {@link XFramesView}.
 * A combo box holds the execution stacks, the list below the frames of the selected one, and selecting a frame makes it
 * the current frame of the session.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedXFramesView extends XDebugView {
    private static final Logger LOG = Logger.getInstance(UnifiedXFramesView.class);

    /**
     * The row shown below the frames while more of them are computed.
     */
    private static final Object LOADING = new Object();

    /**
     * How the file of a frame is shown - the icon of its root, a library or a source folder, and the color of its
     * scope. The frames without a file are the ones of no project file.
     */
    private record FileDecoration(Image icon, @Nullable ColorValue background) {
    }

    private final Project myProject;
    private final XDebugSessionImpl mySession;
    private final UIAccess myUIAccess;

    private final DockLayout myRoot;
    private final MutableFlatDataModel<XExecutionStack> myStacksModel;
    private final ComboBox<XExecutionStack> myStacksComboBox;
    private final DockLayout myStacksPanel;
    private final MutableFlatDataModel<Object> myFramesModel;
    private final ListBox<Object> myFramesList;

    private final Map<XExecutionStack, StackFramesListBuilder> myBuilders = new HashMap<>();
    /**
     * Computed with the frames, off the ui thread - the root of a file is read from the project model.
     */
    private final Map<VirtualFile, FileDecoration> myFileDecorations = new ConcurrentHashMap<>();
    private volatile @Nullable FileDecoration myNoFileDecoration;
    private final Map<XExecutionStack, Integer> myExecutionStacksWithSelection = new HashMap<>();

    private @Nullable XExecutionStack mySelectedStack;
    private int mySelectedFrameIndex;
    private boolean myListenersEnabled;
    private boolean myRefresh;
    /**
     * Every pause asks the suspend context for all of its execution stacks - an answer to a pause before is dropped.
     */
    private int myStacksGeneration;

    @RequiredUIAccess
    public UnifiedXFramesView(Project project, XDebugSessionImpl session) {
        myProject = project;
        mySession = session;
        myUIAccess = project.getUIAccess();

        myStacksModel = FlatDataModel.lazyOf(List.of());
        myStacksComboBox = ComboBox.create(myStacksModel);
        myStacksComboBox.addStyle(ComboBoxStyle.INPLACE);
        myStacksComboBox.setRender((presentation, item) -> {
            XExecutionStack stack = item.getValue();
            if (stack != null) {
                presentation.withIcon(stack.getIcon());
                presentation.append(stack.getDisplayName());
            }
        });
        myStacksComboBox.addValueListener(event -> {
            XExecutionStack stack = event.getValue();
            if (myListenersEnabled && stack != null && !Objects.equals(stack, mySelectedStack)) {
                updateFrames(stack, mySession, null);
            }
        });
        // the threads take the width of the view, the way the swing combo box does
        myStacksPanel = DockLayout.create();
        myStacksPanel.center(myStacksComboBox);
        myStacksPanel.setVisible(false);

        myFramesModel = FlatDataModel.lazyOf(List.of());
        myFramesList = ListBox.create(myFramesModel);
        myFramesList.setRender((presentation, item) -> {
            Object value = item.getValue();
            if (value instanceof XStackFrame frame) {
                UnifiedColoredTextContainer text = new UnifiedColoredTextContainer();
                frame.customizePresentation(text);
                text.appendTo(presentation);

                // the icon the frame gave is replaced by the one of its root, as the swing list does
                FileDecoration decoration = getDecoration(frame);
                if (decoration != null) {
                    presentation.withIcon(decoration.icon());
                    if (!item.isSelected()) {
                        presentation.withBackgroundColor(decoration.background());
                    }
                }
            }
            else if (value instanceof LocalizeValue error) {
                presentation.append(error, TextAttribute.ERROR);
            }
            else {
                presentation.append(XDebuggerLocalize.stackFrameLoadingText(), TextAttribute.GRAYED);
            }
        });
        myFramesList.addValueListener(event -> {
            Object value = event.getValue();
            if (myListenersEnabled && value != null && myFramesModel.indexOf(value) != mySelectedFrameIndex) {
                processFrameSelection(mySession, true);
            }
        });

        myRoot = DockLayout.create();
        myRoot.top(myStacksPanel);
        myRoot.center(myFramesList);
    }

    public Component getMainPanel() {
        return myRoot;
    }

    private static @Nullable VirtualFile getFile(XStackFrame frame) {
        XSourcePosition position = frame.getSourcePosition();
        return position != null ? position.getFile() : null;
    }

    private @Nullable FileDecoration getDecoration(XStackFrame frame) {
        VirtualFile file = getFile(frame);
        return file == null ? myNoFileDecoration : myFileDecorations.get(file);
    }

    private void computeDecorations(List<? extends XStackFrame> frames) {
        if (myProject.isDisposed()) {
            return;
        }

        FileColorManager colorManager = FileColorManager.getInstance(myProject);
        ExectutionDebugInternal internal = myProject.getInstance(ExectutionDebugInternal.class);
        ReadAction.run(() -> {
            for (XStackFrame frame : frames) {
                VirtualFile file = getFile(frame);
                if (file == null) {
                    if (myNoFileDecoration == null) {
                        ColorValue background = TargetAWT.from(colorManager.getScopeColor(NonProjectFilesScope.NAME));
                        myNoFileDecoration = new FileDecoration(PlatformIconGroup.actionsHelp(), background);
                    }
                }
                else if (file.isValid() && !myFileDecorations.containsKey(file)) {
                    Image icon = internal.getContentRootIcon(myProject, file);
                    myFileDecorations.put(file, new FileDecoration(icon, colorManager.getFileColorValue(file)));
                }
            }
        });
    }

    @Override
    protected XDebugSession getSession() {
        return mySession;
    }

    @Override
    public void processSessionEvent(SessionEvent event, XDebugSession session) {
        myRefresh = event == SessionEvent.SETTINGS_CHANGED;

        if (event == SessionEvent.BEFORE_RESUME) {
            return;
        }

        XExecutionStack currentExecutionStack = ((XDebugSessionImpl) session).getCurrentExecutionStack();
        XStackFrame currentStackFrame = session.getCurrentStackFrame();
        XSuspendContext suspendContext = session.getSuspendContext();

        if (event == SessionEvent.FRAME_CHANGED && Objects.equals(mySelectedStack, currentExecutionStack)) {
            myUIAccess.giveIfNeed(() -> {
                if (currentStackFrame != null) {
                    selectFrame(currentStackFrame);
                }
            });
            return;
        }

        myUIAccess.give(() -> {
            if (event != SessionEvent.SETTINGS_CHANGED) {
                mySelectedFrameIndex = 0;
                mySelectedStack = null;
            }

            myListenersEnabled = false;
            myBuilders.values().forEach(StackFramesListBuilder::dispose);
            myBuilders.clear();

            if (suspendContext == null) {
                requestClear();
                return;
            }

            if (event == SessionEvent.PAUSED) {
                // clear immediately
                cancelClear();
                doClear();
            }

            XExecutionStack activeExecutionStack = mySelectedStack != null ? mySelectedStack : currentExecutionStack;
            addExecutionStacks(Collections.singletonList(activeExecutionStack));
            addExecutionStacks(Arrays.asList(suspendContext.getExecutionStacks()));
            computeExecutionStacks(suspendContext);

            selectStack(activeExecutionStack);
            myStacksPanel.setVisible(activeExecutionStack != null && !StringUtil.isEmpty(activeExecutionStack.getDisplayName()));

            updateFrames(activeExecutionStack, session, event == SessionEvent.FRAME_CHANGED ? currentStackFrame : null);
        });
    }

    @Override
    protected void clear() {
        myUIAccess.giveIfNeed(this::doClear);
    }

    @RequiredUIAccess
    private void doClear() {
        myListenersEnabled = false;
        myStacksGeneration++;
        myStacksModel.removeAll();
        myStacksPanel.setVisible(false);
        myFramesModel.removeAll();
        myExecutionStacksWithSelection.clear();
    }

    @Override
    public void dispose() {
    }

    @RequiredUIAccess
    private void addExecutionStacks(List<? extends XExecutionStack> executionStacks) {
        for (XExecutionStack executionStack : executionStacks) {
            if (executionStack != null && !myExecutionStacksWithSelection.containsKey(executionStack)) {
                myStacksModel.add(executionStack);
                myExecutionStacksWithSelection.put(executionStack, 0);
            }
        }
    }

    /**
     * The combo box has no popup to wait for, so all the execution stacks are asked for when the session pauses.
     */
    @RequiredUIAccess
    private void computeExecutionStacks(XSuspendContext suspendContext) {
        int generation = ++myStacksGeneration;
        suspendContext.computeExecutionStacks(new XSuspendContext.XExecutionStackContainer() {
            @Override
            public void addExecutionStack(List<? extends XExecutionStack> executionStacks, boolean last) {
                myUIAccess.give(() -> {
                    if (generation == myStacksGeneration) {
                        addExecutionStacks(executionStacks);
                        // a change of the items drops the selection of the combo box
                        selectStack(mySelectedStack);
                    }
                });
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
            }
        });
    }

    @RequiredUIAccess
    private void updateFrames(@Nullable XExecutionStack executionStack, XDebugSession session, @Nullable XStackFrame frameToSelect) {
        if (mySelectedStack != null) {
            getOrCreateBuilder(mySelectedStack, session).stop();
        }

        mySelectedStack = executionStack;
        if (executionStack != null) {
            mySelectedFrameIndex = myExecutionStacksWithSelection.getOrDefault(executionStack, 0);
            StackFramesListBuilder builder = getOrCreateBuilder(executionStack, session);
            builder.setToSelect(frameToSelect != null ? frameToSelect : mySelectedFrameIndex);
            myListenersEnabled = false;
            builder.initModel();
            myListenersEnabled = !builder.start();
        }
    }

    private StackFramesListBuilder getOrCreateBuilder(XExecutionStack executionStack, XDebugSession session) {
        return myBuilders.computeIfAbsent(executionStack, k -> new StackFramesListBuilder(executionStack, session));
    }

    @RequiredUIAccess
    private void selectStack(@Nullable XExecutionStack stack) {
        boolean listenersEnabled = myListenersEnabled;
        myListenersEnabled = false;
        myStacksComboBox.setValue(stack, false);
        myListenersEnabled = listenersEnabled;
    }

    @RequiredUIAccess
    private void selectFrame(XStackFrame frame) {
        boolean listenersEnabled = myListenersEnabled;
        myListenersEnabled = false;
        myFramesList.setValue(frame, false);
        myListenersEnabled = listenersEnabled;

        mySelectedFrameIndex = myFramesModel.indexOf(frame);
        if (mySelectedStack != null) {
            myExecutionStacksWithSelection.put(mySelectedStack, mySelectedFrameIndex);
        }
    }

    @RequiredUIAccess
    private void processFrameSelection(@Nullable XDebugSession session, boolean force) {
        Object selected = myFramesList.getValue();
        mySelectedFrameIndex = selected == null ? -1 : myFramesModel.indexOf(selected);
        if (mySelectedStack != null) {
            myExecutionStacksWithSelection.put(mySelectedStack, mySelectedFrameIndex);
        }

        if (selected instanceof XStackFrame frame && session != null && mySelectedStack != null) {
            if (force || (!myRefresh && session.getCurrentStackFrame() != frame)) {
                session.setCurrentStackFrame(mySelectedStack, frame, mySelectedFrameIndex == 0);
            }
        }
    }

    private class StackFramesListBuilder implements XStackFrameContainerEx {
        private @Nullable XExecutionStack myExecutionStack;
        private final List<XStackFrame> myStackFrames = new ArrayList<>();
        private LocalizeValue myErrorMessage = LocalizeValue.empty();
        private volatile boolean myRunning;
        private boolean myAllFramesLoaded;
        private final XDebugSession mySession;
        private Object myToSelect;

        private StackFramesListBuilder(XExecutionStack executionStack, XDebugSession session) {
            myExecutionStack = executionStack;
            mySession = session;
        }

        void setToSelect(Object toSelect) {
            myToSelect = toSelect;
        }

        @Override
        public void addStackFrames(List<? extends XStackFrame> stackFrames, boolean last) {
            addStackFrames(stackFrames, null, last);
        }

        @Override
        public void addStackFrames(List<? extends XStackFrame> stackFrames, @Nullable XStackFrame toSelect, boolean last) {
            if (isObsolete()) {
                return;
            }
            computeDecorations(stackFrames);
            myUIAccess.give(() -> {
                if (isObsolete()) {
                    return;
                }
                myStackFrames.addAll(stackFrames);
                addFrameListElements(stackFrames, last);

                if (toSelect != null) {
                    setToSelect(toSelect);
                }

                myAllFramesLoaded = last;

                selectCurrentFrame();

                if (last) {
                    myRunning = false;
                    myListenersEnabled = true;
                }
            });
        }

        @Override
        public void errorOccurred(LocalizeValue errorMessage) {
            if (isObsolete()) {
                return;
            }
            myUIAccess.give(() -> {
                if (isObsolete()) {
                    return;
                }
                if (myErrorMessage.isEmpty()) {
                    myErrorMessage = errorMessage;
                    addFrameListElements(Collections.singletonList(errorMessage), true);
                    myRunning = false;
                    myListenersEnabled = true;
                }
            });
        }

        @RequiredUIAccess
        private void addFrameListElements(List<?> values, boolean last) {
            if (myExecutionStack == null || myExecutionStack != mySelectedStack) {
                return;
            }

            int size = myFramesModel.getSize();
            boolean loadingPresent = size > 0 && myFramesModel.get(size - 1) == LOADING;
            int insertIndex = loadingPresent ? size - 1 : size;
            for (Object value : values) {
                myFramesModel.add(value, insertIndex++);
            }

            if (last) {
                if (loadingPresent) {
                    myFramesModel.remove(LOADING);
                }
            }
            else if (!loadingPresent) {
                myFramesModel.add(LOADING);
            }
        }

        @Override
        public boolean isObsolete() {
            return !myRunning;
        }

        public void dispose() {
            myRunning = false;
            myExecutionStack = null;
        }

        public boolean start() {
            if (myExecutionStack == null || myErrorMessage.isNotEmpty()) {
                return false;
            }
            myRunning = true;
            myStackFrames.clear();
            myExecutionStack.computeStackFrames(this);
            return true;
        }

        public void stop() {
            myRunning = false;
        }

        @RequiredUIAccess
        private void selectCurrentFrame() {
            if (myToSelect instanceof XStackFrame frame) {
                if (!Objects.equals(myFramesList.getValue(), frame) && myFramesModel.indexOf(frame) >= 0) {
                    selectFrame(frame);
                    processFrameSelection(mySession, false);
                    myListenersEnabled = true;
                }
                if (myAllFramesLoaded && myFramesList.getValue() == null) {
                    LOG.error("Frame was not found, " + frame.getClass() + " must correctly override equals");
                }
            }
            else if (myToSelect instanceof Integer selectedFrameIndex) {
                Object current = myFramesList.getValue();
                int currentIndex = current == null ? -1 : myFramesModel.indexOf(current);
                if (currentIndex != selectedFrameIndex
                    && myFramesModel.getSize() > selectedFrameIndex
                    && myFramesModel.get(selectedFrameIndex) instanceof XStackFrame frame) {
                    selectFrame(frame);
                    processFrameSelection(mySession, false);
                    myListenersEnabled = true;
                }
            }
        }

        @RequiredUIAccess
        public void initModel() {
            myFramesModel.removeAll();
            myStackFrames.forEach(myFramesModel::add);
            if (myErrorMessage.isNotEmpty()) {
                myFramesModel.add(myErrorMessage);
            }
            else if (!myAllFramesLoaded) {
                myFramesModel.add(LOADING);
            }
            selectCurrentFrame();
        }
    }
}
