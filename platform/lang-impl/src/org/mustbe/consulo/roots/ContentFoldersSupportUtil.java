package org.mustbe.consulo.roots;

import com.intellij.openapi.roots.ModifiableRootModel;
import consulo.roots.ContentFolderTypeProvider;
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
    for (ContentFolderSupportPatcher patcher : ContentFolderSupportPatcher.EP_NAME.getExtensions()) {
      patcher.patch(moduleRootManager, providers);
    }
    providers.add(ExcludedContentFolderTypeProvider.getInstance());
    return providers;
  }
}
