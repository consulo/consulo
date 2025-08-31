/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.ActionButtonComponent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo("Be careful while using this class. In most cases AnAction will be enough")
public abstract class AnActionButton extends AnAction implements ShortcutProvider {
    public static class AnActionButtonWrapper extends AnActionButton implements ActionWithDelegate<AnAction> {
        private final AnAction myAction;

        public AnActionButtonWrapper(Presentation presentation, @Nonnull AnAction action) {
            super(presentation.getText(), presentation.getDescription(), presentation.getIcon());
            myAction = action;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myAction.actionPerformed(new AnActionEventWrapper(e, this));
        }

        @Override
        public void updateButton(@Nonnull AnActionEvent e) {
            myAction.update(e);
            boolean enabled = e.getPresentation().isEnabled();
            boolean visible = e.getPresentation().isVisible();
            if (enabled && visible) {
                super.updateButton(e);
            }
        }

        @Override
        public boolean isDumbAware() {
            return myAction.isDumbAware();
        }

        @Nonnull
        @Override
        public AnAction getDelegate() {
            return myAction;
        }
    }

    public static class CheckedAnActionButton extends AnActionButtonWrapper implements CheckedActionGroup {
        private final AnAction myDelegate;

        public CheckedAnActionButton(Presentation presentation, AnAction action) {
            super(presentation, action);
            myDelegate = action;
        }

        @Nonnull
        @Override
        public AnAction getDelegate() {
            return myDelegate;
        }
    }

    public static final class AnActionEventWrapper extends AnActionEvent {
        private final AnActionButton myPeer;

        private AnActionEventWrapper(AnActionEvent e, AnActionButton peer) {
            super(e.getInputEvent(), e.getDataContext(), e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
            myPeer = peer;
        }

        public void showPopup(JBPopup popup) {
            popup.show(myPeer.getPreferredPopupPoint());
        }
    }

    private boolean myEnabled = true;
    private boolean myVisible = true;
    private ShortcutSet myShortcut;
    private AnAction myAction = null;
    private JComponent myContextComponent;
    private Set<AnActionButtonUpdater> myUpdaters;

    public AnActionButton(String text) {
        super(text);
    }

    public AnActionButton(String text, String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public AnActionButton(String text, Image icon) {
        this(text, null, icon);
    }

    protected AnActionButton(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected AnActionButton(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected AnActionButton(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    public AnActionButton() {
    }

    public static AnActionButton fromAction(AnAction action) {
        Presentation presentation = action.getTemplatePresentation();
        return action instanceof CheckedActionGroup
            ? new CheckedAnActionButton(presentation, action)
            : new AnActionButtonWrapper(presentation, action);
    }

    public boolean isEnabled() {
        return myEnabled;
    }

    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    public boolean isVisible() {
        return myVisible;
    }

    public void setVisible(boolean visible) {
        myVisible = visible;
    }

    @Override
    public final void update(@Nonnull AnActionEvent e) {
        boolean myActionVisible = true;
        boolean myActionEnabled = true;
        if (myAction != null) {
            myAction.update(e);
            myActionEnabled = e.getPresentation().isEnabled();
            myActionVisible = e.getPresentation().isVisible();
        }
        boolean enabled = isEnabled() && isContextComponentOk() && myActionEnabled;
        if (enabled && myUpdaters != null) {
            for (AnActionButtonUpdater updater : myUpdaters) {
                if (!updater.isEnabled(e)) {
                    enabled = false;
                    break;
                }
            }
        }
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(isVisible() && myActionVisible);

        if (enabled) {
            updateButton(e);
        }
    }

    public final void addCustomUpdater(@Nonnull AnActionButtonUpdater updater) {
        if (myUpdaters == null) {
            myUpdaters = new HashSet<>();
        }
        myUpdaters.add(updater);
    }

    public void updateButton(AnActionEvent e) {
        JComponent component = getContextComponent();
        e.getPresentation().setEnabled(component != null && component.isShowing() && component.isEnabled());
    }

    @Override
    public ShortcutSet getShortcut() {
        return myShortcut;
    }

    public void setShortcut(ShortcutSet shortcut) {
        myShortcut = shortcut;
    }

    public void setContextComponent(JComponent contextComponent) {
        myContextComponent = contextComponent;
    }

    public JComponent getContextComponent() {
        return myContextComponent;
    }

    private boolean isContextComponentOk() {
        return myContextComponent == null
            || (myContextComponent.isVisible() && UIUtil.getParentOfType(JLayeredPane.class, myContextComponent) != null);
    }

    public final RelativePoint getPreferredPopupPoint() {
        for (Container c = myContextComponent; (c = c.getParent()) != null;) {
            if (c instanceof JComponent jComponent
                && jComponent.getClientProperty(ActionToolbar.ACTION_TOOLBAR_PROPERTY_KEY) instanceof ActionToolbar toolbar) {
                for (Component comp : jComponent.getComponents()) {
                    if (comp instanceof ActionButtonComponent
                        && comp instanceof AnActionHolder actionHolder
                        && actionHolder.getIdeAction() == this) {
                        return new RelativePoint(comp.getParent(), new Point(comp.getX(), comp.getY() + comp.getHeight()));
                    }
                }
            }
        }
        return null;
    }
}
