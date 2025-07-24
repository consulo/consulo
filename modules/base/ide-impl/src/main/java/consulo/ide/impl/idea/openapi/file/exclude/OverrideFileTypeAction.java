// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.file.exclude;

import consulo.annotation.component.ActionImpl;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ActionImpl(id = "OverrideFileTypeAction")
public class OverrideFileTypeAction extends AnAction {
    public OverrideFileTypeAction() {
        super(
            ActionLocalize.actionOverridefiletypeactionText(),
            ActionLocalize.actionOverridefiletypeactionDescription()
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        VirtualFile[] files = getContextFiles(e, file -> OverrideFileTypeManager.getInstance().getFileValue(file) == null);
        boolean enabled = files.length != 0;
        Presentation presentation = e.getPresentation();
        presentation.setDescriptionValue(
            enabled
                ? ActionLocalize.actionOverridefiletypeactionVerboseDescription(files[0].getName(), files.length - 1)
                : ActionLocalize.actionOverridefiletypeactionDescription()
        );
        presentation.setEnabledAndVisible(enabled);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VirtualFile[] files = getContextFiles(e, file -> OverrideFileTypeManager.getInstance().getFileValue(file) == null);
        if (files.length == 0) {
            return;
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        // although well-behaved types have unique names, file types coming from plugins can be wild
        Map<LocalizeValue, List<LocalizeValue>> duplicates = Arrays.stream(FileTypeManager.getInstance().getRegisteredFileTypes())
            .map(FileType::getDisplayName)
            .collect(Collectors.groupingBy(Function.identity()));

        List<FileType> sortedFileTypes = ContainerUtil.sorted(
            Arrays.asList(FileTypeManager.getInstance().getRegisteredFileTypes()),
            (f1, f2) -> f1.getDisplayName().compareIgnoreCase(f2.getDisplayName())
        );
        for (FileType type : sortedFileTypes) {
            if (!OverrideFileTypeManager.isOverridable(type)) {
                continue;
            }
            boolean hasDuplicate = duplicates.get(type.getDisplayName()).size() > 1;
            String dupHint = null;
            if (hasDuplicate) {
                PluginDescriptor descriptor = PluginManager.getPlugin(type.getClass());
                dupHint = descriptor == null
                    ? null
                    : " (" +
                    (descriptor.isBundled()
                        ? ActionLocalize.groupOverridefiletypeactionBundledplugin()
                        : ActionLocalize.groupOverridefiletypeactionFromnamedplugin(descriptor.getName())) +
                    ")";
            }

            String finalDupHint = dupHint;
            LocalizeValue displayText = type.getDisplayName()
                .map((localizeManager, s) -> s + StringUtil.notNullize(finalDupHint));
            group.add(new ChangeToThisFileTypeAction(displayText, files, type));
        }
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                ActionLocalize.groupOverridefiletypeactionTitle().get(),
                group.build(),
                e.getDataContext(),
                false,
                null,
                -1
            )
            .showInBestPositionFor(e.getDataContext());
    }

    @Nonnull
    static VirtualFile[] getContextFiles(@Nonnull AnActionEvent e, @Nonnull Predicate<? super VirtualFile> additionalPredicate) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (files == null) {
            return VirtualFile.EMPTY_ARRAY;
        }
        return Arrays.stream(files)
            .filter(file -> file != null && !file.isDirectory())
            .filter(additionalPredicate)
            .toArray(VirtualFile.ARRAY_FACTORY::create);
    }

    private static class ChangeToThisFileTypeAction extends AnAction {
        private final
        @Nonnull
        VirtualFile[] myFiles;
        private final FileType myType;

        ChangeToThisFileTypeAction(@Nonnull LocalizeValue displayText, @Nonnull VirtualFile[] files, @Nonnull FileType type) {
            super(
                displayText,
                ActionLocalize.actionChangetothisfiletypeactionDescription(type.getDescription()),
                type.getIcon()
            );
            myFiles = files;
            myType = type;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            for (VirtualFile file : myFiles) {
                if (file.isValid() && !file.isDirectory() && OverrideFileTypeManager.isOverridable(file.getFileType())) {
                    OverrideFileTypeManager.getInstance().addFile(file, myType);
                }
            }
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            boolean enabled = ContainerUtil.exists(
                myFiles,
                file -> file.isValid() && !file.isDirectory() && OverrideFileTypeManager.isOverridable(file.getFileType())
            );
            e.getPresentation().setEnabled(enabled);
        }
    }
}
