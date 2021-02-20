// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.*;
import com.intellij.openapi.actionSystem.ShortcutProvider;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.PossiblyDumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class IntentionActionWithTextCaching implements Comparable<IntentionActionWithTextCaching>, PossiblyDumbAware, ShortcutProvider, IntentionActionDelegate {
  private static final Logger LOG = Logger.getInstance(IntentionActionWithTextCaching.class);
  private final List<IntentionAction> myOptionIntentions = new ArrayList<>();
  private final List<IntentionAction> myOptionErrorFixes = new ArrayList<>();
  private final List<IntentionAction> myOptionInspectionFixes = new ArrayList<>();
  private final String myText;
  private final IntentionAction myAction;
  private final String myDisplayName;
  private final Image myIcon;

  IntentionActionWithTextCaching(@Nonnull IntentionAction action) {
    this(action, action.getText(), null, (__1, __2) -> {
    });
  }

  IntentionActionWithTextCaching(@Nonnull HighlightInfo.IntentionActionDescriptor descriptor, @Nonnull BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> markInvoked) {
    this(descriptor.getAction(), descriptor.getDisplayName(), descriptor.getIcon(), markInvoked);
  }

  private IntentionActionWithTextCaching(@Nonnull IntentionAction action,
                                         String displayName,
                                         @Nullable Image icon,
                                         @Nonnull BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> markInvoked) {
    myIcon = icon;
    myText = action.getText();
    // needed for checking errors in user written actions
    //noinspection ConstantConditions
    LOG.assertTrue(myText != null, "action " + action.getClass() + " text returned null");
    myAction = new MyIntentionAction(action, markInvoked);
    myDisplayName = displayName;
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  void addIntention(@Nonnull IntentionAction action) {
    myOptionIntentions.add(action);
  }

  void addErrorFix(@Nonnull IntentionAction action) {
    myOptionErrorFixes.add(action);
  }

  void addInspectionFix(@Nonnull IntentionAction action) {
    myOptionInspectionFixes.add(action);
  }

  @Nonnull
  public IntentionAction getAction() {
    return myAction;
  }

  @Nonnull
  List<IntentionAction> getOptionIntentions() {
    return myOptionIntentions;
  }

  @Nonnull
  List<IntentionAction> getOptionErrorFixes() {
    return myOptionErrorFixes;
  }

  @Nonnull
  List<IntentionAction> getOptionInspectionFixes() {
    return myOptionInspectionFixes;
  }

  @Nonnull
  public List<IntentionAction> getOptionActions() {
    return ContainerUtil.concat(myOptionIntentions, myOptionErrorFixes, myOptionInspectionFixes);
  }

  String getToolName() {
    return myDisplayName;
  }

  @Override
  @Nonnull
  public String toString() {
    return getText();
  }

  @Override
  public int compareTo(@Nonnull final IntentionActionWithTextCaching other) {
    if (myAction instanceof Comparable) {
      //noinspection unchecked
      return ((Comparable)myAction).compareTo(other.getAction());
    }
    if (other.getAction() instanceof Comparable) {
      //noinspection unchecked
      return -((Comparable)other.getAction()).compareTo(myAction);
    }
    return Comparing.compare(getText(), other.getText());
  }

  Image getIcon() {
    return myIcon;
  }

  @Override
  public boolean isDumbAware() {
    return DumbService.isDumbAware(myAction);
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    return myAction instanceof ShortcutProvider ? ((ShortcutProvider)myAction).getShortcut() : null;
  }

  @Nonnull
  @Override
  public IntentionAction getDelegate() {
    return getAction();
  }

  public boolean isShowSubmenu() {
    IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
    if (action instanceof CustomizableIntentionAction) {
      return ((CustomizableIntentionAction)myAction).isShowSubmenu();
    }
    return true;
  }

  public boolean isSelectable() {
    IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
    if (action instanceof CustomizableIntentionAction) {
      return ((CustomizableIntentionAction)myAction).isSelectable();
    }
    return true;
  }

  public boolean isShowIcon() {
    IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
    if (action instanceof CustomizableIntentionAction) {
      return ((CustomizableIntentionAction)action).isShowIcon();
    }
    return true;
  }

  // IntentionAction which wraps the original action and then marks it as executed to hide it from the popup to avoid invoking it twice accidentally
  private class MyIntentionAction implements IntentionAction, CustomizableIntentionActionDelegate, Comparable<MyIntentionAction>, ShortcutProvider, PossiblyDumbAware {
    private final IntentionAction myAction;
    @Nonnull
    private final BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> myMarkInvoked;

    MyIntentionAction(@Nonnull IntentionAction action, @Nonnull BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> markInvoked) {
      myAction = action;
      myMarkInvoked = markInvoked;
    }

    @Override
    public boolean isDumbAware() {
      return DumbService.isDumbAware(myAction);
    }

    @Nonnull
    @Override
    public String getText() {
      return myText;
    }

    @Override
    public String toString() {
      return getDelegate().getClass() + ": " + getDelegate();
    }

    @Nls
    @Nonnull
    @Override
    public String getFamilyName() {
      return myAction.getFamilyName();
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
      return myAction.isAvailable(project, editor, file);
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      myAction.invoke(project, editor, file);
      myMarkInvoked.accept(IntentionActionWithTextCaching.this, myAction);
    }

    @Override
    public boolean startInWriteAction() {
      return myAction.startInWriteAction();
    }

    @Nonnull
    @Override
    public IntentionAction getDelegate() {
      return myAction;
    }

    @Override
    public
    @Nullable
    FileModifier getFileModifierForPreview(@Nonnull PsiFile target) {
      return myAction.getFileModifierForPreview(target);
    }

    @Nullable
    @Override
    public PsiElement getElementToMakeWritable(@Nonnull PsiFile currentFile) {
      return myAction.getElementToMakeWritable(currentFile);
    }

    @Nullable
    @Override
    public ShortcutSet getShortcut() {
      return myAction instanceof ShortcutProvider ? ((ShortcutProvider)myAction).getShortcut() : null;
    }

    @Override
    public int compareTo(@Nonnull final MyIntentionAction other) {
      if (myAction instanceof Comparable) {
        //noinspection unchecked
        return ((Comparable)myAction).compareTo(other.getDelegate());
      }
      if (other.getDelegate() instanceof Comparable) {
        //noinspection unchecked
        return -((Comparable)other.getDelegate()).compareTo(myAction);
      }
      return Comparing.compare(getText(), other.getText());
    }
  }
}
