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
package consulo.desktop.awt.internal.diff.util;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.ContextMenuPopupHandler;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorPopupHandler;
import consulo.diff.DiffContext;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.EmptyContent;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.impl.internal.TextDiffSettingsHolder;
import consulo.diff.impl.internal.TextDiffSettingsHolder.TextDiffSettings;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.impl.internal.util.HighlightPolicy;
import consulo.diff.impl.internal.util.IgnorePolicy;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.disposer.Disposable;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.ui.ToggleActionButton;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class TextDiffViewerUtil {
    public static final Logger LOG = Logger.getInstance(TextDiffViewerUtil.class);

    @Nonnull
    public static List<AnAction> createEditorPopupActions() {
        List<AnAction> result = new ArrayList<>();
        result.add(ActionManager.getInstance().getAction("CompareClipboardWithSelection"));

        result.add(AnSeparator.getInstance());
        ContainerUtil.addAll(
            result,
            ((ActionGroup)ActionManager.getInstance().getAction(IdeActions.GROUP_DIFF_EDITOR_POPUP)).getChildren(null)
        );

        return result;
    }

    @Nonnull
    public static FoldingModelSupport.Settings getFoldingModelSettings(@Nonnull DiffContext context) {
        TextDiffSettings settings = getTextSettings(context);
        return new FoldingModelSupport.Settings(settings.getContextRange(), settings.isExpandByDefault());
    }

    @Nonnull
    public static TextDiffSettings getTextSettings(@Nonnull DiffContext context) {
        TextDiffSettings settings = context.getUserData(TextDiffSettingsHolder.KEY);
        if (settings == null) {
            settings = TextDiffSettings.getSettings(context.getUserData(DiffUserDataKeys.PLACE));
            context.putUserData(TextDiffSettingsHolder.KEY, settings);
            if (DiffImplUtil.isUserDataFlagSet(DiffUserDataKeys.DO_NOT_IGNORE_WHITESPACES, context)) {
                settings.setIgnorePolicy(IgnorePolicy.DEFAULT);
            }
        }
        return settings;
    }

    @Nonnull
    public static boolean[] checkForceReadOnly(@Nonnull DiffContext context, @Nonnull ContentDiffRequest request) {
        int contentCount = request.getContents().size();
        boolean[] result = new boolean[contentCount];

        if (DiffImplUtil.isUserDataFlagSet(DiffUserDataKeys.FORCE_READ_ONLY, request, context)) {
            Arrays.fill(result, true);
            return result;
        }

        boolean[] data = request.getUserData(DiffUserDataKeys.FORCE_READ_ONLY_CONTENTS);
        if (data != null && data.length == contentCount) {
            return data;
        }

        return result;
    }

    public static void checkDifferentDocuments(@Nonnull ContentDiffRequest request) {
        // Actually, this should be a valid case. But it has little practical sense and will require explicit checks everywhere.
        // Some listeners will be processed once instead of 2 times, some listeners will cause illegal document modifications.
        List<DiffContent> contents = request.getContents();

        boolean sameDocuments = false;
        for (int i = 0; i < contents.size(); i++) {
            for (int j = i + 1; j < contents.size(); j++) {
                DiffContent content1 = contents.get(i);
                DiffContent content2 = contents.get(j);
                if (!(content1 instanceof DocumentContent)) {
                    continue;
                }
                if (!(content2 instanceof DocumentContent)) {
                    continue;
                }
                sameDocuments |= ((DocumentContent)content1).getDocument() == ((DocumentContent)content2).getDocument();
            }
        }

        if (sameDocuments) {
            StringBuilder message = new StringBuilder();
            message.append("DiffRequest with same documents detected\n");
            message.append(request.toString()).append("\n");
            for (DiffContent content : contents) {
                message.append(content.toString()).append("\n");
            }
            LOG.warn(new Throwable(message.toString()));
        }
    }

    public static boolean areEqualLineSeparators(@Nonnull List<? extends DiffContent> contents) {
        return areEqualDocumentContentProperties(contents, DocumentContent::getLineSeparator);
    }

    public static boolean areEqualCharsets(@Nonnull List<? extends DiffContent> contents) {
        return areEqualDocumentContentProperties(contents, DocumentContent::getCharset);
    }

    private static <T> boolean areEqualDocumentContentProperties(
        @Nonnull List<? extends DiffContent> contents,
        @Nonnull final Function<DocumentContent, T> propertyGetter
    ) {
        List<T> properties = ContainerUtil.mapNotNull(
            contents,
            content -> {
                if (content instanceof EmptyContent) {
                    return null;
                }
                return propertyGetter.apply((DocumentContent)content);
            }
        );

        return properties.size() < 2 || ContainerUtil.newHashSet(properties).size() == 1;
    }

    //
    // Actions
    //

    // TODO: pretty icons ?
    public static abstract class ComboBoxSettingAction<T> extends ComboBoxAction implements DumbAware {
        private DefaultActionGroup myChildren;

        public ComboBoxSettingAction() {
            setEnabledInModalContext(true);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setText(getText(getCurrentSetting()));
        }

        @Nonnull
        public DefaultActionGroup getPopupGroup() {
            initChildren();
            return myChildren;
        }

        @Nonnull
        @Override
        public DefaultActionGroup createPopupActionGroup(JComponent c) {
            initChildren();
            return myChildren;
        }

        private void initChildren() {
            if (myChildren == null) {
                myChildren = new DefaultActionGroup();
                for (T setting : getAvailableSettings()) {
                    myChildren.add(new MyAction(setting));
                }
            }
        }

        @Nonnull
        protected abstract List<T> getAvailableSettings();

        @Nonnull
        protected abstract String getText(@Nonnull T setting);

        @Nonnull
        protected abstract T getCurrentSetting();

        protected abstract void applySetting(@Nonnull T setting, @Nonnull AnActionEvent e);

        private class MyAction extends AnAction implements DumbAware {
            @Nonnull
            private final T mySetting;

            public MyAction(@Nonnull T setting) {
                super(getText(setting));
                setEnabledInModalContext(true);
                mySetting = setting;
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                applySetting(mySetting, e);
            }
        }
    }

    public static abstract class HighlightPolicySettingAction extends ComboBoxSettingAction<HighlightPolicy> {
        @Nonnull
        protected final TextDiffSettings mySettings;

        public HighlightPolicySettingAction(@Nonnull TextDiffSettings settings) {
            mySettings = settings;
        }

        @Override
        @RequiredUIAccess
        protected void applySetting(@Nonnull HighlightPolicy setting, @Nonnull AnActionEvent e) {
            if (getCurrentSetting() == setting) {
                return;
            }
            UsageTrigger.trigger("diff.TextDiffSettings.HighlightPolicy." + setting.name());
            mySettings.setHighlightPolicy(setting);
            update(e);
            onSettingsChanged();
        }

        @Nonnull
        @Override
        protected HighlightPolicy getCurrentSetting() {
            return mySettings.getHighlightPolicy();
        }

        @Nonnull
        @Override
        protected String getText(@Nonnull HighlightPolicy setting) {
            return setting.getText().get();
        }

        @Nonnull
        @Override
        protected List<HighlightPolicy> getAvailableSettings() {
            return Arrays.asList(HighlightPolicy.values());
        }

        protected abstract void onSettingsChanged();
    }

    public static abstract class IgnorePolicySettingAction extends ComboBoxSettingAction<IgnorePolicy> {
        @Nonnull
        protected final TextDiffSettings mySettings;

        public IgnorePolicySettingAction(@Nonnull TextDiffSettings settings) {
            mySettings = settings;
        }

        @Override
        @RequiredUIAccess
        protected void applySetting(@Nonnull IgnorePolicy setting, @Nonnull AnActionEvent e) {
            if (getCurrentSetting() == setting) {
                return;
            }
            UsageTrigger.trigger("diff.TextDiffSettings.IgnorePolicy." + setting.name());
            mySettings.setIgnorePolicy(setting);
            update(e);
            onSettingsChanged();
        }

        @Nonnull
        @Override
        protected IgnorePolicy getCurrentSetting() {
            return mySettings.getIgnorePolicy();
        }

        @Nonnull
        @Override
        protected String getText(@Nonnull IgnorePolicy setting) {
            return setting.getText().get();
        }

        @Nonnull
        @Override
        protected List<IgnorePolicy> getAvailableSettings() {
            return Arrays.asList(IgnorePolicy.values());
        }

        protected abstract void onSettingsChanged();
    }

    public static class ToggleAutoScrollAction extends ToggleActionButton implements DumbAware {
        @Nonnull
        protected final TextDiffSettings mySettings;

        public ToggleAutoScrollAction(@Nonnull TextDiffSettings settings) {
            super(DiffLocalize.synchronizeScrolling(), AllIcons.Actions.SynchronizeScrolling);
            mySettings = settings;
            setEnabledInModalContext(true);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return mySettings.isEnableSyncScroll();
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            mySettings.setEnableSyncScroll(state);
        }
    }

    public static abstract class ToggleExpandByDefaultAction extends ToggleActionButton implements DumbAware {
        @Nonnull
        protected final TextDiffSettings mySettings;

        public ToggleExpandByDefaultAction(@Nonnull TextDiffSettings settings) {
            super(DiffLocalize.collapseUnchangedFragments(), AllIcons.Actions.Collapseall);
            mySettings = settings;
            setEnabledInModalContext(true);
        }

        @Override
        public boolean isVisible() {
            return mySettings.getContextRange() != -1;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return !mySettings.isExpandByDefault();
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            boolean expand = !state;
            if (mySettings.isExpandByDefault() == expand) {
                return;
            }
            mySettings.setExpandByDefault(expand);
            expandAll(expand);
        }

        protected abstract void expandAll(boolean expand);
    }

    public static abstract class ReadOnlyLockAction extends ToggleAction implements DumbAware {
        @Nonnull
        protected final DiffContext myContext;
        @Nonnull
        protected final TextDiffSettings mySettings;

        public ReadOnlyLockAction(@Nonnull DiffContext context) {
            super("Disable editing", null, AllIcons.Nodes.Padlock);
            myContext = context;
            mySettings = getTextSettings(context);
            setEnabledInModalContext(true);
        }

        protected void applyDefaults() {
            if (isVisible()) { // apply default state
                setSelected(null, isSelected(null));
            }
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            if (!isVisible()) {
                e.getPresentation().setEnabledAndVisible(false);
            }
            else {
                super.update(e);
            }
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return mySettings.isReadOnlyLock();
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            mySettings.setReadOnlyLock(state);
            doApply(state);
        }

        private boolean isVisible() {
            return myContext.getUserData(DiffUserDataKeysEx.SHOW_READ_ONLY_LOCK) == Boolean.TRUE && canEdit();
        }

        protected abstract void doApply(boolean readOnly);

        protected abstract boolean canEdit();
    }

    public static class EditorReadOnlyLockAction extends ReadOnlyLockAction {
        private final List<? extends EditorEx> myEditableEditors;

        public EditorReadOnlyLockAction(@Nonnull DiffContext context, @Nonnull List<? extends EditorEx> editableEditors) {
            super(context);
            myEditableEditors = editableEditors;
            applyDefaults();
        }

        @Override
        protected void doApply(boolean readOnly) {
            for (EditorEx editor : myEditableEditors) {
                editor.setViewer(readOnly);
            }
        }

        @Override
        protected boolean canEdit() {
            return !myEditableEditors.isEmpty();
        }
    }

    @Nonnull
    public static List<? extends EditorEx> getEditableEditors(@Nonnull List<? extends EditorEx> editors) {
        return ContainerUtil.filter(editors, (Condition<EditorEx>)editor -> !editor.isViewer());
    }

    public static class EditorFontSizeSynchronizer implements PropertyChangeListener {
        @Nonnull
        private final List<? extends EditorEx> myEditors;

        private boolean myDuringUpdate = false;

        public EditorFontSizeSynchronizer(@Nonnull List<? extends EditorEx> editors) {
            myEditors = editors;
        }

        public void install(@Nonnull Disposable disposable) {
            if (myEditors.size() < 2) {
                return;
            }
            for (EditorEx editor : myEditors) {
                editor.addPropertyChangeListener(this, disposable);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (myDuringUpdate) {
                return;
            }

            if (!EditorEx.PROP_FONT_SIZE.equals(evt.getPropertyName())) {
                return;
            }
            if (evt.getOldValue().equals(evt.getNewValue())) {
                return;
            }
            int fontSize = (Integer)evt.getNewValue();

            for (EditorEx editor : myEditors) {
                if (evt.getSource() != editor) {
                    updateEditor(editor, fontSize);
                }
            }
        }

        public void updateEditor(@Nonnull EditorEx editor, int fontSize) {
            try {
                myDuringUpdate = true;
                editor.setFontSize(fontSize);
            }
            finally {
                myDuringUpdate = false;
            }
        }
    }

    public static class EditorActionsPopup {
        @Nonnull
        private final List<? extends AnAction> myEditorPopupActions;

        public EditorActionsPopup(@Nonnull List<? extends AnAction> editorPopupActions) {
            myEditorPopupActions = editorPopupActions;
        }

        public void install(@Nonnull List<? extends EditorEx> editors, @Nonnull JComponent component) {
            ActionUtil.recursiveRegisterShortcutSet(new DefaultActionGroup(myEditorPopupActions), component, null);

            EditorPopupHandler handler =
                new ContextMenuPopupHandler.Simple(myEditorPopupActions.isEmpty() ? null : new DefaultActionGroup(myEditorPopupActions));
            for (EditorEx editor : editors) {
                editor.installPopupHandler(handler);
            }
        }
    }
}