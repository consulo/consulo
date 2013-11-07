package org.mustbe.consulo.roots;

import com.intellij.openapi.roots.ModifiableRootModel;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.impl.ExcludedContentFolderTypeProvider;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13:25/07.11.13
 */
public class ContentFoldersSupportUtil {
  @NotNull
  public static Set<ContentFolderTypeProvider> getSupportedFolders(ModifiableRootModel moduleRootManager) {
    Set<ContentFolderTypeProvider> providers = new LinkedHashSet<ContentFolderTypeProvider>();
    for (ModuleExtension moduleExtension : moduleRootManager.getExtensions()) {
      ContentFoldersSupport annotation = moduleExtension.getClass().getAnnotation(ContentFoldersSupport.class);
      if(annotation == null) {
        // if extension is mutable go get super class
        annotation = moduleExtension.getClass().getSuperclass().getAnnotation(ContentFoldersSupport.class);
      }
      if(annotation != null) {
        for (Class<? extends ContentFolderTypeProvider> o : annotation.value()) {
          ContentFolderTypeProvider folderTypeProvider = ContentFolderTypeProvider.EP_NAME.findExtension(o);
          if(folderTypeProvider != null) {
            providers.add(folderTypeProvider);
          }
        }
      }
    }
    providers.add(ExcludedContentFolderTypeProvider.getInstance());
    return providers;
  }
}
