/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.language.editor.ui;

import consulo.dataContext.DataContext;
import consulo.language.editor.ui.awt.EditorAWTUtil;
import consulo.language.editor.ui.internal.LanguageEditorPopupFactory;
import consulo.language.editor.util.LanguageEditorNavigationUtil;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.EditSourceUtil;
import consulo.navigation.Navigatable;
import consulo.project.event.DumbModeListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author ven
 */
public final class PopupNavigationUtil {

  private PopupNavigationUtil() {
  }

  @Nonnull
  public static JBPopup getPsiElementPopup(@Nonnull PsiElement[] elements, String title) {
    return getPsiElementPopup(elements, new DefaultPsiElementCellRenderer(), title);
  }

  @Nonnull
  public static JBPopup getPsiElementPopup(@Nonnull PsiElement[] elements, @Nonnull final PsiElementListCellRenderer<PsiElement> renderer, final String title) {
    return getPsiElementPopup(elements, renderer, title, element -> {
      Navigatable descriptor = EditSourceUtil.getDescriptor(element);
      if (descriptor != null && descriptor.canNavigate()) {
        descriptor.navigate(true);
      }
      return true;
    });
  }

  @Nonnull
  public static <T extends PsiElement> JBPopup getPsiElementPopup(@Nonnull T[] elements,
                                                                  @Nonnull final PsiElementListCellRenderer<T> renderer,
                                                                  final String title,
                                                                  @Nonnull final PsiElementProcessor<T> processor) {
    return getPsiElementPopup(elements, renderer, title, processor, null);
  }

  @Nonnull
  public static <T extends PsiElement> JBPopup getPsiElementPopup(@Nonnull T[] elements,
                                                                  @Nonnull final PsiElementListCellRenderer<T> renderer,
                                                                  @Nullable final String title,
                                                                  @Nonnull final PsiElementProcessor<T> processor,
                                                                  @Nullable final T initialSelection) {
    assert elements.length > 0 : "Attempted to show a navigation popup with zero elements";
    IPopupChooserBuilder<T> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(List.of(elements)).setRenderer(renderer).setFont(EditorAWTUtil.getEditorFont()).withHintUpdateSupply();
    if (initialSelection != null) {
      builder.setSelectedValue(initialSelection, true);
    }
    if (title != null) {
      builder.setTitle(title);
    }
    renderer.installSpeedSearch(builder, true);

    JBPopup popup = builder.setItemsChosenCallback(selectedValues -> {
      for (T element : selectedValues) {
        if (element != null) {
          processor.execute(element);
        }
      }
    }).createPopup();

    hidePopupIfDumbModeStarts(popup, elements[0].getProject());

    return popup;
  }

  public static void hidePopupIfDumbModeStarts(@Nonnull JBPopup popup, @Nonnull Project project) {
    if (!DumbService.isDumb(project)) {
      project.getMessageBus().connect(popup).subscribe(DumbModeListener.class, new DumbModeListener() {
        @Override
        public void enteredDumbMode() {
          popup.cancel();
        }
      });
    }
  }

  public static boolean activateFileWithPsiElement(@Nonnull PsiElement elt) {
    return LanguageEditorNavigationUtil.activateFileWithPsiElement(elt);
  }

  public static boolean activateFileWithPsiElement(@Nonnull PsiElement elt, boolean searchForOpen) {
    return LanguageEditorNavigationUtil.activateFileWithPsiElement(elt, searchForOpen);
  }

  public static boolean openFileWithPsiElement(PsiElement element, boolean searchForOpen, boolean requestFocus) {
    return LanguageEditorNavigationUtil.openFileWithPsiElement(element, searchForOpen, requestFocus);
  }

  @Nonnull
  public static JBPopup getRelatedItemsPopup(final List<? extends GotoRelatedItem> items, String title) {
    return getRelatedItemsPopup(items, title, false);
  }

  /**
   * Returns navigation popup that shows list of related items from {@code items} list
   *
   * @param items
   * @param title
   * @param showContainingModules Whether the popup should show additional information that aligned at the right side of the dialog.<br>
   *                              It's usually a module name or library name of corresponding navigation item.<br>
   *                              {@code false} by default
   * @return
   */
  @Nonnull
  public static JBPopup getRelatedItemsPopup(final List<? extends GotoRelatedItem> items, String title, boolean showContainingModules) {
    Object[] elements = new Object[items.size()];
    //todo[nik] move presentation logic to GotoRelatedItem class
    final Map<PsiElement, GotoRelatedItem> itemsMap = new HashMap<>();
    for (int i = 0; i < items.size(); i++) {
      GotoRelatedItem item = items.get(i);
      elements[i] = item.getElement() != null ? item.getElement() : item;
      itemsMap.put(item.getElement(), item);
    }

    return LanguageEditorPopupFactory.getInstance().getPsiElementPopup(elements, itemsMap, title, showContainingModules, element -> {
      if (element instanceof PsiElement) {
        //noinspection SuspiciousMethodCalls
        itemsMap.get(element).navigate();
      }
      else {
        ((GotoRelatedItem)element).navigate();
      }
      return true;
    });
  }

  @Nonnull
  public static List<GotoRelatedItem> collectRelatedItems(@Nonnull PsiElement contextElement, @Nullable DataContext dataContext) {
    Set<GotoRelatedItem> items = new LinkedHashSet<>();
    GotoRelatedProvider.EP_NAME.forEachExtensionSafe(provider -> {
      items.addAll(provider.getItems(contextElement));
      if (dataContext != null) {
        items.addAll(provider.getItems(dataContext));
      }
    });
    GotoRelatedItem[] result = items.toArray(new GotoRelatedItem[items.size()]);
    Arrays.sort(result, (i1, i2) -> {
      String o1 = i1.getGroup();
      String o2 = i2.getGroup();
      return StringUtil.isEmpty(o1) ? 1 : StringUtil.isEmpty(o2) ? -1 : o1.compareTo(o2);
    });
    return Arrays.asList(result);
  }
}
