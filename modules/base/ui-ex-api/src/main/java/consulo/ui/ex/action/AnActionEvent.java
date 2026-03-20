/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.dataContext.DataContext;
import consulo.dataContext.DataContextWrapper;
import consulo.dataContext.DataManager;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.PlaceProvider;
import consulo.ui.ex.action.event.AnActionEventVisitor;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import java.awt.event.InputEvent;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Container for the information necessary to execute or update an {@link AnAction}.
 *
 * @see AnAction#actionPerformed(AnActionEvent)
 * @see AnAction#update(AnActionEvent)
 */
public class AnActionEvent implements PlaceProvider<String> {
    private static final String ourInjectedPrefix = "$injected$.";

    // normal -> injected keys
    private static final Map<Key, Key> ourInjectedKeys = new IdentityHashMap<>();
    // injected -> normal keys
    private static final Map<Key, Key> ourUnInjectedKeys = new IdentityHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> Key<T> injectedId(Key<T> dataId) {
        synchronized (ourInjectedKeys) {
            Key injected = ourInjectedKeys.get(dataId);
            if (injected == null) {
                injected = Key.create(ourInjectedPrefix + dataId);

                ourInjectedKeys.put(dataId, injected);
                ourUnInjectedKeys.put(injected, dataId);
            }
            return injected;
        }
    }

    
    @SuppressWarnings("unchecked")
    public static <T> Key<T> uninjectedId(Key<T> injectedKey) {
        Key normalKey = ourUnInjectedKeys.get(injectedKey);
        if (normalKey == null) {
            normalKey = injectedKey;
        }
        return normalKey;
    }

    
    public static DataContext getInjectedDataContext(final DataContext context) {
        return new DataContextWrapper(context) {
            @Override
            public <T> @Nullable T getData(Key<T> dataId) {
                T injected = super.getData(injectedId(dataId));
                if (injected != null) {
                    return injected;
                }
                return super.getData(dataId);
            }
        };
    }

    private final InputEvent myInputEvent;
    
    private final ActionManager myActionManager;
    
    private final DataContext myDataContext;
    
    private final String myPlace;
    
    private final Presentation myPresentation;
    @JdkConstants.InputEventMask
    private final int myModifiers;
    private boolean myWorksInInjected;

    private final boolean myIsContextMenuAction;
    private final boolean myIsActionToolbar;

    private final InputDetails myInputDetails;

    /**
     * @throws IllegalArgumentException if {@code dataContext} is {@code null} or
     *                                  {@code place} is {@code null} or {@code presentation} is {@code null}
     * @see ActionManager#getInstance()
     */
    public AnActionEvent(
        InputEvent inputEvent,
        DataContext dataContext,
        String place,
        @Nullable Presentation presentation,
        ActionManager actionManager,
        @JdkConstants.InputEventMask int modifiers
    ) {
        this(
            inputEvent,
            dataContext,
            place,
            presentation == null ? new Presentation() : presentation,
            actionManager,
            modifiers,
            false,
            false
        );
    }

    public AnActionEvent(
        InputEvent inputEvent,
        DataContext dataContext,
        String place,
        Presentation presentation,
        ActionManager actionManager,
        @JdkConstants.InputEventMask int modifiers,
        boolean isContextMenuAction,
        boolean isActionToolbar
    ) {
        this(inputEvent, dataContext, place, presentation, actionManager, modifiers, isContextMenuAction, isActionToolbar, null);
    }

    public AnActionEvent(
        InputEvent inputEvent,
        DataContext dataContext,
        String place,
        Presentation presentation,
        ActionManager actionManager,
        @JdkConstants.InputEventMask int modifiers,
        boolean isContextMenuAction,
        boolean isActionToolbar,
        InputDetails inputDetails
    ) {
        // TODO[vova,anton] make this constructor package-private. No one is allowed to create AnActionEvents
        myInputEvent = inputEvent;
        myActionManager = actionManager;
        myDataContext = dataContext;
        myPlace = place;
        myPresentation = presentation;
        myModifiers = modifiers;
        myIsContextMenuAction = isContextMenuAction;
        myIsActionToolbar = isActionToolbar;
        myInputDetails = inputDetails;
    }

    @Deprecated
    
    public static AnActionEvent createFromInputEvent(AnAction action, @Nullable InputEvent event, String place) {
        DataContext context =
            event == null ? DataManager.getInstance().getDataContext() : DataManager.getInstance().getDataContext(event.getComponent());
        return createFromAnAction(action, event, place, context);
    }

    
    public static AnActionEvent createFromAnAction(
        AnAction action,
        @Nullable InputEvent event,
        String place,
        DataContext dataContext
    ) {
        return createFromAnAction(action, event, place, dataContext, null);
    }

    
    public static AnActionEvent createFromAnAction(
        AnAction action,
        @Nullable InputEvent event,
        String place,
        DataContext dataContext,
        @Nullable InputDetails inputDetails
    ) {
        int modifiers = event == null ? 0 : event.getModifiers();
        Presentation presentation = action.getTemplatePresentation().clone();
        AnActionEvent anActionEvent = new AnActionEvent(
            event,
            dataContext,
            place,
            presentation,
            ActionManager.getInstance(),
            modifiers,
            false,
            false,
            inputDetails
        );
        anActionEvent.setInjectedContext(action.isInInjectedContext());
        return anActionEvent;
    }

    
    public static AnActionEvent createFromDataContext(
        String place,
        @Nullable Presentation presentation,
        DataContext dataContext
    ) {
        return new AnActionEvent(
            null,
            dataContext,
            place,
            presentation == null ? new Presentation() : presentation,
            ActionManager.getInstance(),
            0
        );
    }

    
    public static AnActionEvent createFromInputEvent(
        @Nullable InputEvent event,
        String place,
        @Nullable Presentation presentation,
        DataContext dataContext
    ) {
        return new AnActionEvent(
            event,
            dataContext,
            place,
            presentation,
            ActionManager.getInstance(),
            event == null ? 0 : event.getModifiers()
        );
    }

    
    public static AnActionEvent createFromInputEvent(
        @Nullable InputEvent event,
        String place,
        Presentation presentation,
        DataContext dataContext,
        boolean isContextMenuAction,
        boolean isToolbarAction,
        @Nullable InputDetails inputDetails
    ) {
        return new AnActionEvent(
            event,
            dataContext,
            place,
            presentation,
            ActionManager.getInstance(),
            event == null ? 0 : event.getModifiers(),
            isContextMenuAction,
            isToolbarAction,
            inputDetails
        );
    }

    /**
     * Returns the <code>InputEvent</code> which causes invocation of the action. It might be
     * <code>KeyEvent</code>, <code>MouseEvent</code>.
     *
     * @return the <code>InputEvent</code> instance.
     */
    public InputEvent getInputEvent() {
        return myInputEvent;
    }

    /**
     * Returns the context which allows to retrieve information about the state of IDEA related to
     * the action invocation (active editor, selection and so on).
     *
     * @return the data context instance.
     */
    
    public DataContext getDataContext() {
        return myWorksInInjected ? getInjectedDataContext(myDataContext) : myDataContext;
    }

    public @Nullable <T> T getData(Key<T> key) {
        return getDataContext().getData(key);
    }

    public <T> boolean hasData(Key<T> key) {
        return getDataContext().hasData(key);
    }

    /**
     * Returns not null data by a data key. This method assumes that data has been checked for null in AnAction#update method.
     * <br/><br/>
     * Example of proper usage:
     * <p>
     * <pre>
     *
     * public class MyAction extends AnAction {
     *   public void update(AnActionEvent e) {
     *     //perform action if and only if EDITOR != null
     *     boolean enabled = e.getData(CommonDataKeys.EDITOR) != null;
     *     e.getPresentation.setEnabled(enabled);
     *   }
     *
     *   public void actionPerformed(AnActionEvent e) {
     *     //if we're here then EDITOR != null
     *     Document doc = e.getRequiredData(CommonDataKeys.EDITOR).getDocument();
     *     doSomething(doc);
     *   }
     * }
     *
     * </pre>
     */
    
    public <T> T getRequiredData(Key<T> key) {
        return getDataContext().getRequiredData(key);
    }

    /**
     * Returns the identifier of the place in the IDEA user interface from where the action is invoked
     * or updated.
     *
     * @return the place identifier
     * @see ActionPlaces
     */
    @Override
    
    public String getPlace() {
        return myPlace;
    }

    public boolean isFromActionToolbar() {
        return myIsActionToolbar;
    }

    public boolean isFromContextMenu() {
        return myIsContextMenuAction;
    }

    /**
     * Returns the presentation which represents the action in the place from where it is invoked
     * or updated.
     *
     * @return the presentation instance.
     */
    
    public Presentation getPresentation() {
        return myPresentation;
    }

    /**
     * Returns the modifier keys held down during this action event.
     *
     * @return the modifier keys.
     */
    @JdkConstants.InputEventMask
    public int getModifiers() {
        return myModifiers;
    }

    
    public ActionManager getActionManager() {
        return myActionManager;
    }

    public void setInjectedContext(boolean worksInInjected) {
        myWorksInInjected = worksInInjected;
    }

    public boolean isInInjectedContext() {
        return myWorksInInjected;
    }

    public final @Nullable InputDetails getInputDetails() {
        return myInputDetails;
    }

    public void accept(AnActionEventVisitor visitor) {
        visitor.visitEvent(this);
    }
}
