// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.fileEditor.impl.text;

import consulo.application.AllIcons;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.*;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two panel editor with three states: Editor, Preview and Editor with Preview.
 * Based on SplitFileEditor by Valentin Fondaratov
 *
 * @author Konstantin Bulenkov
 */
public class TextEditorWithPreviewImpl extends UserDataHolderBase implements TextEditorWithPreview {
    protected final TextEditor myEditor;
    protected final FileEditor myPreview;
    @Nonnull
    private final MyListenersMultimap myListenersGenerator = new MyListenersMultimap();
    private Layout myLayout;
    private JComponent myComponent;
    private SplitEditorToolbar myToolbarWrapper;

    @Nullable
    private final ActionToolbar myLeftToolbarActionToolbar;
    private final String myName;

    public TextEditorWithPreviewImpl(@Nonnull TextEditor editor,
                                     @Nonnull FileEditor preview,
                                     @Nullable ActionToolbar leftToolbarActionToolbar,
                                     @Nonnull String editorName) {
        myEditor = editor;
        myPreview = preview;
        myLeftToolbarActionToolbar = leftToolbarActionToolbar;
        myName = editorName;
    }

    @Nonnull
    @Override
    public TextEditor getTextEditor() {
        return myEditor;
    }

    @Nonnull
    @Override
    public FileEditor getPreviewEditor() {
        return myPreview;
    }

    @Nullable
    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
        return myEditor.getBackgroundHighlighter();
    }

    @Nullable
    @Override
    public FileEditorLocation getCurrentLocation() {
        return myEditor.getCurrentLocation();
    }

    @Nullable
    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        return myEditor.getStructureViewBuilder();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myEditor);
        Disposer.dispose(myPreview);
    }

    @Override
    public void selectNotify() {
        myEditor.selectNotify();
        myPreview.selectNotify();
    }

    @Override
    public void deselectNotify() {
        myEditor.deselectNotify();
        myPreview.deselectNotify();
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        if (myComponent == null) {
            final OnePixelSplitter splitter = new OnePixelSplitter(false, 0.5f, 0.15f, 0.85f);
            splitter.setSplitterProportionKey(getSplitterProportionKey());
            splitter.setFirstComponent(myEditor.getComponent());
            splitter.setSecondComponent(myPreview.getComponent());

            myToolbarWrapper = new SplitEditorToolbar(splitter);
            myToolbarWrapper.addGutterToTrack(((EditorGutterComponentEx) myEditor.getEditor().getGutter()));

            if (myPreview instanceof TextEditor textEditor) {
                myToolbarWrapper.addGutterToTrack(((EditorGutterComponentEx) textEditor.getEditor().getGutter()));
            }

            if (myLayout == null) {
                String lastUsed = ApplicationPropertiesComponent.getInstance().getValue(getLayoutPropertyName());
                myLayout = Layout.fromName(lastUsed, Layout.SHOW_EDITOR_AND_PREVIEW);
            }
            adjustEditorsVisibility();

            myComponent = JBUI.Panels.simplePanel(splitter).addToTop(myToolbarWrapper);
        }
        return myComponent;
    }

    @Override
    public void setState(@Nonnull FileEditorState state) {
        if (state instanceof MyFileEditorState compositeState) {
            if (compositeState.getFirstState() != null) {
                myEditor.setState(compositeState.getFirstState());
            }
            if (compositeState.getSecondState() != null) {
                myPreview.setState(compositeState.getSecondState());
            }
            if (compositeState.getSplitLayout() != null) {
                myLayout = compositeState.getSplitLayout();
                invalidateLayout();
            }
        }
    }

    private void adjustEditorsVisibility() {
        myEditor.getComponent().setVisible(myLayout == Layout.SHOW_EDITOR || myLayout == Layout.SHOW_EDITOR_AND_PREVIEW);
        myPreview.getComponent().setVisible(myLayout == Layout.SHOW_PREVIEW || myLayout == Layout.SHOW_EDITOR_AND_PREVIEW);
    }

    private void invalidateLayout() {
        adjustEditorsVisibility();
        myToolbarWrapper.refresh();
        myComponent.repaint();

        final JComponent focusComponent = getPreferredFocusedComponent();
        if (focusComponent != null) {
            IdeFocusManager.findInstanceByComponent(focusComponent).requestFocus(focusComponent, true);
        }
    }

    @Nonnull
    protected String getSplitterProportionKey() {
        return "TextEditorWithPreview.SplitterProportionKey";
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        switch (myLayout) {
            case SHOW_EDITOR_AND_PREVIEW:
            case SHOW_EDITOR:
                return myEditor.getPreferredFocusedComponent();
            case SHOW_PREVIEW:
                return myPreview.getPreferredFocusedComponent();
            default:
                throw new IllegalStateException(myLayout.myName);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public FileEditorState getState(@Nonnull FileEditorStateLevel level) {
        return new MyFileEditorState(myLayout, myEditor.getState(level), myPreview.getState(level));
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
        myEditor.addPropertyChangeListener(listener);
        myPreview.addPropertyChangeListener(listener);

        final DoublingEventListenerDelegate delegate = myListenersGenerator.addListenerAndGetDelegate(listener);
        myEditor.addPropertyChangeListener(delegate);
        myPreview.addPropertyChangeListener(delegate);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
        myEditor.removePropertyChangeListener(listener);
        myPreview.removePropertyChangeListener(listener);

        final DoublingEventListenerDelegate delegate = myListenersGenerator.removeListenerAndGetDelegate(listener);
        if (delegate != null) {
            myEditor.removePropertyChangeListener(delegate);
            myPreview.removePropertyChangeListener(delegate);
        }
    }

    static class MyFileEditorState implements FileEditorState {
        private final Layout mySplitLayout;
        private final FileEditorState myFirstState;
        private final FileEditorState mySecondState;

        public MyFileEditorState(Layout layout, FileEditorState firstState, FileEditorState secondState) {
            mySplitLayout = layout;
            myFirstState = firstState;
            mySecondState = secondState;
        }

        @Nullable
        public Layout getSplitLayout() {
            return mySplitLayout;
        }

        @Nullable
        public FileEditorState getFirstState() {
            return myFirstState;
        }

        @Nullable
        public FileEditorState getSecondState() {
            return mySecondState;
        }

        @Override
        public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
            return otherState instanceof MyFileEditorState fileEditorState
                && (myFirstState == null || myFirstState.canBeMergedWith(fileEditorState.myFirstState, level))
                && (mySecondState == null || mySecondState.canBeMergedWith(fileEditorState.mySecondState, level));
        }
    }

    @Override
    public boolean isModified() {
        return myEditor.isModified() || myPreview.isModified();
    }

    @Override
    public boolean isValid() {
        return myEditor.isValid() && myPreview.isValid();
    }

    private class DoublingEventListenerDelegate implements PropertyChangeListener {
        @Nonnull
        private final PropertyChangeListener myDelegate;

        private DoublingEventListenerDelegate(@Nonnull PropertyChangeListener delegate) {
            myDelegate = delegate;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            myDelegate.propertyChange(new PropertyChangeEvent(TextEditorWithPreviewImpl.this, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));
        }
    }

    private class MyListenersMultimap {
        private final Map<PropertyChangeListener, Pair<Integer, DoublingEventListenerDelegate>> myMap = new HashMap<>();

        @Nonnull
        public DoublingEventListenerDelegate addListenerAndGetDelegate(@Nonnull PropertyChangeListener listener) {
            if (!myMap.containsKey(listener)) {
                myMap.put(listener, Pair.create(1, new DoublingEventListenerDelegate(listener)));
            }
            else {
                final Pair<Integer, DoublingEventListenerDelegate> oldPair = myMap.get(listener);
                myMap.put(listener, Pair.create(oldPair.getFirst() + 1, oldPair.getSecond()));
            }

            return myMap.get(listener).getSecond();
        }

        @Nullable
        public DoublingEventListenerDelegate removeListenerAndGetDelegate(@Nonnull PropertyChangeListener listener) {
            final Pair<Integer, DoublingEventListenerDelegate> oldPair = myMap.get(listener);
            if (oldPair == null) {
                return null;
            }

            if (oldPair.getFirst() == 1) {
                myMap.remove(listener);
            }
            else {
                myMap.put(listener, Pair.create(oldPair.getFirst() - 1, oldPair.getSecond()));
            }
            return oldPair.getSecond();
        }
    }

    public class SplitEditorToolbar extends JPanel implements Disposable {
        private final ActionToolbar myRightToolbar;

        private final List<EditorGutterComponentEx> myGutters = new ArrayList<>();

        private final ComponentAdapter myAdjustToGutterListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustSpacing();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                adjustSpacing();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                adjustSpacing();
            }
        };

        public SplitEditorToolbar(@Nonnull final JComponent targetComponentForActions) {
            super(new BorderLayout());

            final ActionToolbar leftToolbar = createToolbar();
            if (leftToolbar != null) {
                leftToolbar.setTargetComponent(targetComponentForActions);
                add(leftToolbar.getComponent(), BorderLayout.WEST);
            }

            ActionGroup group = new DefaultActionGroup(new ChangeViewModeAction(Layout.SHOW_EDITOR), new ChangeViewModeAction(Layout.SHOW_EDITOR_AND_PREVIEW), new ChangeViewModeAction(Layout.SHOW_PREVIEW));
            myRightToolbar = ActionManager.getInstance().createActionToolbar("TextEditorWithPreview", group, true);
            myRightToolbar.setTargetComponent(targetComponentForActions);
            add(myRightToolbar.getComponent(), BorderLayout.EAST);

            addComponentListener(myAdjustToGutterListener);

            setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0));
        }

        public void addGutterToTrack(@Nonnull EditorGutterComponentEx gutterComponentEx) {
            myGutters.add(gutterComponentEx);

            gutterComponentEx.getComponent().addComponentListener(myAdjustToGutterListener);
        }

        public void refresh() {
            adjustSpacing();
            myRightToolbar.updateActionsImmediately();
        }

        private void adjustSpacing() {
            EditorGutterComponentEx leftMostGutter = null;
            for (EditorGutterComponentEx gutter : myGutters) {
                if (!gutter.getComponent().isShowing()) {
                    continue;
                }
                if (leftMostGutter == null || leftMostGutter.getComponent().getX() > gutter.getComponent().getX()) {
                    leftMostGutter = gutter;
                }
            }

            revalidate();
            repaint();
        }

        @Override
        public void dispose() {
            removeComponentListener(myAdjustToGutterListener);
            for (EditorGutterComponentEx gutter : myGutters) {
                gutter.getComponent().removeComponentListener(myAdjustToGutterListener);
            }
        }
    }

    @Nullable
    private ActionToolbar createToolbar() {
        return myLeftToolbarActionToolbar;
    }

    public enum Layout {
        SHOW_EDITOR("Editor only", AllIcons.General.LayoutEditorOnly),
        SHOW_PREVIEW("Preview only", AllIcons.General.LayoutPreviewOnly),
        SHOW_EDITOR_AND_PREVIEW("Editor and Preview", AllIcons.General.LayoutEditorPreview);

        private final String myName;
        private final Image myIcon;

        Layout(String name, Image icon) {
            myName = name;
            myIcon = icon;
        }

        public static Layout fromName(String name, Layout defaultValue) {
            for (Layout layout : Layout.values()) {
                if (layout.myName.equals(name)) {
                    return layout;
                }
            }
            return defaultValue;
        }

        public String getName() {
            return myName;
        }

        public Image getIcon() {
            return myIcon;
        }
    }

    private class ChangeViewModeAction extends ToggleAction implements DumbAware {
        private final Layout myActionLayout;

        public ChangeViewModeAction(Layout layout) {
            super(layout.getName(), layout.getName(), layout.getIcon());
            myActionLayout = layout;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myLayout == myActionLayout;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                myLayout = myActionLayout;
                ApplicationPropertiesComponent.getInstance().setValue(getLayoutPropertyName(), myLayout.myName, Layout.SHOW_EDITOR_AND_PREVIEW.myName);
                adjustEditorsVisibility();
            }
        }
    }

    @Nonnull
    private String getLayoutPropertyName() {
        return myName + "Layout";
    }
}
