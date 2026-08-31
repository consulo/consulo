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
package consulo.ide.impl.fileEditor.text;

import consulo.application.AllIcons;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposer;
import consulo.fileEditor.*;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Pair;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified counterpart of the awt two panel editor - editor, preview, or both.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class UnifiedTextEditorWithPreview extends UserDataHolderBase implements TextEditorWithPreview {
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

        public static Layout fromName(@Nullable String name, Layout defaultValue) {
            for (Layout layout : values()) {
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

    private static class MyFileEditorState implements FileEditorState {
        private final @Nullable Layout mySplitLayout;
        private final @Nullable FileEditorState myFirstState;
        private final @Nullable FileEditorState mySecondState;

        private MyFileEditorState(@Nullable Layout splitLayout,
                                  @Nullable FileEditorState firstState,
                                  @Nullable FileEditorState secondState) {
            mySplitLayout = splitLayout;
            myFirstState = firstState;
            mySecondState = secondState;
        }

        @Override
        public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
            return otherState instanceof MyFileEditorState fileEditorState
                && (myFirstState == null || myFirstState.canBeMergedWith(fileEditorState.myFirstState, level))
                && (mySecondState == null || mySecondState.canBeMergedWith(fileEditorState.mySecondState, level));
        }
    }

    private static final int DEFAULT_PROPORTION = 50;

    private final TextEditor myEditor;
    private final FileEditor myPreview;
    private final @Nullable ActionToolbar myLeftToolbarActionToolbar;
    private final String myName;

    private final Map<PropertyChangeListener, Pair<Integer, DelegatePropertyChangeListener>> myListeners = new HashMap<>();

    private @Nullable Layout myLayout;
    private @Nullable DockLayout myComponent;
    private @Nullable ActionToolbar myLayoutToolbar;

    public UnifiedTextEditorWithPreview(TextEditor editor,
                                        FileEditor preview,
                                        @Nullable ActionToolbar leftToolbarActionToolbar,
                                        String editorName) {
        myEditor = editor;
        myPreview = preview;
        myLeftToolbarActionToolbar = leftToolbarActionToolbar;
        myName = editorName;
    }

    @Override
    public TextEditor getTextEditor() {
        return myEditor;
    }

    @Override
    public FileEditor getPreviewEditor() {
        return myPreview;
    }

    @Override
    @RequiredUIAccess
    public void switchToPreview() {
        myLayout = Layout.SHOW_PREVIEW;

        adjustEditorsVisibility();
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        if (myComponent == null) {
            ApplicationPropertiesComponent properties = ApplicationPropertiesComponent.getInstance();

            TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
            splitLayout.setProportion(properties.getInt(getSplitterProportionKey(), DEFAULT_PROPORTION));
            splitLayout.setFirstComponent(myEditor.getUIComponent());
            splitLayout.setSecondComponent(myPreview.getUIComponent());
            splitLayout.addSplitProportionChangedListener(
                event -> properties.setValue(getSplitterProportionKey(), event.getProportion(), DEFAULT_PROPORTION)
            );

            if (myLayout == null) {
                String lastUsed = ApplicationPropertiesComponent.getInstance().getValue(getLayoutPropertyName());
                myLayout = Layout.fromName(lastUsed, Layout.SHOW_EDITOR_AND_PREVIEW);
            }
            adjustEditorsVisibility();

            myComponent = DockLayout.create();
            myComponent.center(splitLayout);
            myComponent.top(createToolbar(myComponent));
        }
        return myComponent;
    }

    @RequiredUIAccess
    private Component createToolbar(Component targetComponent) {
        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(new ChangeViewModeAction(Layout.SHOW_EDITOR))
            .add(new ChangeViewModeAction(Layout.SHOW_EDITOR_AND_PREVIEW))
            .add(new ChangeViewModeAction(Layout.SHOW_PREVIEW))
            .build();

        myLayoutToolbar = ActionManager.getInstance().createActionToolbar("TextEditorWithPreview", group, true);
        myLayoutToolbar.setTargetUIComponent(targetComponent);

        DockLayout toolbarLayout = DockLayout.create();
        if (myLeftToolbarActionToolbar != null) {
            myLeftToolbarActionToolbar.setTargetUIComponent(targetComponent);
            toolbarLayout.left(myLeftToolbarActionToolbar.getUIComponent());
        }
        // the filler is what pushes the layout switch to the far edge - a dock layout without a centre lets its
        // sides sit next to each other instead of at the two ends of the row
        toolbarLayout.center(HorizontalLayout.create());
        toolbarLayout.right(myLayoutToolbar.getUIComponent());
        return toolbarLayout;
    }

    @RequiredUIAccess
    private void adjustEditorsVisibility() {
        Component editorComponent = myEditor.getUIComponent();
        if (editorComponent != null) {
            editorComponent.setVisible(myLayout == Layout.SHOW_EDITOR || myLayout == Layout.SHOW_EDITOR_AND_PREVIEW);
        }

        Component previewComponent = myPreview.getUIComponent();
        if (previewComponent != null) {
            previewComponent.setVisible(myLayout == Layout.SHOW_PREVIEW || myLayout == Layout.SHOW_EDITOR_AND_PREVIEW);
        }
    }

    @RequiredUIAccess
    private void invalidateLayout() {
        adjustEditorsVisibility();

        if (myLayoutToolbar != null) {
            myLayoutToolbar.updateActionsAsync();
        }

        Component focusComponent = getPreferredFocusedUIComponent();
        if (focusComponent != null) {
            IdeFocusManager.getGlobalInstance().requestFocus(focusComponent, true);
        }
    }

    private String getSplitterProportionKey() {
        return myName + "SplitterProportion";
    }

    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myLayout == Layout.SHOW_PREVIEW
            ? myPreview.getPreferredFocusedUIComponent()
            : myEditor.getPreferredFocusedUIComponent();
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public FileEditorState getState(FileEditorStateLevel level) {
        return new MyFileEditorState(myLayout, myEditor.getState(level), myPreview.getState(level));
    }

    @Override
    public void setState(FileEditorState state) {
        if (state instanceof MyFileEditorState compositeState) {
            if (compositeState.myFirstState != null) {
                myEditor.setState(compositeState.myFirstState);
            }
            if (compositeState.mySecondState != null) {
                myPreview.setState(compositeState.mySecondState);
            }
            if (compositeState.mySplitLayout != null) {
                myLayout = compositeState.mySplitLayout;
                invalidateLayout();
            }
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

    @Override
    public @Nullable BackgroundEditorHighlighter getBackgroundHighlighter() {
        return myEditor.getBackgroundHighlighter();
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return myEditor.getCurrentLocation();
    }

    @Override
    public @Nullable StructureViewBuilder getStructureViewBuilder() {
        return myEditor.getStructureViewBuilder();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        Pair<Integer, DelegatePropertyChangeListener> pair = myListeners.get(listener);
        pair = pair == null
            ? Pair.create(1, new DelegatePropertyChangeListener(listener))
            : Pair.create(pair.getFirst() + 1, pair.getSecond());
        myListeners.put(listener, pair);

        myEditor.addPropertyChangeListener(pair.getSecond());
        myPreview.addPropertyChangeListener(pair.getSecond());
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        Pair<Integer, DelegatePropertyChangeListener> pair = myListeners.get(listener);
        if (pair == null) {
            return;
        }

        if (pair.getFirst() == 1) {
            myListeners.remove(listener);
        }
        else {
            myListeners.put(listener, Pair.create(pair.getFirst() - 1, pair.getSecond()));
        }

        myEditor.removePropertyChangeListener(pair.getSecond());
        myPreview.removePropertyChangeListener(pair.getSecond());
    }

    @Override
    public void dispose() {
        Disposer.dispose(myEditor);
        Disposer.dispose(myPreview);
    }

    private String getLayoutPropertyName() {
        return myName + "Layout";
    }

    /**
     * Re-fires the child editor events as own ones, otherwise the file editor manager would see the inner editor as
     * the source and would not find the composite it belongs to.
     */
    private class DelegatePropertyChangeListener implements PropertyChangeListener {
        private final PropertyChangeListener myDelegate;

        private DelegatePropertyChangeListener(PropertyChangeListener delegate) {
            myDelegate = delegate;
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            myDelegate.propertyChange(new PropertyChangeEvent(UnifiedTextEditorWithPreview.this,
                                                              event.getPropertyName(),
                                                              event.getOldValue(),
                                                              event.getNewValue()));
        }
    }

    private class ChangeViewModeAction extends ToggleAction implements DumbAware {
        private final Layout myActionLayout;

        private ChangeViewModeAction(Layout layout) {
            super(LocalizeValue.of(layout.getName()), LocalizeValue.of(layout.getName()), layout.getIcon());
            myActionLayout = layout;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myLayout == myActionLayout;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                myLayout = myActionLayout;
                ApplicationPropertiesComponent.getInstance()
                    .setValue(getLayoutPropertyName(), myActionLayout.getName(), Layout.SHOW_EDITOR_AND_PREVIEW.getName());
                adjustEditorsVisibility();
            }
        }
    }
}
