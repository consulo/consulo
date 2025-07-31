/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.language.editor.ui.DefaultPsiElementCellRenderer;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.popup.PopupListElementRenderer;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 */
@ActionImpl(id = "GotoRelated")
public class GotoRelatedFileAction extends AnAction {
    public GotoRelatedFileAction() {
        super(ActionLocalize.actionGotorelatedText(), ActionLocalize.actionGotorelatedDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        PsiFile psiFile = e.getData(PsiFile.KEY);
        if (psiFile == null) {
            return;
        }

        DataContext context = e.getDataContext();
        List<GotoRelatedItem> items = getItems(psiFile, editor, context);
        if (items.isEmpty()) {
            return;
        }
        if (items.size() == 1 && items.get(0).getElement() != null) {
            items.get(0).navigate();
            return;
        }
        if (Application.get().isUnitTestMode()) {
            //noinspection UseOfSystemOutOrSystemErr
            System.out.println(items);
        }
        createPopup(items, "Go to Related Files").showInBestPositionFor(context);
    }

    public static JBPopup createPopup(List<? extends GotoRelatedItem> items, String title) {
        Object[] elements = new Object[items.size()];
        //todo[nik] move presentation logic to GotoRelatedItem class
        Map<PsiElement, GotoRelatedItem> itemsMap = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            GotoRelatedItem item = items.get(i);
            elements[i] = item.getElement() != null ? item.getElement() : item;
            itemsMap.put(item.getElement(), item);
        }

        return getPsiElementPopup(
            elements,
            itemsMap,
            title,
            element -> {
                if (element instanceof PsiElement) {
                    //noinspection SuspiciousMethodCalls
                    itemsMap.get(element).navigate();
                }
                else {
                    ((GotoRelatedItem) element).navigate();
                }
                return true;
            }
        );
    }

    private static JBPopup getPsiElementPopup(
        Object[] elements,
        Map<PsiElement, GotoRelatedItem> itemsMap,
        String title,
        Predicate<Object> processor
    ) {
        SimpleReference<Boolean> hasMnemonic = SimpleReference.create(false);

        DefaultPsiElementCellRenderer renderer = new DefaultPsiElementCellRenderer() {
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
            @RequiredReadAction
            public String getContainerText(PsiElement element, String name) {
                String customContainerName = itemsMap.get(element).getCustomContainerName();

                if (customContainerName != null) {
                    return customContainerName;
                }
                PsiFile file = element.getContainingFile();
                return file != null && !getElementText(element).equals(file.getName())
                    ? "(" + file.getName() + ")"
                    : null;
            }

            @Override
            protected DefaultListCellRenderer getRightCellRenderer(Object value) {
                return null;
            }

            @Override
            protected boolean customizeNonPsiElementLeftRenderer(
                ColoredListCellRenderer renderer,
                JList list,
                Object value,
                int index,
                boolean selected,
                boolean hasFocus
            ) {
                GotoRelatedItem item = (GotoRelatedItem) value;
                Color color = list.getForeground();
                SimpleTextAttributes nameAttributes = new SimpleTextAttributes(Font.PLAIN, color);
                String name = item.getCustomName();
                if (name == null) {
                    return false;
                }
                renderer.append(name, nameAttributes);
                renderer.setIcon(item.getCustomIcon());
                return true;
            }

            @Override
            public Component getListCellRendererComponent(
                JList<? extends PsiElement> list,
                PsiElement value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                JPanel component = (JPanel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (!hasMnemonic.get()) {
                    return component;
                }

                JPanel panelWithMnemonic = new JPanel(new BorderLayout());
                int mnemonic = getMnemonic(value, itemsMap);
                JLabel label = new JLabel("");
                if (mnemonic != -1) {
                    label.setText(mnemonic + ".");
                    label.setDisplayedMnemonicIndex(0);
                }
                label.setPreferredSize(new JLabel("8.").getPreferredSize());

                JComponent leftRenderer = (JComponent) component.getComponents()[0];
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
        ListPopupImpl popup = new ListPopupImpl(new BaseListPopupStep<Object>(title, Arrays.asList(elements)) {
            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public String getIndexedString(Object value) {
                if (value instanceof GotoRelatedItem gotoRelatedItem) {
                    return gotoRelatedItem.getCustomName();
                }
                PsiElement element = (PsiElement) value;
                return renderer.getElementText(element) + " " + renderer.getContainerText(element, null);
            }

            @Override
            public PopupStep onChosen(Object selectedValue, boolean finalChoice) {
                processor.test(selectedValue);
                return super.onChosen(selectedValue, finalChoice);
            }
        }) {
        };
        popup.getList().setCellRenderer(new PopupListElementRenderer<PsiElement>(popup) {
            Map<Object, String> separators = new HashMap<>();

            {
                ListModel model = popup.getList().getModel();
                String current = null;
                boolean hasTitle = false;
                for (int i = 0; i < model.getSize(); i++) {
                    Object element = model.getElementAt(i);
                    GotoRelatedItem item = itemsMap.get(element);
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
            public Component getListCellRendererComponent(
                JList<? extends PsiElement> list,
                PsiElement value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                Component component = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String separator = separators.get(value);

                if (separator != null) {
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(component, BorderLayout.CENTER);
                    SeparatorWithText sep = new SeparatorWithText() {
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
            int mnemonic = getMnemonic(item, itemsMap);
            if (mnemonic != -1) {
                Action action = createNumberAction(mnemonic, popup, itemsMap, processor);
                popup.registerAction(mnemonic + "Action", KeyStroke.getKeyStroke(String.valueOf(mnemonic)), action);
                popup.registerAction(mnemonic + "Action", KeyStroke.getKeyStroke("NUMPAD" + String.valueOf(mnemonic)), action);
                hasMnemonic.set(true);
            }
        }
        return popup;
    }

    @Nonnull
    @RequiredReadAction
    public static List<GotoRelatedItem> getItems(@Nonnull PsiFile psiFile, @Nullable Editor editor, @Nullable DataContext dataContext) {
        PsiElement contextElement = psiFile;
        if (editor != null) {
            PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
            if (element != null) {
                contextElement = element;
            }
        }

        Set<GotoRelatedItem> items = new LinkedHashSet<>();

        PsiElement finalContextElement = contextElement;
        psiFile.getApplication().getExtensionPoint(GotoRelatedProvider.class).forEachExtensionSafe(provider -> {
            items.addAll(provider.getItems(finalContextElement));
            if (dataContext != null) {
                items.addAll(provider.getItems(dataContext));
            }
        });
        sortByGroupNames(items);
        return new ArrayList<>(items);
    }

    private static void sortByGroupNames(Set<GotoRelatedItem> items) {
        Map<String, List<GotoRelatedItem>> map = new HashMap<>();
        for (GotoRelatedItem item : items) {
            String key = item.getGroup();
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<>());
            }
            map.get(key).add(item);
        }
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys, (o1, o2) -> StringUtil.isEmpty(o1) ? 1 : StringUtil.isEmpty(o2) ? -1 : o1.compareTo(o2));
        items.clear();
        for (String key : keys) {
            items.addAll(map.get(key));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(PsiFile.KEY));
    }

    private static Action createNumberAction(
        int mnemonic,
        ListPopupImpl listPopup,
        Map<PsiElement, GotoRelatedItem> itemsMap,
        Predicate<Object> processor
    ) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (Object item : listPopup.getListStep().getValues()) {
                    if (getMnemonic(item, itemsMap) == mnemonic) {
                        listPopup.setFinalRunnable(() -> processor.test(item));
                        listPopup.closeOk(null);
                    }
                }
            }
        };
    }

    private static int getMnemonic(Object item, Map<PsiElement, GotoRelatedItem> itemsMap) {
        return (item instanceof GotoRelatedItem gotoRelatedItem ? gotoRelatedItem : itemsMap.get(item)).getMnemonic();
    }
}
