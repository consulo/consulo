// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.file.exclude;

import consulo.ui.ex.action.ActionsBundle;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.file.FileTypeManager;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OverrideFileTypeAction extends AnAction {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    VirtualFile[] files = getContextFiles(e, file -> OverrideFileTypeManager.getInstance().getFileValue(file) == null);
    boolean enabled = files.length != 0;
    Presentation presentation = e.getPresentation();
    presentation.setDescription(enabled
                                ? ActionsBundle.message("action.OverrideFileTypeAction.verbose.description", files[0].getName(), files.length - 1)
                                : ActionsBundle.message("action.OverrideFileTypeAction.description"));
    presentation.setEnabledAndVisible(enabled);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    VirtualFile[] files = getContextFiles(e, file -> OverrideFileTypeManager.getInstance().getFileValue(file) == null);
    if (files.length == 0) return;
    DefaultActionGroup group = new DefaultActionGroup();
    // although well-behaved types have unique names, file types coming from plugins can be wild
    Map<String, List<String>> duplicates = Arrays.stream(FileTypeManager.getInstance().getRegisteredFileTypes()).map(t -> t.getDisplayName()).collect(Collectors.groupingBy(Function.identity()));

    for (FileType type : ContainerUtil.sorted(Arrays.asList(FileTypeManager.getInstance().getRegisteredFileTypes()), (f1, f2) -> f1.getDisplayName().compareToIgnoreCase(f2.getDisplayName()))) {
      if (!OverrideFileTypeManager.isOverridable(type)) continue;
      boolean hasDuplicate = duplicates.get(type.getDisplayName()).size() > 1;
      String dupHint = null;
      if (hasDuplicate) {
        PluginDescriptor descriptor = PluginManager.getPlugin(type.getClass());
        dupHint = descriptor == null
                  ? null
                  : " (" +
                    (descriptor.isBundled()
                     ? ActionsBundle.message("group.OverrideFileTypeAction.bundledPlugin")
                     : ActionsBundle.message("group.OverrideFileTypeAction.fromNamedPlugin", descriptor.getName())) +
                    ")";
      }
      String displayText = type.getDisplayName() + StringUtil.notNullize(dupHint);
      group.add(new ChangeToThisFileTypeAction(displayText, files, type));
    }
    JBPopupFactory.getInstance().createActionGroupPopup(ActionsBundle.message("group.OverrideFileTypeAction.title"), group, e.getDataContext(), false, null, -1)
            .showInBestPositionFor(e.getDataContext());
  }

  @Nonnull
  static VirtualFile[] getContextFiles(@Nonnull AnActionEvent e, @Nonnull Predicate<? super VirtualFile> additionalPredicate) {
    VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
    if (files == null) return VirtualFile.EMPTY_ARRAY;
    return Arrays.stream(files).filter(file -> file != null && !file.isDirectory()).filter(additionalPredicate).toArray(count -> VirtualFile.ARRAY_FACTORY.create(count));
  }

  private static class ChangeToThisFileTypeAction extends AnAction {
    private final
    @Nonnull
    VirtualFile[] myFiles;
    private final FileType myType;

    ChangeToThisFileTypeAction(@Nonnull String displayText, @Nonnull VirtualFile[] files, @Nonnull FileType type) {
      super(displayText, ActionsBundle.message("action.ChangeToThisFileTypeAction.description", type.getDescription().get()), type.getIcon());
      myFiles = files;
      myType = type;
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      for (VirtualFile file : myFiles) {
        if (file.isValid() && !file.isDirectory() && OverrideFileTypeManager.isOverridable(file.getFileType())) {
          OverrideFileTypeManager.getInstance().addFile(file, myType);
        }
      }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      boolean enabled = ContainerUtil.exists(myFiles, file -> file.isValid() && !file.isDirectory() && OverrideFileTypeManager.isOverridable(file.getFileType()));
      e.getPresentation().setEnabled(enabled);
    }
  }
}
