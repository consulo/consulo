/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "animatedImages", order = "after delayedAction")
public class AnimatedImagesUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Animated Images");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout rows = VerticalLayout.create();

        rows.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("ImageBox, busy()"), ImageBox.create(Image.busy())));
        rows.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("ImageBox, busy(32)"), ImageBox.create(Image.busy(32))));
        rows.add(LabeledBuilder.sided(
            LocalizeValue.localizeTODO("ImageBox, resize(busy(), 32, 16)"),
            ImageBox.create(ImageEffects.resize(Image.busy(), 32, 16))
        ));

        Label busyLabel = Label.create(LocalizeValue.localizeTODO("Indexing..."));
        busyLabel.setImage(Image.busy());
        rows.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Label, busy() with text"), busyLabel));

        Image blinking = ImageEffects.blinking(PlatformIconGroup.ideFatalerror());
        rows.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("ImageBox, blinking(ideFatalerror)"), ImageBox.create(blinking)));

        Label blinkingLabel = Label.create(LocalizeValue.localizeTODO("Fatal errors"));
        blinkingLabel.setImage(blinking);
        rows.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Label, blinking(ideFatalerror) with text"), blinkingLabel));

        Image busyWithErrorMark = ImageEffects.layered(Image.busy(), PlatformIconGroup.nodesErrormark());
        rows.add(LabeledBuilder.sided(
            LocalizeValue.localizeTODO("ImageBox, layered(busy(), nodesErrormark)"),
            ImageBox.create(busyWithErrorMark)
        ));
        rows.add(LabeledBuilder.sided(
            LocalizeValue.localizeTODO("ImageBox, grayed(layered(busy(), nodesErrormark))"),
            ImageBox.create(ImageEffects.grayed(busyWithErrorMark))
        ));

        Button busyButton = Button.create(LocalizeValue.localizeTODO("Running"));
        busyButton.setIcon(Image.busy());
        rows.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Button, busy() icon"), busyButton));

        Button disabledButton = Button.create(LocalizeValue.localizeTODO("Running"));
        disabledButton.setIcon(busyWithErrorMark);
        disabledButton.setEnabled(false);
        rows.add(LabeledBuilder.sided(
            LocalizeValue.localizeTODO("Disabled Button, layered(busy(), nodesErrormark) icon"),
            disabledButton
        ));

        Label swappedLabel = Label.create(LocalizeValue.localizeTODO("Working"));
        swappedLabel.setImage(Image.busy());

        CheckBox busyCheckBox = CheckBox.create(LocalizeValue.localizeTODO("Busy"), true);
        busyCheckBox.addValueListener(event -> {
            boolean busy = Boolean.TRUE.equals(event.getValue());
            swappedLabel.setImage(busy ? Image.busy() : PlatformIconGroup.actionsCommit());
            swappedLabel.setText(busy ? LocalizeValue.localizeTODO("Working") : LocalizeValue.localizeTODO("Done"));
        });
        rows.add(DockLayout.create().left(busyCheckBox).right(swappedLabel));

        Tree<String> tree = Tree.create(
            (TreeModel<String>) (nodeFactory, parentValue) -> {
                if (parentValue != null) {
                    return;
                }

                for (int i = 0; i < 30; i++) {
                    TreeNode<String> node = nodeFactory.apply("Running test " + i);
                    node.setLeaf(true);

                    Image icon = i % 5 == 4 ? busyWithErrorMark : Image.busy();
                    node.setRenderer((value, presentation) -> {
                        presentation.append(value);
                        presentation.withIcon(icon);
                    });
                }
            }
        );
        Disposer.register(uiDisposable, tree.destroyHook());

        List<String> listItems = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            listItems.add("Running task " + i);
        }

        ListBox<String> listBox = ListBox.create(listItems);
        listBox.setRender((presentation, item) -> {
            presentation.append(item.getValue());
            presentation.withIcon(Image.busy());
        });

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(ScrollableLayout.create(tree));
        splitLayout.setSecondComponent(ScrollableLayout.create(listBox));

        return DockLayout.create().top(rows).center(splitLayout);
    }
}
