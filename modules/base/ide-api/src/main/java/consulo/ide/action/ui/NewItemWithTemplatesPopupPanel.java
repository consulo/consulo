// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.ui.ListBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewItemWithTemplatesPopupPanel<T> extends NewItemSimplePopupPanel {

    protected final ListBox<T> myTemplatesList;

    private final MutableFlatDataModel<T> myTemplatesModel;
    private final ScrollableLayout myTemplatesListHolder;

    private final Collection<TemplatesListVisibilityListener> myVisibilityListeners = new ArrayList<>();

    @RequiredUIAccess
    public NewItemWithTemplatesPopupPanel(List<T> templatesList, TextItemRender<T> render) {
        myTemplatesModel = FlatDataModel.of(templatesList);

        myTemplatesList = ListBox.create(myTemplatesModel);
        myTemplatesList.setRender(render);
        myTemplatesList.addDoubleClickListener(event -> {
            if (myApplyAction != null) {
                myApplyAction.accept(null);
            }
        });

        myTextField.addKeyPressedListener(event -> {
            KeyCode keyCode = event.getInputDetails().getKeyCode();

            if (KeyCode.DOWN.equals(keyCode)) {
                myTemplatesList.moveSelection(1);
            }
            else if (KeyCode.UP.equals(keyCode)) {
                myTemplatesList.moveSelection(-1);
            }
        });

        myTemplatesListHolder = ScrollableLayout.create(myTemplatesList);

        myRootLayout.center(myTemplatesListHolder);
    }

    public void addTemplatesVisibilityListener(TemplatesListVisibilityListener listener) {
        myVisibilityListeners.add(listener);
    }

    public void removeTemplatesVisibilityListener(TemplatesListVisibilityListener listener) {
        myVisibilityListeners.remove(listener);
    }

    @RequiredUIAccess
    protected void setTemplatesListVisible(boolean visible) {
        if (myTemplatesListHolder.isVisible() != visible) {
            myTemplatesListHolder.setVisible(visible);
            myVisibilityListeners.forEach(l -> l.visibilityChanged(visible));
        }
    }

    protected void updateTemplatesList(List<T> templatesList) {
        myTemplatesModel.replaceAll(templatesList);
    }
}
