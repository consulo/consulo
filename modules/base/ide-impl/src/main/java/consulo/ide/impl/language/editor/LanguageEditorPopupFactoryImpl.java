/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.language.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.impl.idea.ide.PsiCopyPasteManagerImpl;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.impl.idea.ui.popup.list.PopupListElementRenderer;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.ui.DefaultPsiElementCellRenderer;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.editor.ui.PsiElementListNavigator;
import consulo.language.editor.ui.internal.LanguageEditorPopupFactory;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.usage.UsageView;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 01-Apr-22
 */
@Singleton
@ServiceImpl
public class LanguageEditorPopupFactoryImpl implements LanguageEditorPopupFactory {
  private static class NavigateOrPopupBuilderImpl extends PsiElementListNavigator.NavigateOrPopupBuilder {
    public NavigateOrPopupBuilderImpl(@Nonnull NavigatablePsiElement[] targets, String title) {
      super(targets, title);
    }

    @Override
    @Nullable
    public final JBPopup build() {
      if (myTargets.length == 0) {
        if (!allowEmptyTargets()) return null; // empty initial targets are not allowed
        if (myListUpdaterTask == null || myListUpdaterTask.isFinished()) return null; // there will be no targets.
      }
      if (myTargets.length == 1 && (myListUpdaterTask == null || myListUpdaterTask.isFinished())) {
        myTargetsConsumer.accept(myTargets);
        return null;
      }
      List<NavigatablePsiElement> initialTargetsList = Arrays.asList(myTargets);
      Ref<NavigatablePsiElement[]> updatedTargetsList = Ref.create(myTargets);

      final IPopupChooserBuilder<NavigatablePsiElement> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(initialTargetsList);
      afterPopupBuilderCreated(builder);
      if (myListRenderer instanceof PsiElementListCellRenderer) {
        ((PsiElementListCellRenderer)myListRenderer).installSpeedSearch(builder);
      }

      IPopupChooserBuilder<NavigatablePsiElement> popupChooserBuilder = builder.
              setTitle(myTitle).
              setMovable(true).
              setFont(EditorUtil.getEditorFont()).
              setRenderer(myListRenderer).
              withHintUpdateSupply().
              setResizable(true).
              setItemsChosenCallback(selectedValues -> myTargetsConsumer.accept(ArrayUtil.toObjectArray(selectedValues))).
              setCancelCallback(() -> {
                if (myListUpdaterTask != null) {
                  myListUpdaterTask.cancelTask();
                }
                return true;
              });
      final Ref<UsageView> usageView = new Ref<>();
      if (myFindUsagesTitle != null) {
        popupChooserBuilder = popupChooserBuilder.setCouldPin(popup -> {
          usageView.set(FindUtil.showInUsageView(null, updatedTargetsList.get(), myFindUsagesTitle, getProject()));
          popup.cancel();
          return false;
        });
      }

      final JBPopup popup = popupChooserBuilder.createPopup();
      if (builder instanceof PopupChooserBuilder) {
        JBList<NavigatablePsiElement> list = (JBList)((PopupChooserBuilder)builder).getChooserComponent();
        list.setTransferHandler(new TransferHandler() {
          @Override
          protected Transferable createTransferable(JComponent c) {
            final Object[] selectedValues = list.getSelectedValues();
            final PsiElement[] copy = new PsiElement[selectedValues.length];
            for (int i = 0; i < selectedValues.length; i++) {
              copy[i] = (PsiElement)selectedValues[i];
            }
            return new PsiCopyPasteManagerImpl.MyTransferable(copy);
          }

          @Override
          public int getSourceActions(JComponent c) {
            return COPY;
          }
        });

        JScrollPane pane = ((PopupChooserBuilder)builder).getScrollPane();
        pane.setBorder(null);
        pane.setViewportBorder(null);
      }

      if (myListUpdaterTask != null) {
        ListComponentUpdater popupUpdater = builder.getBackgroundUpdater();
        myListUpdaterTask.init(popup, new ListComponentUpdater<>() {
          @Override
          public void replaceModel(@Nonnull List<? extends PsiElement> data) {
            updatedTargetsList.set(data.toArray(NavigatablePsiElement.EMPTY_ARRAY));
            popupUpdater.replaceModel(data);
          }

          @Override
          public void paintBusy(boolean paintBusy) {
            popupUpdater.paintBusy(paintBusy);
          }
        }, usageView);
      }
      return popup;
    }
  }

  @Nonnull
  @Override
  public PsiElementListNavigator.NavigateOrPopupBuilder builder(@Nonnull NavigatablePsiElement[] targets, String title) {
    return new NavigateOrPopupBuilderImpl(targets, title);
  }

  @Override
  @Nonnull
  public JBPopup getPsiElementPopup(final Object[] elements,
                                            final Map<PsiElement, GotoRelatedItem> itemsMap,
                                            final String title,
                                            final boolean showContainingModules,
                                            final Predicate<Object> processor) {

    final Ref<Boolean> hasMnemonic = Ref.create(false);
    final DefaultPsiElementCellRenderer renderer = new DefaultPsiElementCellRenderer() {
      @Override
      public String getElementText(PsiElement element) {
        String customName = itemsMap.get(element).getCustomName();
        return (customName != null ? customName : super.getElementText(element));
      }

      @Override
      protected Image getIcon(PsiElement element) {
        Image customIcon = itemsMap.get(element).getCustomIcon();
        return customIcon != null ? customIcon : super.getIcon(element);
      }

      @Override
      public String getContainerText(PsiElement element, String name) {
        String customContainerName = itemsMap.get(element).getCustomContainerName();

        if (customContainerName != null) {
          return customContainerName;
        }
        PsiFile file = element.getContainingFile();
        return file != null && !getElementText(element).equals(file.getName()) ? "(" + file.getName() + ")" : null;
      }

      @Override
      protected DefaultListCellRenderer getRightCellRenderer(Object value) {
        return showContainingModules ? super.getRightCellRenderer(value) : null;
      }

      @Override
      protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer, JList list, Object value, int index, boolean selected, boolean hasFocus) {
        final GotoRelatedItem item = (GotoRelatedItem)value;
        Color color = list.getForeground();
        final SimpleTextAttributes nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
        final String name = item.getCustomName();
        if (name == null) return false;
        renderer.append(name, nameAttributes);
        renderer.setIcon(item.getCustomIcon());
        return true;
      }

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final JPanel component = (JPanel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (!hasMnemonic.get()) return component;

        final JPanel panelWithMnemonic = new JPanel(new BorderLayout());
        final int mnemonic = getMnemonic(value, itemsMap);
        final JLabel label = new JLabel("");
        if (mnemonic != -1) {
          label.setText(mnemonic + ".");
          label.setDisplayedMnemonicIndex(0);
        }
        label.setPreferredSize(new JLabel("8.").getPreferredSize());

        final JComponent leftRenderer = (JComponent)component.getComponents()[0];
        component.remove(leftRenderer);
        panelWithMnemonic.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0));
        panelWithMnemonic.setBackground(leftRenderer.getBackground());
        label.setBackground(leftRenderer.getBackground());
        panelWithMnemonic.add(label, BorderLayout.WEST);
        panelWithMnemonic.add(leftRenderer, BorderLayout.CENTER);
        component.add(panelWithMnemonic);
        return component;
      }
    };
    final ListPopupImpl popup = new ListPopupImpl(new BaseListPopupStep<Object>(title, Arrays.asList(elements)) {
      @Override
      public boolean isSpeedSearchEnabled() {
        return true;
      }

      @Override
      public String getIndexedString(Object value) {
        if (value instanceof GotoRelatedItem) {
          //noinspection ConstantConditions
          return ((GotoRelatedItem)value).getCustomName();
        }
        PsiElement element = (PsiElement)value;
        if (!element.isValid()) return "INVALID";
        return renderer.getElementText(element) + " " + renderer.getContainerText(element, null);
      }

      @Override
      public PopupStep onChosen(Object selectedValue, boolean finalChoice) {
        processor.test(selectedValue);
        return super.onChosen(selectedValue, finalChoice);
      }
    }) {
    };
    popup.getList().setCellRenderer(new PopupListElementRenderer(popup) {
      Map<Object, String> separators = new HashMap<>();

      {
        final ListModel model = popup.getList().getModel();
        String current = null;
        boolean hasTitle = false;
        for (int i = 0; i < model.getSize(); i++) {
          final Object element = model.getElementAt(i);
          final GotoRelatedItem item = itemsMap.get(element);
          if (item != null && !StringUtil.equals(current, item.getGroup())) {
            current = item.getGroup();
            separators.put(element, current);
            if (!hasTitle && !StringUtil.isEmpty(current)) {
              hasTitle = true;
            }
          }
        }

        if (!hasTitle) {
          separators.remove(model.getElementAt(0));
        }
      }

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        final Component component = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        final String separator = separators.get(value);

        if (separator != null) {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add(component, BorderLayout.CENTER);
          final SeparatorWithText sep = new SeparatorWithText() {
            @Override
            protected void paintComponent(Graphics g) {
              g.setColor(new JBColor(Color.WHITE, UIUtil.getSeparatorColor()));
              g.fillRect(0, 0, getWidth(), getHeight());
              super.paintComponent(g);
            }
          };
          sep.setCaption(separator);
          panel.add(sep, BorderLayout.NORTH);
          return panel;
        }
        return component;
      }
    });

    popup.setMinimumSize(new Dimension(200, -1));

    for (Object item : elements) {
      final int mnemonic = getMnemonic(item, itemsMap);
      if (mnemonic != -1) {
        final Action action = createNumberAction(mnemonic, popup, itemsMap, processor);
        popup.registerAction(mnemonic + "Action", KeyStroke.getKeyStroke(String.valueOf(mnemonic)), action);
        popup.registerAction(mnemonic + "Action", KeyStroke.getKeyStroke("NUMPAD" + String.valueOf(mnemonic)), action);
        hasMnemonic.set(true);
      }
    }
    return popup;
  }

  private static int getMnemonic(Object item, Map<PsiElement, GotoRelatedItem> itemsMap) {
    return (item instanceof GotoRelatedItem ? (GotoRelatedItem)item : itemsMap.get((PsiElement)item)).getMnemonic();
  }

  private static Action createNumberAction(final int mnemonic, final ListPopupImpl listPopup, final Map<PsiElement, GotoRelatedItem> itemsMap, final Predicate<Object> processor) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (final Object item : listPopup.getListStep().getValues()) {
          if (getMnemonic(item, itemsMap) == mnemonic) {
            listPopup.setFinalRunnable(() -> processor.test(item));
            listPopup.closeOk(null);
          }
        }
      }
    };
  }
}
