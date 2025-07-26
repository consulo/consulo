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
package consulo.ui.ex.action;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ActionAPI;
import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayFactory;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents an entity that has a state, a presentation and can be performed.
 * <p>
 * For an action to be useful, you need to implement {@link AnAction#actionPerformed}
 * and optionally to override {@link AnAction#update}. By overriding the
 * {@link AnAction#update} method you can dynamically change action's presentation
 * depending on the place (for more information on places see {@link ActionPlaces}.
 * <p>
 * The same action can have various presentations.
 * <p>
 * <pre>
 *  public class MyAction extends AnAction {
 *    public MyAction() {
 *      // ...
 *    }
 *
 *    public void update(AnActionEvent e) {
 *      Presentation presentation = e.getPresentation();
 *      if (e.getPlace().equals(ActionPlaces.MAIN_MENU)) {
 *        presentation.setText("My Menu item name");
 *      } else if (e.getPlace().equals(ActionPlaces.MAIN_TOOLBAR)) {
 *        presentation.setText("My Toolbar item name");
 *      }
 *    }
 *
 *    public void actionPerformed(AnActionEvent e) { ... }
 *  }
 * </pre>
 *
 * @see AnActionEvent
 * @see Presentation
 * @see ActionPlaces
 */
@ActionAPI
public abstract class AnAction implements PossiblyDumbAware {
    @Nonnull
    @Deprecated
    @DeprecationInfo("Use constructors with LocalizeValue parameters")
    @SuppressWarnings("deprecation")
    public static AnAction create(
        @Nonnull String text,
        @Nullable String description,
        @Nullable Image image,
        @RequiredUIAccess @Nonnull Consumer<AnActionEvent> actionPerformed
    ) {
        return new AnAction(text, description, image) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    @Nonnull
    public static AnAction create(
        @Nonnull LocalizeValue text,
        @Nullable LocalizeValue description,
        @Nullable Image image,
        @RequiredUIAccess @Nonnull Consumer<AnActionEvent> actionPerformed
    ) {
        return new AnAction(text, description == null ? LocalizeValue.empty() : description, image) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                actionPerformed.accept(e);
            }
        };
    }

    public static final AnAction[] EMPTY_ARRAY = new AnAction[0];

    public static ArrayFactory<AnAction> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new AnAction[count];

    public static final Key<List<AnAction>> ACTIONS_KEY = Key.create("AnAction.shortcutSet");

    private Presentation myTemplatePresentation;
    private ShortcutSet myShortcutSet = CustomShortcutSet.EMPTY;
    private boolean myEnabledInModalContext;
    private boolean myIsDefaultIcon = true;
    private boolean myWorksInInjected;

    /**
     * Creates a new action with its text, description and icon set to <code>null</code>.
     */
    public AnAction() {
        // avoid eagerly creating template presentation
    }

    /**
     * Creates a new action with <code>icon</code> provided. Its text, description set to <code>null</code>.
     * <p>
     * If creating action via @{@link ActionImpl} use {@link #getTemplateIcon()}
     *
     * @param icon Default icon to appear in toolbars and menus (Note some platform don't have icons in menu).
     */
    public AnAction(@Nullable Image icon) {
        this(LocalizeValue.empty(), LocalizeValue.empty(), icon);
    }

    /**
     * Creates a new action with the specified text. Description and icon are
     * set to <code>null</code>.
     *
     * @param text Serves as a tooltip when the presentation is a button and the name of the
     *             menu item when the presentation is a menu item.
     */
    @Deprecated
    @DeprecationInfo("Use constructors with LocalizeValue parameters")
    @SuppressWarnings("deprecation")
    public AnAction(@Nullable String text) {
        this(text, null, null);
    }

    @Deprecated
    @DeprecationInfo("Use constructors with LocalizeValue parameters")
    @SuppressWarnings("deprecation")
    public AnAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        Presentation presentation = getTemplatePresentation();
        presentation.setText(text);
        presentation.setDescription(description);
        presentation.setIcon(icon);
    }

    public AnAction(@Nonnull LocalizeValue text) {
        this(text, LocalizeValue.empty(), null);
    }

    public AnAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        this(text, description, null);
    }

    public AnAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        Presentation presentation = getTemplatePresentation();
        presentation.setTextValue(text);
        presentation.setDescriptionValue(description);
        presentation.setIcon(icon);
    }

    /**
     * Returns the shortcut set associated with this action.
     *
     * @return shortcut set associated with this action
     */
    public final ShortcutSet getShortcutSet() {
        return myShortcutSet;
    }

    /**
     * Registers a set of shortcuts that will be processed when the specified component
     * is the ancestor of focused component. Note that the action doesn't have
     * to be registered in action manager in order for that shortcut to work.
     *
     * @param shortcutSet the shortcuts for the action.
     * @param component   the component for which the shortcuts will be active.
     */
    public final void registerCustomShortcutSet(@Nonnull ShortcutSet shortcutSet, @Nullable JComponent component) {
        registerCustomShortcutSet(shortcutSet, component, (Disposable) null);
    }

    public final void registerCustomShortcutSet(int keyCode, @JdkConstants.InputEventMask int modifiers, @Nullable JComponent component) {
        registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(keyCode, modifiers)), component);
    }

    public final void registerCustomShortcutSet(
        @Nonnull ShortcutSet shortcutSet,
        @Nullable JComponent component,
        @Nullable Disposable parentDisposable
    ) {
        setShortcutSet(shortcutSet);
        registerCustomShortcutSet(component, parentDisposable);
    }

    public final void registerCustomShortcutSet(@Nullable JComponent component, @Nullable Disposable parentDisposable) {
        if (component == null) {
            return;
        }
        List<AnAction> actionList = (List<AnAction>) component.getClientProperty(ACTIONS_KEY);
        if (actionList == null) {
            component.putClientProperty(ACTIONS_KEY, actionList = new ArrayList<>());
        }
        if (!actionList.contains(this)) {
            actionList.add(this);
        }

        if (parentDisposable != null) {
            Disposer.register(parentDisposable, () -> unregisterCustomShortcutSet(component));
        }
    }

    public final void unregisterCustomShortcutSet(JComponent component) {
        List<AnAction> actionList = (List<AnAction>) component.getClientProperty(ACTIONS_KEY);
        if (actionList != null) {
            actionList.remove(this);
        }
    }

    /**
     * Copies template presentation and shortcuts set from <code>sourceAction</code>.
     *
     * @param sourceAction cannot be <code>null</code>
     */
    public final void copyFrom(@Nonnull AnAction sourceAction) {
        Presentation sourcePresentation = sourceAction.getTemplatePresentation();
        Presentation presentation = getTemplatePresentation();
        presentation.copyFrom(sourcePresentation);
        copyShortcutFrom(sourceAction);
    }

    public final void copyShortcutFrom(@Nonnull AnAction sourceAction) {
        myShortcutSet = sourceAction.myShortcutSet;
    }


    public final boolean isEnabledInModalContext() {
        return myEnabledInModalContext;
    }

    protected final void setEnabledInModalContext(boolean enabledInModalContext) {
        myEnabledInModalContext = enabledInModalContext;
    }

    /**
     * Override with true returned if your action has to display its text along with the icon when placed in the toolbar
     */
    public boolean displayTextInToolbar() {
        return false;
    }

    /**
     * Updates the state of the action. Default implementation does nothing.
     * Override this method to provide the ability to dynamically change action's
     * state and(or) presentation depending on the context (For example
     * when your action state depends on the selection you can check for
     * selection and change the state accordingly).
     * This method can be called frequently, for instance, if an action is added to a toolbar,
     * it will be updated twice a second. This means that this method is supposed to work really fast,
     * no real work should be done at this phase. For example, checking selection in a tree or a list,
     * is considered valid, but working with a file system is not. If you cannot understand the state of
     * the action fast you should do it in the {@link #actionPerformed(AnActionEvent)} method and notify
     * the user that action cannot be executed if it's the case.
     *
     * @param e Carries information on the invocation place and data available
     */
    public void update(@Nonnull AnActionEvent e) {
    }

    /**
     * Same as {@link #update(AnActionEvent)} but is calls immediately before actionPerformed() as final check guard.
     * Default implementation delegates to {@link #update(AnActionEvent)}.
     *
     * @param e Carries information on the invocation place and data available
     */
    public void beforeActionPerformedUpdate(@Nonnull AnActionEvent e) {
        boolean worksInInjected = isInInjectedContext();
        e.setInjectedContext(worksInInjected);
        update(e);
        if (!e.getPresentation().isEnabled() && worksInInjected) {
            e.setInjectedContext(false);
            update(e);
        }
    }

    /**
     * Returns a template presentation that will be used
     * as a template for created presentations.
     *
     * @return template presentation
     */
    @Nonnull
    public final Presentation getTemplatePresentation() {
        Presentation presentation = myTemplatePresentation;
        if (presentation == null) {
            myTemplatePresentation = presentation = createTemplatePresentation();
        }
        return presentation;
    }

    @Nonnull
    protected Presentation createTemplatePresentation() {
        Presentation presentation = new Presentation();
        presentation.setIcon(getTemplateIcon());
        return presentation;
    }

    /**
     * Icon from template presentation, without creating presentation. Don't use constructor if registering it via @ActionImpl
     */
    @Nullable
    @Deprecated
    @DeprecationInfo("Use creating via constructor")
    protected Image getTemplateIcon() {
        return null;
    }

    /**
     * Implement this method to provide your action handler.
     *
     * @param e Carries information on the invocation place
     */
    @RequiredUIAccess
    public abstract void actionPerformed(@Nonnull AnActionEvent e);

    public void setShortcutSet(ShortcutSet shortcutSet) {
        myShortcutSet = shortcutSet;
    }

    /**
     * Sets the flag indicating whether the action has an internal or a user-customized icon.
     *
     * @param isDefaultIconSet true if the icon is internal, false if the icon is customized by the user.
     */
    public void setDefaultIcon(boolean isDefaultIconSet) {
        myIsDefaultIcon = isDefaultIconSet;
    }

    /**
     * Returns true if the action has an internal, not user-customized icon.
     *
     * @return true if the icon is internal, false if the icon is customized by the user.
     */
    public boolean isDefaultIcon() {
        return myIsDefaultIcon;
    }

    public void setInjectedContext(boolean worksInInjected) {
        myWorksInInjected = worksInInjected;
    }

    public boolean isInInjectedContext() {
        return myWorksInInjected;
    }

    @Override
    public boolean isDumbAware() {
        return this instanceof DumbAware;
    }

    /**
     * Specifies the thread and the way {@link AnAction#update(AnActionEvent)},
     * {@link ActionGroup#getChildren(AnActionEvent)} or other update-like methods shall be called.
     * <p>
     * The preferred value is {@link ActionUpdateThread#BGT}.
     * <p>
     * The default value is {@link ActionUpdateThread#EDT}.
     *
     * @see ActionUpdateThread
     */
    @Nonnull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Return weight of action while execution on mouse/keyboard press handling
     *
     * @return weight
     */
    public int getExecuteWeight() {
        return 0;
    }

    /**
     * Returns default action text.
     * This method must be overridden in case template presentation contains user data like Project name,
     * Run Configuration name, etc
     *
     * @return action presentable text without private user data
     */
    @Nullable
    public String getTemplateText() {
        return getTemplatePresentation().getText();
    }

    @Override
    public String toString() {
        return getTemplatePresentation().toString();
    }
}
