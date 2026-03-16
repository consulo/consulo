// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.internal.intention;

import consulo.application.dumb.PossiblyDumbAware;
import consulo.codeEditor.Editor;
import consulo.language.editor.inspection.FileModifier;
import consulo.language.editor.intention.CustomizableIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.ShortcutProvider;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class IntentionActionWithTextCaching implements Comparable<IntentionActionWithTextCaching>, PossiblyDumbAware, ShortcutProvider, IntentionActionDelegate {
    private static final Logger LOG = Logger.getInstance(IntentionActionWithTextCaching.class);
    private final List<IntentionAction> myOptionIntentions = new ArrayList<>();
    private final List<IntentionAction> myOptionErrorFixes = new ArrayList<>();
    private final List<IntentionAction> myOptionInspectionFixes = new ArrayList<>();
    private final LocalizeValue myText;
    private final IntentionAction myAction;
    private final LocalizeValue myDisplayName;
    private final Image myIcon;

    IntentionActionWithTextCaching(IntentionAction action) {
        this(action, action.getText(), null, (__1, __2) -> {
        });
    }

    IntentionActionWithTextCaching(IntentionActionDescriptor descriptor, BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> markInvoked) {
        this(descriptor.getAction(), descriptor.getDisplayName(), descriptor.getIcon(), markInvoked);
    }

    private IntentionActionWithTextCaching(IntentionAction action,
                                           LocalizeValue displayName,
                                           @Nullable Image icon,
                                           BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> markInvoked) {
        myIcon = icon;
        myText = action.getText();
        // needed for checking errors in user written actions
        //noinspection ConstantConditions
        LOG.assertTrue(myText != null, "action " + action.getClass() + " text returned null");
        myAction = new MyIntentionAction(action, markInvoked);
        myDisplayName = displayName;
    }

    
    public LocalizeValue getText() {
        return myText;
    }

    void addIntention(IntentionAction action) {
        myOptionIntentions.add(action);
    }

    void addErrorFix(IntentionAction action) {
        myOptionErrorFixes.add(action);
    }

    void addInspectionFix(IntentionAction action) {
        myOptionInspectionFixes.add(action);
    }

    
    public IntentionAction getAction() {
        return myAction;
    }

    
    public List<IntentionAction> getOptionIntentions() {
        return myOptionIntentions;
    }

    
    public List<IntentionAction> getOptionErrorFixes() {
        return myOptionErrorFixes;
    }

    
    public List<IntentionAction> getOptionInspectionFixes() {
        return myOptionInspectionFixes;
    }

    
    public List<IntentionAction> getOptionActions() {
        return ContainerUtil.concat(myOptionIntentions, myOptionErrorFixes, myOptionInspectionFixes);
    }

    public LocalizeValue getToolName() {
        return myDisplayName;
    }

    @Override
    
    public String toString() {
        return getText().get();
    }

    @Override
    public int compareTo(IntentionActionWithTextCaching other) {
        if (myAction instanceof Comparable) {
            //noinspection unchecked
            return ((Comparable) myAction).compareTo(other.getAction());
        }
        if (other.getAction() instanceof Comparable) {
            //noinspection unchecked
            return -((Comparable) other.getAction()).compareTo(myAction);
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
        return myAction instanceof ShortcutProvider ? ((ShortcutProvider) myAction).getShortcut() : null;
    }

    
    @Override
    public IntentionAction getDelegate() {
        return getAction();
    }

    public boolean isShowSubmenu() {
        IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
        if (action instanceof CustomizableIntentionAction) {
            return ((CustomizableIntentionAction) myAction).isShowSubmenu();
        }
        return true;
    }

    public boolean isSelectable() {
        IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
        if (action instanceof CustomizableIntentionAction) {
            return ((CustomizableIntentionAction) myAction).isSelectable();
        }
        return true;
    }

    public boolean isShowIcon() {
        IntentionAction action = IntentionActionDelegate.unwrap(getDelegate());
        if (action instanceof CustomizableIntentionAction) {
            return ((CustomizableIntentionAction) action).isShowIcon();
        }
        return true;
    }

    // IntentionAction which wraps the original action and then marks it as executed to hide it from the popup to avoid invoking it twice accidentally
    private class MyIntentionAction implements IntentionAction, CustomizableIntentionActionDelegate, Comparable<MyIntentionAction>, ShortcutProvider, PossiblyDumbAware {
        private final IntentionAction myAction;
        
        private final BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> myMarkInvoked;

        MyIntentionAction(IntentionAction action, BiConsumer<? super IntentionActionWithTextCaching, ? super IntentionAction> markInvoked) {
            myAction = action;
            myMarkInvoked = markInvoked;
        }

        @Override
        public boolean isDumbAware() {
            return DumbService.isDumbAware(myAction);
        }

        
        @Override
        public LocalizeValue getText() {
            return myText;
        }

        @Override
        public String toString() {
            return getDelegate().getClass() + ": " + getDelegate();
        }

        @Override
        public boolean isAvailable(Project project, Editor editor, PsiFile file) {
            return myAction.isAvailable(project, editor, file);
        }

        @Override
        public void invoke(Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            myAction.invoke(project, editor, file);
            myMarkInvoked.accept(IntentionActionWithTextCaching.this, myAction);
        }

        @Override
        public boolean startInWriteAction() {
            return myAction.startInWriteAction();
        }

        
        @Override
        public IntentionAction getDelegate() {
            return myAction;
        }

        @Override
        public
        @Nullable
        FileModifier getFileModifierForPreview(PsiFile target) {
            return myAction.getFileModifierForPreview(target);
        }

        @Nullable
        @Override
        public PsiElement getElementToMakeWritable(PsiFile currentFile) {
            return myAction.getElementToMakeWritable(currentFile);
        }

        @Nullable
        @Override
        public ShortcutSet getShortcut() {
            return myAction instanceof ShortcutProvider ? ((ShortcutProvider) myAction).getShortcut() : null;
        }

        @Override
        public int compareTo(MyIntentionAction other) {
            if (myAction instanceof Comparable) {
                //noinspection unchecked
                return ((Comparable) myAction).compareTo(other.getDelegate());
            }
            if (other.getDelegate() instanceof Comparable) {
                //noinspection unchecked
                return -((Comparable) other.getDelegate()).compareTo(myAction);
            }
            return Comparing.compare(getText(), other.getText());
        }
    }
}
