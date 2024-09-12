/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.rename;

import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.inplace.MemberInplaceRenameHandler;
import consulo.language.editor.ui.RadioUpDownListener;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author dsl
 */
public class RenameHandlerRegistry {
    public static final Key<Boolean> SELECT_ALL = Key.create("rename.selectAll");
    private final Set<RenameHandler> myHandlers = new HashSet<>();
    private static final RenameHandlerRegistry INSTANCE = new RenameHandlerRegistry();
    private final PsiElementRenameHandler myDefaultElementRenameHandler;

    public static RenameHandlerRegistry getInstance() {
        return INSTANCE;
    }

    private RenameHandlerRegistry() {
        // should be checked last
        myDefaultElementRenameHandler = new PsiElementRenameHandler();
    }

    public boolean hasAvailableHandler(DataContext dataContext) {
        for (RenameHandler renameHandler : RenameHandler.EP_NAME.getExtensionList()) {
            if (renameHandler.isAvailableOnDataContext(dataContext)) {
                return true;
            }
        }
        for (RenameHandler renameHandler : myHandlers) {
            if (renameHandler.isAvailableOnDataContext(dataContext)) {
                return true;
            }
        }
        return myDefaultElementRenameHandler.isAvailableOnDataContext(dataContext);
    }

    @Nullable
    @RequiredUIAccess
    public RenameHandler getRenameHandler(DataContext dataContext) {
        final Map<LocalizeValue, RenameHandler> availableHandlers = new TreeMap<>();
        RenameHandler.EP_NAME.forEachExtensionSafe(renameHandler -> {
            if (renameHandler.isRenaming(dataContext)) {
                availableHandlers.put(getHandlerTitle(renameHandler), renameHandler);
            }
        });

        for (RenameHandler renameHandler : myHandlers) {
            if (renameHandler.isRenaming(dataContext)) {
                availableHandlers.put(getHandlerTitle(renameHandler), renameHandler);
            }
        }

        if (availableHandlers.size() == 1) {
            return availableHandlers.values().iterator().next();
        }

        for (Iterator<Map.Entry<LocalizeValue, RenameHandler>> iterator = availableHandlers.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<LocalizeValue, RenameHandler> entry = iterator.next();
            if (entry.getValue() instanceof MemberInplaceRenameHandler) {
                iterator.remove();
                break;
            }
        }

        if (availableHandlers.size() == 1) {
            return availableHandlers.values().iterator().next();
        }

        if (availableHandlers.size() > 1) {
            final LocalizeValue[] variants = availableHandlers.keySet().toArray(LocalizeValue[]::new);
            final HandlersChooser chooser = new HandlersChooser(dataContext.getData(Project.KEY), variants);
            chooser.show();
            if (chooser.isOK()) {
                return availableHandlers.get(chooser.getSelection());
            }
            throw new ProcessCanceledException();
        }
        return myDefaultElementRenameHandler.isRenaming(dataContext) ? myDefaultElementRenameHandler : null;
    }

    @Nonnull
    private static LocalizeValue getHandlerTitle(RenameHandler renameHandler) {
        return renameHandler.getActionTitleValue().captilize();
    }

    /**
     * @see RenameHandler#EP_NAME
     * @deprecated
     */
    public void registerHandler(RenameHandler handler) {
        myHandlers.add(handler);
    }

    private static class HandlersChooser extends DialogWrapper {
        private final LocalizeValue[] myRenamers;
        private LocalizeValue mySelection;
        private final RadioButton[] myRButtons;

        protected HandlersChooser(Project project, LocalizeValue[] renamers) {
            super(project);
            myRenamers = renamers;
            myRButtons = new RadioButton[myRenamers.length];
            mySelection = renamers[0];
            setTitle(RefactoringLocalize.selectRefactoringTitle());
            init();
        }

        @Override
        @RequiredUIAccess
        protected JComponent createNorthPanel() {
            final VerticalLayout radioPanel = VerticalLayout.create();

            ValueGroup<Boolean> bg = ValueGroups.boolGroup();
            boolean selected = true;
            int rIdx = 0;
            for (final LocalizeValue renamer : myRenamers) {
                final RadioButton rb = RadioButton.create(renamer, selected);
                myRButtons[rIdx++] = rb;
                ComponentEventListener<ValueComponent<Boolean>, ValueComponentEvent<Boolean>> listener = event -> {
                    if (rb.getValueOrError()) {
                        mySelection = renamer;
                    }
                };
                rb.addValueListener(listener);
                selected = false;
                bg.add(rb);
                radioPanel.add(rb);
            }

            new RadioUpDownListener(Arrays.stream(myRButtons)
                .map(radioButton -> (JRadioButton) TargetAWT.to(radioButton))
                .toArray(JRadioButton[]::new));

            return (JComponent) TargetAWT.to(LabeledLayout.create(RefactoringLocalize.whatWouldYouLikeToDo(), radioPanel));
        }

        @Override
        @RequiredUIAccess
        public JComponent getPreferredFocusedComponent() {
            return (JComponent) TargetAWT.to(myRButtons[0]);
        }

        public LocalizeValue getSelection() {
            return mySelection;
        }

        @Override
        protected JComponent createCenterPanel() {
            return null;
        }
    }
}
