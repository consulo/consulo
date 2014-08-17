package com.intellij.compiler.impl;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 17.08.14
 */
public class ModuleRootCompilerContentIterator implements ContentIterator
{
  private final FileType myFileType;
  private final Collection<VirtualFile> myFiles;

  public ModuleRootCompilerContentIterator(FileType fileType, Collection<VirtualFile> files)
  {
    myFileType = fileType;
    myFiles = files;
  }

  @Override
  public boolean processFile(VirtualFile fileOrDir)
  {
    if(fileOrDir.isDirectory())
    {
      return true;
    }
    if(!fileOrDir.isInLocalFileSystem())
    {
      return true;
    }

    if(myFileType == null || myFileType == fileOrDir.getFileType())
    {
      myFiles.add(fileOrDir);
    }
    return true;
  }
}
