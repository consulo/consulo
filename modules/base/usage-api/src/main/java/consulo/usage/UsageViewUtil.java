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

package consulo.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.ElementDescriptionUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.logging.Logger;
import consulo.navigation.NavigationItem;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class UsageViewUtil {
    private static final Logger LOG = Logger.getInstance(UsageViewUtil.class);

    private UsageViewUtil() {
    }

    @Nullable
    public static KeyboardShortcut getShowUsagesWithSettingsShortcut() {
        return ActionManager.getInstance().getKeyboardShortcut("ShowSettingsAndFindUsages");
    }

    public static KeyboardShortcut getShowUsagesWithSettingsShortcut(@Nonnull UsageTarget[] targets) {
        ConfigurableUsageTarget configurableTarget = getConfigurableTarget(targets);
        return configurableTarget == null ? getShowUsagesWithSettingsShortcut() : configurableTarget.getShortcut();
    }

    public static ConfigurableUsageTarget getConfigurableTarget(@Nonnull UsageTarget[] targets) {
        ConfigurableUsageTarget configurableUsageTarget = null;
        if (targets.length != 0) {
            NavigationItem target = targets[0];
            if (target instanceof ConfigurableUsageTarget) {
                configurableUsageTarget = (ConfigurableUsageTarget)target;
            }
        }
        return configurableUsageTarget;
    }

    public static String createNodeText(PsiElement element) {
        return ElementDescriptionUtil.getElementDescription(element, UsageViewNodeTextLocation.INSTANCE);
    }

    public static String getShortName(PsiElement psiElement) {
        LOG.assertTrue(psiElement.isValid(), psiElement);
        return ElementDescriptionUtil.getElementDescription(psiElement, UsageViewShortNameLocation.INSTANCE);
    }

    public static String getLongName(PsiElement psiElement) {
        LOG.assertTrue(psiElement.isValid(), psiElement);
        return ElementDescriptionUtil.getElementDescription(psiElement, UsageViewLongNameLocation.INSTANCE);
    }

    public static String getType(@Nonnull PsiElement psiElement) {
        return ElementDescriptionUtil.getElementDescription(psiElement, UsageViewTypeLocation.INSTANCE);
    }

    public static boolean hasNonCodeUsages(UsageInfo[] usages) {
        for (UsageInfo usage : usages) {
            if (usage.isNonCodeUsage) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    public static boolean hasUsagesInGeneratedCode(UsageInfo[] usages, Project project) {
        for (UsageInfo usage : usages) {
            VirtualFile file = usage.getVirtualFile();
            if (file != null) {
                if (GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, project)) {
                    return true;
                }
            }
        }

        return false;
    }

    @RequiredReadAction
    public static boolean hasReadOnlyUsages(UsageInfo[] usages) {
        for (UsageInfo usage : usages) {
            if (!usage.isWritable()) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    public static UsageInfo[] removeDuplicatedUsages(@Nonnull UsageInfo[] usages) {
        Set<UsageInfo> set = new LinkedHashSet<>(Arrays.asList(usages));

        // Replace duplicates of move rename usage infos in injections from non code usages of master files
        String newTextInNonCodeUsage = null;

        for (UsageInfo usage : usages) {
            if (!(usage instanceof NonCodeUsageInfo)) {
                continue;
            }
            newTextInNonCodeUsage = ((NonCodeUsageInfo)usage).newText;
            break;
        }

        if (newTextInNonCodeUsage != null) {
            for (UsageInfo usage : usages) {
                if (!(usage instanceof MoveRenameUsageInfo)) {
                    continue;
                }
                PsiFile file = usage.getFile();

                if (file != null) {
                    PsiElement context = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);
                    if (context != null) {

                        PsiElement usageElement = usage.getElement();
                        if (usageElement == null) {
                            continue;
                        }

                        PsiReference psiReference = usage.getReference();
                        if (psiReference == null) {
                            continue;
                        }

                        int injectionOffsetInMasterFile = InjectedLanguageManager.getInstance(usageElement.getProject())
                            .injectedToHost(usageElement, usageElement.getTextOffset());
                        TextRange rangeInElement = usage.getRangeInElement();
                        assert rangeInElement != null : usage;
                        TextRange range = rangeInElement.shiftRight(injectionOffsetInMasterFile);
                        PsiFile containingFile = context.getContainingFile();
                        if (containingFile == null) {
                            continue; //move package to another package
                        }
                        set.remove(NonCodeUsageInfo.create(
                            containingFile,
                            range.getStartOffset(),
                            range.getEndOffset(),
                            ((MoveRenameUsageInfo)usage).getReferencedElement(),
                            newTextInNonCodeUsage
                        ));
                    }
                }
            }
        }
        return set.toArray(new UsageInfo[set.size()]);
    }

    @Nonnull
    public static UsageInfo[] toUsageInfoArray(@Nonnull Collection<? extends UsageInfo> collection) {
        int size = collection.size();
        return size == 0 ? UsageInfo.EMPTY_ARRAY : collection.toArray(new UsageInfo[size]);
    }

    @Nonnull
    public static PsiElement[] toElements(@Nonnull UsageInfo[] usageInfos) {
        return ContainerUtil.map2Array(usageInfos, PsiElement.class, UsageInfo::getElement);
    }

    @RequiredReadAction
    public static void navigateTo(@Nonnull UsageInfo info, boolean requestFocus) {
        int offset = info.getNavigationOffset();
        VirtualFile file = info.getVirtualFile();
        Project project = info.getProject();
        if (file != null) {
            FileEditorManager.getInstance(project)
                .openTextEditor(OpenFileDescriptorFactory.getInstance(project).builder(file).offset(offset).build(), requestFocus);
        }
    }

    public static Set<UsageInfo> getNotExcludedUsageInfos(UsageView usageView) {
        Set<Usage> excludedUsages = usageView.getExcludedUsages();

        Set<UsageInfo> usageInfos = new LinkedHashSet<>();
        for (Usage usage : usageView.getUsages()) {
            if (usage instanceof UsageInfo2UsageAdapter && !excludedUsages.contains(usage)) {
                UsageInfo usageInfo = ((UsageInfo2UsageAdapter)usage).getUsageInfo();
                usageInfos.add(usageInfo);
            }
        }
        return usageInfos;
    }

    @RequiredReadAction
    public static boolean reportNonRegularUsages(UsageInfo[] usages, Project project) {
        boolean inGeneratedCode = hasUsagesInGeneratedCode(usages, project);
        if (hasNonCodeUsages(usages) || inGeneratedCode) {
            return true;
        }
        return false;
    }
}
