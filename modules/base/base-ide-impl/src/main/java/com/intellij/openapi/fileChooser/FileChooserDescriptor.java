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
package com.intellij.openapi.fileChooser;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.DeprecationInfo;
import consulo.fileTypes.ArchiveFileType;
import consulo.fileTypes.impl.VfsIconUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.UILocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.fileOperateDialog.FileOperateDialogProvider;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @see FileChooserDescriptorFactory
 */
public class FileChooserDescriptor extends UserDataHolderBase implements Cloneable {
  private final boolean myChooseFiles;
  private final boolean myChooseFolders;
  private final boolean myChooseJars;
  private final boolean myChooseJarsAsFiles;
  private final boolean myChooseJarContents;
  private final boolean myChooseMultiple;

  @Nonnull
  private LocalizeValue myTitleValue = UILocalize.fileChooserDefaultTitle();
  @Nonnull
  private LocalizeValue myDescriptionValue = LocalizeValue.empty();

  private boolean myHideIgnored = true;
  private final List<VirtualFile> myRoots = new ArrayList<>();
  private boolean myShowFileSystemRoots = true;
  private boolean myTreeRootVisible = false;
  private boolean myShowHiddenFiles = false;
  private Condition<VirtualFile> myFileFilter = null;
  private String myForceOperateDialogProviderId = null;

  /**
   * Creates new instance. Use methods from {@link FileChooserDescriptorFactory} for most used descriptors.
   *
   * @param chooseFiles       controls whether files can be chosen
   * @param chooseFolders     controls whether folders can be chosen
   * @param chooseJars        controls whether .jar files can be chosen
   * @param chooseJarsAsFiles controls whether .jar files will be returned as files or as folders
   * @param chooseJarContents controls whether .jar file contents can be chosen
   * @param chooseMultiple    controls how many files can be chosen
   */
  public FileChooserDescriptor(boolean chooseFiles, boolean chooseFolders, boolean chooseJars, boolean chooseJarsAsFiles, boolean chooseJarContents, boolean chooseMultiple) {
    myChooseFiles = chooseFiles;
    myChooseFolders = chooseFolders;
    myChooseJars = chooseJars;
    myChooseJarsAsFiles = chooseJarsAsFiles;
    myChooseJarContents = chooseJarContents;
    myChooseMultiple = chooseMultiple;
  }

  public FileChooserDescriptor(@Nonnull FileChooserDescriptor d) {
    this(d.isChooseFiles(), d.isChooseFolders(), d.isChooseJars(), d.isChooseJarsAsFiles(), d.isChooseJarContents(), d.isChooseMultiple());
    withTitleValue(d.getTitleValue());
    withDescriptionValue(d.getDescriptionValue());
    withHideIgnored(d.isHideIgnored());
    withRoots(d.getRoots());
    withShowFileSystemRoots(d.isShowFileSystemRoots());
    withTreeRootVisible(d.isTreeRootVisible());
    withShowHiddenFiles(d.isShowHiddenFiles());
  }

  public boolean isChooseFiles() {
    return myChooseFiles;
  }

  public boolean isChooseFolders() {
    return myChooseFolders;
  }

  public boolean isChooseJars() {
    return myChooseJars;
  }

  public boolean isChooseJarsAsFiles() {
    return myChooseJarsAsFiles;
  }

  public boolean isChooseJarContents() {
    return myChooseJarContents;
  }

  public boolean isChooseMultiple() {
    return myChooseMultiple;
  }

  /**
   * @deprecated use {@link #isChooseMultiple()} (to be removed in IDEA 15)
   */
  @SuppressWarnings("UnusedDeclaration")
  public boolean getChooseMultiple() {
    return isChooseMultiple();
  }

  @Nonnull
  public LocalizeValue getTitleValue() {
    return myTitleValue;
  }

  @Nullable
  @Deprecated
  @DeprecationInfo("See #getTitleValue()")
  public String getTitle() {
    return StringUtil.nullize(myTitleValue.getValue());
  }

  @Deprecated
  @DeprecationInfo("Use #withTitleValue(LocalizeValue)")
  public void setTitle(String title) {
    withTitle(title);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #withTitleValue(LocalizeValue)")
  public FileChooserDescriptor withTitle(@Nullable String title) {
    myTitleValue = title == null ? LocalizeValue.empty() : LocalizeValue.of(title);
    return this;
  }

  @Nonnull
  public FileChooserDescriptor withTitleValue(@Nonnull LocalizeValue title) {
    myTitleValue = title;
    return this;
  }

  @Deprecated
  @DeprecationInfo("Use #withDescription(LocalizeValue)")
  public void setDescription(String description) {
    withDescription(description);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use #withDescription(LocalizeValue)")
  public FileChooserDescriptor withDescription(@Nullable String description) {
    myDescriptionValue = description == null ? LocalizeValue.empty() : LocalizeValue.of(description);
    return this;
  }

  @Nonnull
  public FileChooserDescriptor withDescriptionValue(@Nonnull LocalizeValue description) {
    myDescriptionValue = description;
    return this;
  }

  @Nullable
  @Deprecated
  @DeprecationInfo("See #getDescriptionValue()")
  public String getDescription() {
    return StringUtil.nullize(myDescriptionValue.get());
  }

  @Nonnull
  public LocalizeValue getDescriptionValue() {
    return myDescriptionValue;
  }

  public boolean isHideIgnored() {
    return myHideIgnored;
  }

  public void setHideIgnored(boolean hideIgnored) {
    withHideIgnored(hideIgnored);
  }

  public FileChooserDescriptor withHideIgnored(boolean hideIgnored) {
    myHideIgnored = hideIgnored;
    return this;
  }

  @Nonnull
  public List<VirtualFile> getRoots() {
    return Collections.unmodifiableList(myRoots);
  }

  public void setRoots(@Nonnull VirtualFile... roots) {
    withRoots(roots);
  }

  public void setRoots(@Nonnull List<VirtualFile> roots) {
    withRoots(roots);
  }

  public FileChooserDescriptor withRoots(final VirtualFile... roots) {
    return withRoots(Arrays.asList(roots));
  }

  public FileChooserDescriptor withRoots(@Nonnull List<VirtualFile> roots) {
    myRoots.clear();
    myRoots.addAll(roots);
    return this;
  }

  public boolean isShowFileSystemRoots() {
    return myShowFileSystemRoots;
  }

  public void setShowFileSystemRoots(boolean showFileSystemRoots) {
    withShowFileSystemRoots(showFileSystemRoots);
  }

  public FileChooserDescriptor withShowFileSystemRoots(boolean showFileSystemRoots) {
    myShowFileSystemRoots = showFileSystemRoots;
    return this;
  }

  public boolean isTreeRootVisible() {
    return myTreeRootVisible;
  }

  public FileChooserDescriptor withTreeRootVisible(boolean isTreeRootVisible) {
    myTreeRootVisible = isTreeRootVisible;
    return this;
  }

  /**
   * @deprecated use {@link #withTreeRootVisible(boolean)} (to be removed in IDEA 15)
   */
  @SuppressWarnings("UnusedDeclaration")
  public FileChooserDescriptor setIsTreeRootVisible(boolean treeRootVisible) {
    return withTreeRootVisible(treeRootVisible);
  }

  public boolean isShowHiddenFiles() {
    return myShowHiddenFiles;
  }

  public FileChooserDescriptor withShowHiddenFiles(boolean showHiddenFiles) {
    myShowHiddenFiles = showHiddenFiles;
    return this;
  }

  /**
   * Defines whether file can be chosen or not
   */
  @RequiredUIAccess
  public boolean isFileSelectable(VirtualFile file) {
    if (file == null) return false;

    if (file.is(VFileProperty.SYMLINK) && file.getCanonicalPath() == null) {
      return false;
    }
    if (file.isDirectory() && myChooseFolders) {
      return true;
    }

    if (myFileFilter != null && !file.isDirectory()) {
      return myFileFilter.value(file);
    }

    return acceptAsJarFile(file) || acceptAsGeneralFile(file);
  }

  /**
   * Sets simple boolean condition for use in {@link #isFileVisible(VirtualFile, boolean)} and {@link #isFileSelectable(VirtualFile)}.
   */
  public FileChooserDescriptor withFileFilter(@Nullable Condition<VirtualFile> filter) {
    myFileFilter = filter;
    return this;
  }

  /**
   * Defines whether file is visible in the tree
   */
  public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
    if (file.is(VFileProperty.SYMLINK) && file.getCanonicalPath() == null) {
      return false;
    }

    if (!file.isDirectory()) {
      if (FileElement.isArchive(file)) {
        if (!myChooseJars && !myChooseJarContents) {
          return false;
        }
      }
      else if (!myChooseFiles) {
        return false;
      }

      if (myFileFilter != null && !myFileFilter.value(file)) {
        return false;
      }
    }

    if (isHideIgnored() && FileTypeManager.getInstance().isFileIgnored(file)) {
      return false;
    }

    if (!showHiddenFiles && FileElement.isFileHidden(file)) {
      return false;
    }

    return true;
  }

  @Nullable
  public Image getIcon(final VirtualFile file) {
    return VfsIconUtil.getIcon(file, Iconable.ICON_FLAG_READ_STATUS, null);
  }

  @Nonnull
  protected static Image dressIcon(final VirtualFile file, final Image baseIcon) {
    return file.isValid() && file.is(VFileProperty.SYMLINK) ? ImageEffects.layered(baseIcon, AllIcons.Nodes.Symlink) : baseIcon;
  }

  public String getName(final VirtualFile file) {
    return file.getPath();
  }

  @Nullable
  public String getComment(final VirtualFile file) {
    return null;
  }

  /**
   * the method is called upon pressing Ok in the FileChooserDialog
   * Override the method in order to customize validation of user input
   *
   * @param files - selected files to be checked
   * @throws Exception if the the files cannot be accepted
   */
  public void validateSelectedFiles(VirtualFile[] files) throws Exception {
  }

  private boolean acceptAsGeneralFile(VirtualFile file) {
    if (FileElement.isArchive(file)) return false; // should be handle by acceptsAsJarFile
    return !file.isDirectory() && myChooseFiles;
  }

  @Nullable
  public String getForceOperateDialogProviderId() {
    return myForceOperateDialogProviderId;
  }

  public void setForceOperateDialogProviderId(@Nullable String forceOperateDialogProviderId) {
    myForceOperateDialogProviderId = forceOperateDialogProviderId;
  }

  public void setUseApplicationDialog() {
    myForceOperateDialogProviderId = FileOperateDialogProvider.APPLICATION_ID;
  }

  @Deprecated
  @DeprecationInfo("Use #setUseApplicationDialog()")
  public void setForcedToUseIdeaFileChooser(boolean forcedToUseIdeaFileChooser) {
    setForceOperateDialogProviderId(forcedToUseIdeaFileChooser ? FileOperateDialogProvider.APPLICATION_ID : null);
  }

  private boolean acceptAsJarFile(VirtualFile file) {
    return myChooseJars && FileElement.isArchive(file);
  }

  @Nullable
  public final VirtualFile getFileToSelect(VirtualFile file) {
    if (file.isDirectory() && (myChooseFolders || isFileSelectable(file))) {
      return file;
    }
    boolean isJar = file.getFileType() instanceof ArchiveFileType;
    if (!isJar) {
      return acceptAsGeneralFile(file) ? file : null;
    }
    if (myChooseJarsAsFiles) {
      return file;
    }
    if (!acceptAsJarFile(file)) {
      return null;
    }
    String path = file.getPath();
    return ((ArchiveFileType)file.getFileType()).getFileSystem().findLocalVirtualFileByPath(path);
  }

  @Override
  public final Object clone() {
    return super.clone();
  }

  @Override
  public String toString() {
    return "FileChooserDescriptor [" + myTitleValue + "]";
  }
}
