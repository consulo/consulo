/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.util.collection.Lists;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The production {@code IntentionManagerImpl} lives in {@code consulo.ide.impl}, together with the settings and the
 * option actions it offers. Headlessly every registered intention is enabled and offers no options, which is enough
 * for the alt-enter computation to query them.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessIntentionManager extends IntentionManager {
    private final List<IntentionAction> myActions = Lists.newLockFreeCopyOnWriteList();

    @Inject
    public HeadlessIntentionManager(Application application) {
        application.getExtensionPoint(IntentionAction.class).forEachExtensionSafe(myActions::add);
    }

    @Override
    public void addAction(IntentionAction action) {
        myActions.add(action);
    }

    @Override
    public IntentionAction[] getIntentionActions() {
        return myActions.toArray(IntentionAction.EMPTY_ARRAY);
    }

    @Override
    public IntentionAction[] getAvailableIntentionActions() {
        return getIntentionActions();
    }

    @Override
    public List<IntentionAction> getStandardIntentionOptions(HighlightDisplayKey displayKey, PsiElement context) {
        return List.of();
    }

    @Override
    public @Nullable IntentionAction createFixAllIntention(InspectionToolWrapper toolWrapper, IntentionAction action) {
        return null;
    }

    @Override
    public LocalQuickFix convertToFix(IntentionAction action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntentionAction createCleanupAllIntention() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntentionAction> getCleanupIntentionOptions() {
        return List.of();
    }
}
