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
package consulo.ui;

/**
 * How checking one value of a {@link CheckableTree} spreads to the values around it. A change spreads over the
 * nodes the tree has built: down to every node below the changed one, or up to every node above it. Nodes without
 * a check box are passed through - the change goes on to the nodes behind them.
 * <p/>
 * A policy which links values in any direction shows a parent whose children differ, or differ from it, as
 * partially checked.
 *
 * @param checkChildrenWithCheckedParent     checking a node checks every node below it, those built later included
 * @param uncheckChildrenWithUncheckedParent unchecking a node unchecks every node below it
 * @param checkParentWithCheckedChild        checking a node checks every node above it
 * @param uncheckParentWithUncheckedChild    unchecking a node unchecks every node above it
 * @author VISTALL
 * @since 2026-10-10
 */
public record CheckBoxTreePolicy(
    boolean checkChildrenWithCheckedParent,
    boolean uncheckChildrenWithUncheckedParent,
    boolean checkParentWithCheckedChild,
    boolean uncheckParentWithUncheckedChild
) {
    /**
     * Every check box stands for its own value alone.
     */
    public static final CheckBoxTreePolicy INDEPENDENT = new CheckBoxTreePolicy(false, false, false, false);

    /**
     * A parent decides for the nodes below it, while the nodes below leave it alone.
     */
    public static final CheckBoxTreePolicy CASCADE = new CheckBoxTreePolicy(true, true, false, false);

    /**
     * A change spreads both down and up.
     */
    public static final CheckBoxTreePolicy FULL_SYNC = new CheckBoxTreePolicy(true, true, true, true);

    /**
     * Whether no change spreads at all, which is also when no parent is shown partially checked.
     */
    public boolean isIndependent() {
        return !checkChildrenWithCheckedParent
            && !uncheckChildrenWithUncheckedParent
            && !checkParentWithCheckedChild
            && !uncheckParentWithUncheckedChild;
    }
}
