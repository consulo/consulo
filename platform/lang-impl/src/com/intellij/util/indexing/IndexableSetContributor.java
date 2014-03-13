package com.intellij.util.indexing;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author peter
 */
public abstract class IndexableSetContributor {
  public static final ExtensionPointName<IndexableSetContributor> EP_NAME = new ExtensionPointName<IndexableSetContributor>("com.intellij.indexedRootsContributor");

  @NotNull
  public Set<VirtualFile> getAdditionalProjectRootsToIndex(@Nullable Project project) {
    return Collections.emptySet();
  }

  public abstract Set<VirtualFile> getAdditionalRootsToIndex();
}
