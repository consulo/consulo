// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.language;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.execution.impl.internal.action.ExecutorGroupActionGroup;
import consulo.execution.impl.internal.action.RunContextAction;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.language.editor.inspection.PriorityAction;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.ui.ex.action.ActionWithDelegate;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AsyncActionGroup;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class LineMarkerActionWrapper extends ActionGroup implements PriorityAction, ActionWithDelegate<AnAction>, AnActionWithSyncUpdate {
    private static final Logger LOG = Logger.getInstance(LineMarkerActionWrapper.class);
    public static final Key<Pair<PsiElement, MyDataContext>> LOCATION_WRAPPER = Key.create("LOCATION_WRAPPER");

    protected final PsiElement myElement;
    private final AnAction myOrigin;

    public LineMarkerActionWrapper(PsiElement element, AnAction origin) {
        myElement = element;
        myOrigin = origin;

        copyFrom(origin);

        if (!(myOrigin instanceof ActionGroup)) {
            getTemplatePresentation().setPerformGroup(true);
            getTemplatePresentation().setPopupGroup(true);
        }
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (myOrigin instanceof ExecutorAction o && o.getOrigin() instanceof ExecutorGroupActionGroup oo) {
            int order = o.getOrder();
            return ContainerUtil.map2Array(
                oo.getChildren(),
                EMPTY_ARRAY,
                action -> new LineMarkerActionWrapper(
                    myElement,
                    ExecutorAction.wrap(action, ((RunContextAction) action).getExecutor(), order)
                )
            );
        }
        if (myOrigin instanceof ActionGroup o && !(o instanceof AsyncActionGroup)) {
            return o.getChildren(e == null ? null : wrapEvent(e));
        }
        return EMPTY_ARRAY;
    }

    @Override
    public Coroutine<?, List<AnAction>> getChildrenAsync(@Nullable AnActionEvent e) {
        if (myOrigin instanceof ActionGroup o && !isExecutorGroupOrigin()) {
            return o.getChildrenAsync(e == null ? null : wrapEvent(e));
        }
        return super.getChildrenAsync(e);
    }

    private boolean isExecutorGroupOrigin() {
        return myOrigin instanceof ExecutorAction o && o.getOrigin() instanceof ExecutorGroupActionGroup;
    }

    @Override
    public boolean isDumbAware() {
        return myOrigin.isDumbAware();
    }

    @Override
    public boolean hideIfNoVisibleChildren() {
        return myOrigin instanceof ActionGroup actionGroup && actionGroup.hideIfNoVisibleChildren();
    }

    @Override
    public boolean disableIfNoVisibleChildren() {
        return !(myOrigin instanceof ActionGroup actionGroup) || actionGroup.disableIfNoVisibleChildren();
    }

    @Override
    public void update(AnActionEvent e) {
        AnActionEvent wrapped = wrapEvent(e);
        ActionUpdateInvoker.updateSync(myOrigin, wrapped);
        Image icon = wrapped.getPresentation().getIcon();
        if (icon != null) {
            getTemplatePresentation().setIcon(icon);
        }
    }

    
    private AnActionEvent wrapEvent(AnActionEvent e) {
        DataContext dataContext = wrapContext(e.getDataContext());
        return new AnActionEvent(e.getInputEvent(), dataContext, e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
    }

    
    private DataContext wrapContext(DataContext dataContext) {
        Pair<PsiElement, MyDataContext> pair = DataManager.getInstance().loadFromDataContext(dataContext, LOCATION_WRAPPER);
        if (pair == null || pair.first != myElement) {
            pair = Pair.pair(myElement, new MyDataContext(dataContext));
            DataManager.getInstance().saveInDataContext(dataContext, LOCATION_WRAPPER, pair);
        }
        return pair.second;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        myOrigin.actionPerformed(wrapEvent(e));
    }

    
    @Override
    public Priority getPriority() {
        return Priority.TOP;
    }

    
    @Override
    public AnAction getDelegate() {
        return myOrigin;
    }

    private class MyDataContext extends UserDataHolderBase implements DataContext {
        private final DataContext myDelegate;

        MyDataContext(DataContext delegate) {
            myDelegate = delegate;
        }

        @Override
        @RequiredReadAction
        @SuppressWarnings("unchecked")
        public synchronized <T> @Nullable T getData(Key<T> dataId) {
            if (Location.DATA_KEY == dataId) {
                return myElement.isValid() ? (T) new PsiLocation<>(myElement) : null;
            }
            return myDelegate.getData(dataId);
        }
    }
}