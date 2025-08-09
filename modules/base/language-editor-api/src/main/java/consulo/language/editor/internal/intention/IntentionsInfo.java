/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.internal.intention;

import consulo.language.editor.intention.IntentionActionFilter;
import consulo.language.psi.PsiFile;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class IntentionsInfo {
    public final List<IntentionActionDescriptor> intentionsToShow = Lists.newLockFreeCopyOnWriteList();
    public final List<IntentionActionDescriptor> errorFixesToShow = Lists.newLockFreeCopyOnWriteList();
    public final List<IntentionActionDescriptor> inspectionFixesToShow = Lists.newLockFreeCopyOnWriteList();
    public final List<IntentionActionDescriptor> guttersToShow = Lists.newLockFreeCopyOnWriteList();
    public final List<IntentionActionDescriptor> notificationActionsToShow = Lists.newLockFreeCopyOnWriteList();
    private int myOffset;

    public void filterActions(@Nullable PsiFile psiFile) {
        List<IntentionActionFilter> filters = IntentionActionFilter.EXTENSION_POINT_NAME.getExtensionList();
        filter(intentionsToShow, psiFile, filters);
        filter(errorFixesToShow, psiFile, filters);
        filter(inspectionFixesToShow, psiFile, filters);
        filter(guttersToShow, psiFile, filters);
        filter(notificationActionsToShow, psiFile, filters);
    }

    public void setOffset(int offset) {
        myOffset = offset;
    }

    public int getOffset() {
        return myOffset;
    }

    private static void filter(
        @Nonnull List<IntentionActionDescriptor> descriptors,
        @Nullable PsiFile psiFile,
        @Nonnull List<IntentionActionFilter> filters
    ) {
        for (Iterator<IntentionActionDescriptor> it = descriptors.iterator(); it.hasNext(); ) {
            IntentionActionDescriptor actionDescriptor = it.next();
            for (IntentionActionFilter filter : filters) {
                if (!filter.accept(actionDescriptor.getAction(), psiFile)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    public boolean isEmpty() {
        return intentionsToShow.isEmpty() && errorFixesToShow.isEmpty() && inspectionFixesToShow.isEmpty() && guttersToShow.isEmpty() && notificationActionsToShow.isEmpty();
    }

    @Override
    public String toString() {
        return "Errors: " + errorFixesToShow + "; " +
            "Inspection fixes: " + inspectionFixesToShow + "; " +
            "Intentions: " + intentionsToShow + "; " +
            "Gutters: " + guttersToShow + "; " +
            "Notifications: " + notificationActionsToShow;
    }
}
