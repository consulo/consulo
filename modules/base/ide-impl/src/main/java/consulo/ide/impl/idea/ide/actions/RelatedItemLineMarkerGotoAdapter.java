/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.LineMarkersPass;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.gutter.RelatedItemLineMarkerProvider;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author nik
 */
@ExtensionImpl
public class RelatedItemLineMarkerGotoAdapter extends GotoRelatedProvider {
  @Nonnull
  @Override
  @RequiredReadAction
  public List<? extends GotoRelatedItem> getItems(@Nonnull PsiElement context) {
    List<PsiElement> parents = new ArrayList<>();
    PsiElement current = context;
    Set<Language> languages = new HashSet<>();
    while (current != null) {
      parents.add(current);
      languages.add(current.getLanguage());
      if (current instanceof PsiFile) break;
      current = current.getParent();
    }

    List<LineMarkerProvider> providers = new ArrayList<>();
    for (Language language : languages) {
      providers.addAll(LineMarkersPass.getMarkerProviders(context.getContainingFile(), language, context.getProject()));
    }

    List<GotoRelatedItem> items = new ArrayList<>();
    for (LineMarkerProvider provider : providers) {
      if (provider instanceof RelatedItemLineMarkerProvider) {
        List<RelatedItemLineMarkerInfo> markers = new ArrayList<>();
        RelatedItemLineMarkerProvider relatedItemLineMarkerProvider = (RelatedItemLineMarkerProvider)provider;
        for (PsiElement parent : parents) {
          ContainerUtil.addIfNotNull(markers, relatedItemLineMarkerProvider.getLineMarkerInfo(parent));
        }
        relatedItemLineMarkerProvider.collectNavigationMarkers(parents, markers, true);

        addItemsForMarkers(markers, items);
      }
    }

    return items;
  }

  private static void addItemsForMarkers(List<RelatedItemLineMarkerInfo> markers,
                                         List<GotoRelatedItem> result) {
    Set<PsiFile> addedFiles = new HashSet<>();
    for (RelatedItemLineMarkerInfo<?> marker : markers) {
      Collection<? extends GotoRelatedItem> items = marker.createGotoRelatedItems();
      for (GotoRelatedItem item : items) {
        PsiElement element = item.getElement();
        if (element instanceof PsiFile) {
          PsiFile file = (PsiFile)element;
          if (addedFiles.contains(file)) {
            continue;
          }
        }
        if (element != null) {
          ContainerUtil.addIfNotNull(addedFiles, element.getContainingFile());
        }
        result.add(item);
      }
    }
  }
}
