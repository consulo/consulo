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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UnifiedSingleConfigurableEditor extends DialogDescriptor implements Disposable {
    private static final Logger LOG = Logger.getInstance(UnifiedSingleConfigurableEditor.class);

    private static final long MODIFICATION_CHECK_PERIOD = 500;

    private final Configurable myConfigurable;
    private final String myDimensionKey;

    private @Nullable UnifiedOptionsPage myPage;
    private @Nullable ScheduledFuture<?> myModificationCheck;
    private boolean myModified;
    private boolean myChangesWereApplied;
    private boolean myModificationCheckFailed;
    private boolean myDisposed;

    public UnifiedSingleConfigurableEditor(Configurable configurable, LocalizeValue title, String dimensionKey) {
        super(title.orIfEmpty(() -> createTitle(configurable)));
        myConfigurable = configurable;
        myDimensionKey = dimensionKey;
    }

    public Configurable getConfigurable() {
        return myConfigurable;
    }

    @Override
    public String getDimensionServiceKey() {
        return myDimensionKey;
    }

    @Override
    public WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(50, 25);
    }

    @Override
    public @Nullable String getHelpId() {
        String helpTopic = myConfigurable.getHelpTopic();
        return Configurable.DISABLED_HELP_ID.equals(helpTopic) ? null : helpTopic;
    }

    @Override
    public boolean hasDefaultContentBorder() {
        return false;
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        UnifiedOptionsPage page = new UnifiedOptionsPage(myConfigurable);
        myPage = page;
        Disposer.register(uiDisposable, this);

        if (page.isSupported()) {
            try {
                myConfigurable.initialize();
                myConfigurable.reset();
            }
            catch (Throwable e) {
                LOG.error(e);
            }

            startModificationCheck(UIAccess.current());
        }

        page.update(List.of(myConfigurable.getDisplayName()), null, null);
        return page.getComponent();
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        UnifiedOptionsPage page = myPage;
        if (page == null || !page.isSupported()) {
            return null;
        }
        return myConfigurable.getPreferredFocusedUIComponent();
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        AnAction okAction = createOkAction();
        AnAction cancelAction = new CancelAction();
        AnAction applyAction = new ApplyAction();

        if (inverseOrder) {
            return new AnAction[]{applyAction, cancelAction, okAction};
        }
        return new AnAction[]{okAction, cancelAction, applyAction};
    }

    @RequiredUIAccess
    public void runAdvancedInitialization(Runnable advancedInitialization) {
        UnifiedOptionsPage page = myPage;
        if (myDisposed || page == null || !page.isSupported()) {
            return;
        }

        try {
            advancedInitialization.run();
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (action instanceof DialogOkAction) {
            return apply();
        }
        return true;
    }

    @Override
    public void onHandleValue(AnAction action, @Nullable DialogValue value) {
        if (action instanceof DialogOkAction || myChangesWereApplied) {
            Application.get().saveAllWithProgress(UIAccess.current());
        }
    }

    @RequiredUIAccess
    private boolean apply() {
        UnifiedOptionsPage page = myPage;
        if (page == null || !page.isSupported()) {
            return true;
        }

        try {
            if (myConfigurable.isModified()) {
                myConfigurable.apply();
                myChangesWereApplied = true;
            }
            page.update(List.of(myConfigurable.getDisplayName()), null, null);
            return true;
        }
        catch (ConfigurationException e) {
            page.update(List.of(myConfigurable.getDisplayName()), null, e);

            String message = e.getMessage();
            if (message != null) {
                MessageBoxes.okError(LocalizeValue.of(message)).title(e.getTitle()).showAsync(page.getComponent());
            }
            return false;
        }
        finally {
            checkModified();
        }
    }

    @RequiredUIAccess
    private void startModificationCheck(UIAccess uiAccess) {
        myModificationCheck = uiAccess.getScheduler().scheduleWithFixedDelay(
            this::checkModified,
            MODIFICATION_CHECK_PERIOD,
            MODIFICATION_CHECK_PERIOD,
            TimeUnit.MILLISECONDS
        );
    }

    @RequiredUIAccess
    private void checkModified() {
        if (myDisposed) {
            return;
        }

        boolean modified;
        try {
            modified = myConfigurable.isModified();
        }
        catch (IndexNotReadyException e) {
            return;
        }
        catch (Throwable e) {
            if (!myModificationCheckFailed) {
                myModificationCheckFailed = true;
                LOG.error(e);
            }
            return;
        }

        if (modified != myModified) {
            myModified = modified;
            updateOkButtonState();
        }
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        ScheduledFuture<?> modificationCheck = myModificationCheck;
        if (modificationCheck != null) {
            modificationCheck.cancel(false);
        }

        try {
            myConfigurable.disposeUIResources();
        }
        catch (Throwable e) {
            LOG.error(e);
        }

        UnifiedOptionsPage page = myPage;
        if (page != null) {
            Disposer.dispose(page);
        }
    }

    private static LocalizeValue createTitle(Configurable configurable) {
        return configurable.getDisplayName().map(text -> text.replace('\n', ' '));
    }

    private class ApplyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ApplyAction() {
            super(CommonLocalize.buttonApply());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            apply();
            updateOkButtonState();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myModified);
        }
    }

    private class CancelAction extends DialogCancelAction implements AnActionWithSyncUpdate {
        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(myChangesWereApplied ? CommonLocalize.buttonClose() : CommonLocalize.buttonCancel());
        }
    }
}
