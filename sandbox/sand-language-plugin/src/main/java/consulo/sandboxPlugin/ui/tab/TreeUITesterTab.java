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
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.Image;
import consulo.ui.layout.ScrollableLayout;

import java.util.List;
import java.util.Random;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "tree", order = "after table")
public class TreeUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Tree");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Tree<String> tree = Tree.create(
            (TreeModel<String>) (nodeFactory, parentValue) -> {
                if (parentValue == null) {
                    for (int i = 0; i < 50; i++) {
                        TreeNode<String> node = nodeFactory.apply("First Child = " + i);

                        List<Image> icons = List.of(
                            PlatformIconGroup.nodesClass(),
                            PlatformIconGroup.nodesEnum(),
                            PlatformIconGroup.nodesStruct(),
                            PlatformIconGroup.nodesInterface(),
                            PlatformIconGroup.nodesAttribute()
                        );
                        int r = new Random().nextInt(icons.size());

                        node.setRenderer((s, textItemPresentation) -> {
                            textItemPresentation.append(s);
                            textItemPresentation.withIcon(icons.get(r));
                        });
                    }
                }
                else {
                    for (int i = 0; i < 10; i++) {
                        nodeFactory.apply(parentValue + ", second child = " + i);
                    }
                }
            }
        );
        Disposer.register(uiDisposable, tree.destroyHook());

        return ScrollableLayout.create(tree);
    }
}
