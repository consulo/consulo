/*
 * Copyright 2013-2023 consulo.io
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
package consulo.fileEditor.impl.internal;

import consulo.application.ui.UISettings;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 24/08/2023
 */
public abstract class AsyncConfigTreeReader<T> {
  public static final String PINNED = "pinned";
  public static final String CURRENT_IN_TAB = "current-in-tab";

  @Nonnull
  public CompletableFuture<T> process(@Nullable Element element, @Nullable T context, UIAccess uiAccess) {
    if (element == null) {
      return CompletableFuture.completedFuture(null);
    }

    final Element splitterElement = element.getChild("splitter");
    if (splitterElement != null) {
      final Element first = splitterElement.getChild("split-first");
      final Element second = splitterElement.getChild("split-second");
      return processSplitter(splitterElement, first, second, context, uiAccess);
    }

    final Element leaf = element.getChild("leaf");
    if (leaf == null) {
      return CompletableFuture.completedFuture(null);
    }

    List<Element> fileElements = leaf.getChildren("file");
    final List<Element> children = new ArrayList<>(fileElements.size());

    // trim to EDITOR_TAB_LIMIT, ignoring CLOSE_NON_MODIFIED_FILES_FIRST policy
    int toRemove = fileElements.size() - UISettings.getInstance().EDITOR_TAB_LIMIT;
    for (Element fileElement : fileElements) {
      if (toRemove <= 0 || Boolean.valueOf(fileElement.getAttributeValue(PINNED))) {
        children.add(fileElement);
      }
      else {
        toRemove--;
      }
    }

    return processFiles(children, context, leaf, uiAccess);
  }

  @Nonnull
  protected abstract CompletableFuture<T> processFiles(@Nonnull List<Element> fileElements,
                                                       @Nullable T context,
                                                       Element parent,
                                                       UIAccess uiAccess);

  @Nonnull
  protected abstract CompletableFuture<T> processSplitter(@Nonnull Element element,
                                                          @Nullable Element firstChild,
                                                          @Nullable Element secondChild,
                                                          @Nullable T context,
                                                          @Nonnull UIAccess uiAccess);
}
