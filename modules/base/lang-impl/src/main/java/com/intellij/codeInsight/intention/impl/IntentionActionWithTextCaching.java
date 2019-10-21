/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.openapi.actionSystem.ShortcutProvider;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.PossiblyDumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author cdr
 */
public class IntentionActionWithTextCaching implements Comparable<IntentionActionWithTextCaching>, PossiblyDumbAware, ShortcutProvider {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.intention.impl.IntentionActionWithTextCaching");
  private final List<IntentionAction> myOptionIntentions = new ArrayList<>();
  private final List<IntentionAction> myOptionErrorFixes = new ArrayList<>();
  private final List<IntentionAction> myOptionInspectionFixes = new ArrayList<>();
  private final String myText;
  private final IntentionAction myAction;
  private final String myDisplayName;
  private final Image myIcon;

  IntentionActionWithTextCaching(@Nonnull IntentionAction action){
    this(action, action.getText(), null, null);
  }

  IntentionActionWithTextCaching(@Nonnull HighlightInfo.IntentionActionDescriptor descriptor, @Nullable BiConsumer<IntentionActionWithTextCaching,IntentionAction> markInvoked){
    this(descriptor.getAction(), descriptor.getDisplayName(), descriptor.getIcon(), markInvoked);
  }

  private IntentionActionWithTextCaching(@Nonnull IntentionAction action, String displayName, @Nullable Image icon, @Nullable BiConsumer<IntentionActionWithTextCaching, IntentionAction> markInvoked) {
    myIcon = icon;
    myText = action.getText();
    // needed for checking errors in user written actions
    //noinspection ConstantConditions
    LOG.assertTrue(myText != null, "action "+action.getClass()+" text returned null");
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

  String getToolName() {
    return myDisplayName;
  }

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

  // IntentionAction which wraps the original action and then marks it as executed to hide it from the popup to avoid invoking it twice accidentally
  private class MyIntentionAction implements IntentionAction, IntentionActionDelegate, Comparable<MyIntentionAction>, ShortcutProvider {
    private final IntentionAction myAction;
    private final BiConsumer<IntentionActionWithTextCaching, IntentionAction> myMarkInvoked;

    MyIntentionAction(IntentionAction action, BiConsumer<IntentionActionWithTextCaching, IntentionAction> markInvoked) {
      myAction = action;
      myMarkInvoked = markInvoked;
    }

    @Nls
    @Nonnull
    @Override
    public String getText() {
      return myAction.getText();
    }

    @Override
    public String toString() {
      return getDelegate().getClass()+": "+getDelegate();
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
      try {
        myAction.invoke(project, editor, file);
      }
      finally {
        if (myMarkInvoked != null) {
          myMarkInvoked.accept(IntentionActionWithTextCaching.this, myAction);
        }
      }
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
