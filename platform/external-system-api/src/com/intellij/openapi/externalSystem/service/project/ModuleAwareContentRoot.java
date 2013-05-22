package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @since 2/21/12 11:15 AM
 */
public class ModuleAwareContentRoot implements ContentEntry {

  @NotNull private final Module       myModule;
  @NotNull private final ContentEntry myDelegate;
  @NotNull private final VirtualFile  myFile;

  public ModuleAwareContentRoot(@NotNull Module module, @NotNull ContentEntry delegate) throws IllegalArgumentException {
    myDelegate = delegate;
    myModule = module;
    final VirtualFile file = delegate.getFile();
    if (file == null) {
      throw new IllegalArgumentException(String.format(
        "Detected attempt to create ModuleAwareContentRoot object for content root that points to the un-existing file - %s, module: %s",
        delegate, module
      ));
    }
    myFile = file;
  }

  @NotNull
  public Module getModule() {
    return myModule;
  }

  @NotNull
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Override
  @NotNull
  public String getUrl() {
    return myDelegate.getUrl();
  }

  @NotNull
  @Override
  public ContentFolder[] getFolders(@NotNull ContentFolderType contentFolderType) {
    return myDelegate.getFolders(contentFolderType);
  }

  @NotNull
  @Override
  public VirtualFile[] getFolderFiles(@NotNull ContentFolderType contentFolderType) {
    return myDelegate.getFolderFiles(contentFolderType);
  }

  @NotNull
  @Override
  public String[] getFolderUrls(@NotNull ContentFolderType contentFolderType) {
    return myDelegate.getFolderUrls(contentFolderType);
  }

  @Override
  public ContentFolder[] getFolders() {
    return myDelegate.getFolders();
  }

  @NotNull
  @Override
  public ContentFolder addFolder(@NotNull VirtualFile file, @NotNull ContentFolderType contentFolderType) {
    return myDelegate.addFolder(file, contentFolderType);
  }

  @NotNull
  @Override
  public ContentFolder addFolder(@NotNull String url, @NotNull ContentFolderType contentFolderType) {
    return myDelegate.addFolder(url, contentFolderType);
  }

  @Override
  public void removeFolder(@NotNull ContentFolder contentFolder) {
    myDelegate.removeFolder(contentFolder);

  }

  @Override
  public void clearFolders(@NotNull ContentFolderType contentFolderType) {
    myDelegate.clearFolders(contentFolderType);
  }

  @Override
  public boolean isSynthetic() {
    return myDelegate.isSynthetic();
  }
}
