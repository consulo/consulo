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
package consulo.it.ui.tree;

import consulo.application.Application;
import consulo.it.TreeTester;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.CheckBoxTreeTable;
import consulo.ui.TreeExecutor;
import consulo.util.lang.ThreeState;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public abstract class CheckBoxTreeTableContractTestCase extends CheckBoxTreeContractTestCase {
    private static final LocalizeValue LENGTH = LocalizeValue.of("Length");

    @Override
    protected TreeTester<String> createTester(MapTreeModel model, TreeExecutor executor) {
        return TreeTester.createCheckBoxTreeTable(ROOT, model, executor);
    }

    private TreeTester<String> showWithLength(Application application, CheckBoxTreePolicy policy) {
        TreeTester<String> tester = create(application, project(), policy);
        tester.getTreeTable().addColumn(LENGTH, String::length).setSortable(Comparator.naturalOrder());
        tester.show();
        tester.getTree().expandAll();
        tester.settle();
        getEvents().clear();
        return tester;
    }

    @Test
    public void theFactoryAnswersATreeTableWithCheckBoxes(Application application) {
        TreeTester<String> tester = create(application, project(), CheckBoxTreePolicy.CASCADE);

        assertThat(tester.getTree()).isInstanceOf(CheckBoxTreeTable.class);
        assertThat(tester.getCheckableTree()).isSameAs(tester.getTreeTable());
    }

    @Test
    public void theCheckBoxesSitInTheTreeColumnInFrontOfTheColumns(Application application) {
        TreeTester<String> tester = showWithLength(application, CheckBoxTreePolicy.CASCADE);

        tester.assertStructure("""
            -( ) src | 3
             -( ) main | 4
              ( ) App | 3
              ( ) Lib | 3
             ( ) test | 4
            -( ) docs | 4
             ( ) guide | 5
             ( ) api | 3
            """);
        assertThat(tester.headers()).containsExactly("", "Length");

        assertThat(tester.userClickCheckBox("src", "main")).isTrue();

        tester.assertStructure("""
            -(-) src | 3
             -(v) main | 4
              (v) App | 3
              (v) Lib | 3
             ( ) test | 4
            -( ) docs | 4
             ( ) guide | 5
             ( ) api | 3
            """);
        assertThat(getEvents()).containsExactly("main=true", "App=true", "Lib=true");
    }

    @Test
    public void sortingByAColumnKeepsTheCheckStateAndTheCheckedValuesFollowTheNewOrder(Application application) {
        TreeTester<String> tester = showWithLength(application, CheckBoxTreePolicy.CASCADE);
        tester.userClickCheckBox("docs", "guide");
        tester.userClickCheckBox("docs", "api");
        getEvents().clear();

        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("guide", "api");

        tester.sortBy(0, true);

        tester.assertStructure("""
            -( ) src | 3
             -( ) main | 4
              ( ) App | 3
              ( ) Lib | 3
             ( ) test | 4
            -(-) docs | 4
             (v) api | 3
             (v) guide | 5
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("api", "guide");
        assertThat(tester.getCheckableTree().getCheckState("docs")).isEqualTo(ThreeState.UNSURE);
        assertThat(tester.getCheckableTree().isChecked("docs")).isFalse();
        assertThat(getEvents()).isEmpty();
    }

    @Test
    public void aColumnAddedOnceTheTreeTableIsShownKeepsTheChecks(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);
        tester.userClickCheckBox("src", "main", "App");
        tester.userClickCheckBox("docs");
        getEvents().clear();

        tester.getTreeTable().addColumn(LENGTH, String::length);

        tester.assertStructure("""
            -( ) src | 3
             -( ) main | 4
              (v) App | 3
              ( ) Lib | 3
             ( ) test | 4
            -(v) docs | 4
             ( ) guide | 5
             ( ) api | 3
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("App", "docs");
        assertThat(getEvents()).isEmpty();
    }

    @Test
    public void aProgrammaticChangeIsShownNextToTheColumns(Application application) {
        TreeTester<String> tester = showWithLength(application, CheckBoxTreePolicy.FULL_SYNC);

        setChecked(tester, "guide", true);
        setChecked(tester, "api", true);

        tester.assertStructure("""
            -( ) src | 3
             -( ) main | 4
              ( ) App | 3
              ( ) Lib | 3
             ( ) test | 4
            -(v) docs | 4
             (v) guide | 5
             (v) api | 3
            """);
        assertThat(getEvents()).containsExactly("guide=true", "docs=true", "api=true");
    }
}
