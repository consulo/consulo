// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.lineMarker;

import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class LineMarkerActionWrapper extends ActionGroup implements PriorityAction, ActionWithDelegate<AnAction> {
  private static final Logger LOG = Logger.getInstance(LineMarkerActionWrapper.class);
  public static final Key<Pair<PsiElement, MyDataContext>> LOCATION_WRAPPER = Key.create("LOCATION_WRAPPER");

  protected final PsiElement myElement;
  private final AnAction myOrigin;

  public LineMarkerActionWrapper(PsiElement element, @Nonnull AnAction origin) {
    myElement = element;
    myOrigin = origin;
    copyFrom(origin);
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    // This is quickfix for IDEA-208231
    // See com.intellij.codeInsight.daemon.impl.GutterIntentionMenuContributor.addActions(AnAction, List<? super IntentionActionDescriptor>, GutterIconRenderer, AtomicInteger, DataContext)`
    //if (myOrigin instanceof ExecutorAction) {
    //  if (((ExecutorAction)myOrigin).getOrigin() instanceof ExecutorRegistryImpl.ExecutorGroupActionGroup) {
    //    final AnAction[] children = ((ExecutorRegistryImpl.ExecutorGroupActionGroup)((ExecutorAction)myOrigin).getOrigin()).getChildren(null);
    //    LOG.assertTrue(ContainerUtil.all(Arrays.asList(children), o -> o instanceof RunContextAction));
    //    return ContainerUtil.map(children, o -> new LineMarkerActionWrapper(myElement, o)).toArray(AnAction.EMPTY_ARRAY);
    //  }
    //}
    //if (myOrigin instanceof ActionGroup) {
    //  return ((ActionGroup)myOrigin).getChildren(e == null ? null : wrapEvent(e));
    //}
    return AnAction.EMPTY_ARRAY;
  }

  @Override
  public boolean canBePerformed(@Nonnull DataContext context) {
    return !(myOrigin instanceof ActionGroup) || ((ActionGroup)myOrigin).canBePerformed(wrapContext(context));
  }

  @Override
  public boolean isDumbAware() {
    return myOrigin.isDumbAware();
  }

  @Override
  public boolean isPopup() {
    return !(myOrigin instanceof ActionGroup) || ((ActionGroup)myOrigin).isPopup();
  }

  @Override
  public boolean hideIfNoVisibleChildren() {
    return myOrigin instanceof ActionGroup && ((ActionGroup)myOrigin).hideIfNoVisibleChildren();
  }

  @Override
  public boolean disableIfNoVisibleChildren() {
    return !(myOrigin instanceof ActionGroup) || ((ActionGroup)myOrigin).disableIfNoVisibleChildren();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    AnActionEvent wrapped = wrapEvent(e);
    myOrigin.update(wrapped);
    Image icon = wrapped.getPresentation().getIcon();
    if (icon != null) {
      getTemplatePresentation().setIcon(icon);
    }
  }

  @Nonnull
  private AnActionEvent wrapEvent(@Nonnull AnActionEvent e) {
    DataContext dataContext = wrapContext(e.getDataContext());
    return new AnActionEvent(e.getInputEvent(), dataContext, e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
  }

  @Nonnull
  private DataContext wrapContext(DataContext dataContext) {
    Pair<PsiElement, MyDataContext> pair = DataManager.getInstance().loadFromDataContext(dataContext, LOCATION_WRAPPER);
    if (pair == null || pair.first != myElement) {
      pair = Pair.pair(myElement, new MyDataContext(dataContext));
      DataManager.getInstance().saveInDataContext(dataContext, LOCATION_WRAPPER, pair);
    }
    return pair.second;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    myOrigin.actionPerformed(wrapEvent(e));
  }

  @Nonnull
  @Override
  public Priority getPriority() {
    return Priority.TOP;
  }

  @Nonnull
  @Override
  public AnAction getDelegate() {
    return myOrigin;
  }

  private class MyDataContext extends UserDataHolderBase implements DataContext {
    private final DataContext myDelegate;

    MyDataContext(DataContext delegate) {
      myDelegate = delegate;
    }

    @Nullable
    @Override
    public synchronized Object getData(@Nonnull Key dataId) {
      if (Location.DATA_KEY == dataId) return myElement.isValid() ? new PsiLocation<>(myElement) : null;
      return myDelegate.getData(dataId);
    }
  }
}