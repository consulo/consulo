// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.editor.inspection.PriorityAction;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.intention.EmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.intention.QuickFixWrapper;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CachedIntentions {
    private static final Logger LOG = Logger.getInstance(CachedIntentions.class);

    private final Set<IntentionActionWithTextCaching> myIntentions = Sets.newConcurrentHashSet(ACTION_TEXT_AND_CLASS_EQUALS);
    private final Set<IntentionActionWithTextCaching> myErrorFixes = Sets.newConcurrentHashSet(ACTION_TEXT_AND_CLASS_EQUALS);
    private final Set<IntentionActionWithTextCaching> myInspectionFixes = Sets.newConcurrentHashSet(ACTION_TEXT_AND_CLASS_EQUALS);
    private final Set<IntentionActionWithTextCaching> myGutters = Sets.newConcurrentHashSet(ACTION_TEXT_AND_CLASS_EQUALS);
    private final Set<IntentionActionWithTextCaching> myNotifications = Sets.newConcurrentHashSet(ACTION_TEXT_AND_CLASS_EQUALS);
    private int myOffset;
    @Nullable
    private final Editor myEditor;
    @Nonnull
    private final PsiFile myFile;
    @Nonnull
    private final Project myProject;

    public CachedIntentions(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor) {
        myProject = project;
        myFile = file;
        myEditor = editor;
    }

    @Nonnull
    public Set<IntentionActionWithTextCaching> getIntentions() {
        return myIntentions;
    }

    @Nonnull
    public Set<IntentionActionWithTextCaching> getErrorFixes() {
        return myErrorFixes;
    }

    @Nonnull
    public Set<IntentionActionWithTextCaching> getInspectionFixes() {
        return myInspectionFixes;
    }

    @Nonnull
    public Set<IntentionActionWithTextCaching> getGutters() {
        return myGutters;
    }

    @Nonnull
    public Set<IntentionActionWithTextCaching> getNotifications() {
        return myNotifications;
    }

    @Nullable
    public Editor getEditor() {
        return myEditor;
    }

    @Nonnull
    public PsiFile getFile() {
        return myFile;
    }

    @Nonnull
    public Project getProject() {
        return myProject;
    }

    public int getOffset() {
        return myOffset;
    }

    @Nonnull
    public static CachedIntentions create(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor, @Nonnull IntentionsInfo intentions) {
        CachedIntentions res = new CachedIntentions(project, file, editor);
        res.wrapAndUpdateActions(intentions, false);
        return res;
    }

    @Nonnull
    public static CachedIntentions createAndUpdateActions(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor, @Nonnull IntentionsInfo intentions) {
        CachedIntentions res = new CachedIntentions(project, file, editor);
        res.wrapAndUpdateActions(intentions, true);
        return res;
    }

    private static final HashingStrategy<IntentionActionWithTextCaching> ACTION_TEXT_AND_CLASS_EQUALS = new HashingStrategy<>() {
        @Override
        public int hashCode(IntentionActionWithTextCaching object) {
            return object.getText().hashCode();
        }

        @Override
        public boolean equals(IntentionActionWithTextCaching o1, IntentionActionWithTextCaching o2) {
            return getActionClass(o1) == getActionClass(o2) && o1.getText().equals(o2.getText());
        }

        private Class<? extends IntentionAction> getActionClass(IntentionActionWithTextCaching o1) {
            return IntentionActionDelegate.unwrap(o1.getAction()).getClass();
        }
    };

    @RequiredReadAction
    public boolean wrapAndUpdateActions(@Nonnull IntentionsInfo newInfo, boolean callUpdate) {
        myOffset = newInfo.getOffset();
        boolean changed = wrapActionsTo(newInfo.errorFixesToShow, myErrorFixes, callUpdate);
        changed |= wrapActionsTo(newInfo.inspectionFixesToShow, myInspectionFixes, callUpdate);
        changed |= wrapActionsTo(newInfo.intentionsToShow, myIntentions, callUpdate);
        changed |= wrapActionsTo(newInfo.guttersToShow, myGutters, callUpdate);
        changed |= wrapActionsTo(newInfo.notificationActionsToShow, myNotifications, callUpdate);
        return changed;
    }

    public boolean addActions(@Nonnull IntentionsInfo info) {
        boolean changed = addActionsTo(info.errorFixesToShow, myErrorFixes);
        changed |= addActionsTo(info.inspectionFixesToShow, myInspectionFixes);
        changed |= addActionsTo(info.intentionsToShow, myIntentions);
        changed |= addActionsTo(info.guttersToShow, myGutters);
        changed |= addActionsTo(info.notificationActionsToShow, myNotifications);
        return changed;
    }

    private boolean addActionsTo(@Nonnull List<? extends IntentionActionDescriptor> newDescriptors, @Nonnull Set<? super IntentionActionWithTextCaching> cachedActions) {
        boolean changed = false;
        for (IntentionActionDescriptor descriptor : newDescriptors) {
            changed |= cachedActions.add(wrapAction(descriptor, myFile, myFile, myEditor));
        }
        return changed;
    }

    @RequiredReadAction
    private boolean wrapActionsTo(@Nonnull List<? extends IntentionActionDescriptor> newDescriptors,
                                  @Nonnull Set<IntentionActionWithTextCaching> cachedActions,
                                  boolean shouldCallIsAvailable) {
        if (cachedActions.isEmpty() && newDescriptors.isEmpty()) {
            return false;
        }
        boolean changed = false;
        if (myEditor == null) {
            LOG.assertTrue(!shouldCallIsAvailable);
            for (IntentionActionDescriptor descriptor : newDescriptors) {
                changed |= cachedActions.add(wrapAction(descriptor, myFile, myFile, null));
            }
            return changed;
        }
        int caretOffset = myEditor.getCaretModel().getOffset();
        int fileOffset = caretOffset > 0 && caretOffset == myFile.getTextLength() ? caretOffset - 1 : caretOffset;
        PsiElement element;
        PsiElement hostElement;
        if (myFile instanceof PsiCompiledElement) {
            hostElement = element = myFile;
        }
        else if (PsiDocumentManager.getInstance(myProject).isUncommited(myEditor.getDocument())) {
            //???
            FileViewProvider viewProvider = myFile.getViewProvider();
            hostElement = element = viewProvider.findElementAt(fileOffset, viewProvider.getBaseLanguage());
        }
        else {
            hostElement = myFile.getViewProvider().findElementAt(fileOffset, myFile.getLanguage());
            element = InjectedLanguageManager.getInstance(myProject).findElementAtNoCommit(myFile, fileOffset);
        }
        PsiFile injectedFile;
        Editor injectedEditor;
        if (element == null || element == hostElement) {
            injectedFile = myFile;
            injectedEditor = myEditor;
        }
        else {
            injectedFile = element.getContainingFile();
            injectedEditor = InjectedEditorManager.getInstance(myProject).getInjectedEditorForInjectedFile(myEditor, injectedFile);
        }

        ShowIntentionInternal internal = ShowIntentionInternal.getInstance();

        if (shouldCallIsAvailable) {
            for (Iterator<IntentionActionWithTextCaching> iterator = cachedActions.iterator(); iterator.hasNext(); ) {
                IntentionActionWithTextCaching cachedAction = iterator.next();
                IntentionAction action = cachedAction.getAction();
                Pair<PsiFile, Editor> applicableIn = internal.chooseBetweenHostAndInjected(myFile, myEditor, injectedFile, (f, e) -> internal.availableFor(f, e, action));
                if (applicableIn == null) {
                    iterator.remove();
                    changed = true;
                }
            }
        }

        Set<IntentionActionWithTextCaching> wrappedNew = Sets.newHashSet(newDescriptors.size(), ACTION_TEXT_AND_CLASS_EQUALS);
        for (IntentionActionDescriptor descriptor : newDescriptors) {
            IntentionAction action = descriptor.getAction();
            if (element != null && element != hostElement && (!shouldCallIsAvailable || internal.availableFor(injectedFile, injectedEditor, action))) {
                IntentionActionWithTextCaching cachedAction = wrapAction(descriptor, element, injectedFile, injectedEditor);
                wrappedNew.add(cachedAction);
                changed |= cachedActions.add(cachedAction);
            }
            else if (hostElement != null && (!shouldCallIsAvailable || internal.availableFor(myFile, myEditor, action))) {
                IntentionActionWithTextCaching cachedAction = wrapAction(descriptor, hostElement, myFile, myEditor);
                wrappedNew.add(cachedAction);
                changed |= cachedActions.add(cachedAction);
            }
        }
        for (Iterator<IntentionActionWithTextCaching> iterator = cachedActions.iterator(); iterator.hasNext(); ) {
            IntentionActionWithTextCaching cachedAction = iterator.next();
            if (!wrappedNew.contains(cachedAction)) {
                // action disappeared
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Nonnull
    public IntentionActionWithTextCaching wrapAction(@Nonnull IntentionActionDescriptor descriptor,
                                                     @Nonnull PsiElement element,
                                                     @Nonnull PsiFile containingFile,
                                                     @Nullable Editor containingEditor) {
        IntentionActionWithTextCaching cachedAction = new IntentionActionWithTextCaching(descriptor, (cached, action) -> {
            if (action instanceof QuickFixWrapper) {
                // remove only inspection fixes after invocation,
                // since intention actions might be still available
                removeActionFromCached(cached);
                markInvoked(action);
            }
        });
        List<IntentionAction> options = descriptor.getOptions(element, containingEditor);
        if (options == null) {
            return cachedAction;
        }
        for (IntentionAction option : options) {
            Editor editor = ObjectUtil.chooseNotNull(myEditor, containingEditor);
            if (editor == null) {
                continue;
            }
            ShowIntentionInternal internal = ShowIntentionInternal.getInstance();
            Pair<PsiFile, Editor>
                availableIn = internal
                .chooseBetweenHostAndInjected(myFile, editor, containingFile, (f, e) -> internal.availableFor(f, e, option));
            if (availableIn == null) {
                continue;
            }
            IntentionActionWithTextCaching textCaching = new IntentionActionWithTextCaching(option);
            boolean isErrorFix = myErrorFixes.contains(textCaching);
            if (isErrorFix) {
                cachedAction.addErrorFix(option);
            }
            boolean isInspectionFix = myInspectionFixes.contains(textCaching);
            if (isInspectionFix) {
                cachedAction.addInspectionFix(option);
            }
            else {
                cachedAction.addIntention(option);
            }
        }
        return cachedAction;
    }

    private void markInvoked(@Nonnull IntentionAction action) {
        if (myEditor != null) {
            ShowIntentionInternal.getInstance().markActionInvoked(myFile.getProject(), myEditor, action);
        }
    }

    private void removeActionFromCached(@Nonnull IntentionActionWithTextCaching action) {
        // remove from the action from the list after invocation to make it appear unavailable sooner.
        // (the highlighting will process the whole file and remove the no more available action from the list automatically - but it's may be too long)
        myErrorFixes.remove(action);
        myGutters.remove(action);
        myInspectionFixes.remove(action);
        myIntentions.remove(action);
        myNotifications.remove(action);
    }

    @Nonnull
    public List<IntentionActionWithTextCaching> getAllActions() {
        List<IntentionActionWithTextCaching> result = new ArrayList<>(myErrorFixes);
        result.addAll(myInspectionFixes);
        result.addAll(myIntentions);
        result.addAll(myGutters);
        result.addAll(myNotifications);
        result = DumbService.getInstance(myProject).filterByDumbAwareness(result);
        result.sort((o1, o2) -> {
            int weight1 = getWeight(o1);
            int weight2 = getWeight(o2);
            if (weight1 != weight2) {
                return weight2 - weight1;
            }
            return o1.compareTo(o2);
        });
        return result;
    }

    private int getWeight(@Nonnull IntentionActionWithTextCaching action) {
        IntentionAction a = action.getAction();
        int group = getGroup(action).getPriority();
        while (a instanceof IntentionActionDelegate) {
            a = ((IntentionActionDelegate) a).getDelegate();
        }
        if (a instanceof PriorityAction) {
            return group + getPriorityWeight(((PriorityAction) a).getPriority());
        }
        if (a instanceof SuppressIntentionActionFromFix) {
            if (((SuppressIntentionActionFromFix) a).isShouldBeAppliedToInjectionHost() == ThreeState.NO) {
                return group - 1;
            }
        }
        return group;
    }

    private static int getPriorityWeight(PriorityAction.Priority priority) {
        switch (priority) {
            case TOP:
                return 20;
            case HIGH:
                return 3;
            case LOW:
                return -3;
            default:
                return 0;
        }
    }

    @Nonnull
    public IntentionGroup getGroup(@Nonnull IntentionActionWithTextCaching action) {
        if (myErrorFixes.contains(action)) {
            return IntentionGroup.ERROR;
        }
        if (myInspectionFixes.contains(action)) {
            return IntentionGroup.INSPECTION;
        }
        if (myNotifications.contains(action)) {
            return IntentionGroup.NOTIFICATION;
        }
        if (myGutters.contains(action)) {
            return IntentionGroup.GUTTER;
        }
        if (action.getAction() instanceof EmptyIntentionAction) {
            return IntentionGroup.EMPTY_ACTION;
        }
        return IntentionGroup.OTHER;
    }

    @Nonnull
    public Image getIcon(@Nonnull IntentionActionWithTextCaching value) {
        if (value.getIcon() != null) {
            return value.getIcon();
        }

        IntentionAction action = value.getAction();

        while (action instanceof IntentionActionDelegate) {
            action = ((IntentionActionDelegate) action).getDelegate();
        }
        Object iconable = action;
        //custom icon
        if (action instanceof QuickFixWrapper) {
            iconable = ((QuickFixWrapper) action).getFix();
        }

        if (iconable instanceof Iconable) {
            Image icon = ((Iconable) iconable).getIcon(0);
            if (icon != null) {
                return icon;
            }
        }

        if (IntentionManagerSettings.getInstance().isShowLightBulb(action)) {
            return myErrorFixes.contains(value) ? AllIcons.Actions.QuickfixBulb : myInspectionFixes.contains(value) ? AllIcons.Actions.IntentionBulb : Image.empty(Image.DEFAULT_ICON_SIZE);
        }
        else {
            if (myErrorFixes.contains(value)) {
                return AllIcons.Actions.QuickfixOffBulb;
            }
            return Image.empty(Image.DEFAULT_ICON_SIZE);
        }
    }

    public boolean showBulb() {
        return ContainerUtil.exists(getAllActions(), info -> IntentionManagerSettings.getInstance().isShowLightBulb(info.getAction()));
    }

    @Override
    public String toString() {
        return "CachedIntentions{" +
            "myIntentions=" + myIntentions +
            ", myErrorFixes=" + myErrorFixes +
            ", myInspectionFixes=" + myInspectionFixes +
            ", myGutters=" + myGutters +
            ", myNotifications=" + myNotifications +
            '}';
    }
}
