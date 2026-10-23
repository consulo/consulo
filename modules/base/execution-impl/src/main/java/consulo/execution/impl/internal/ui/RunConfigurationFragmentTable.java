package consulo.execution.impl.internal.ui;

import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Places fragment rows: labelled and unlabelled rows in one table (labels aligned), full width components after it.
 */
final class RunConfigurationFragmentTable implements RunConfigurationFragmentLayout {
    private final TableLayout myTable = TableLayout.create(StaticPosition.TOP);
    private final List<Component> myBottom = new ArrayList<>();
    private int myRow;

    @RequiredUIAccess
    @Override
    public void addLabeled(LocalizeValue label, Component component) {
        Label labelComponent = Label.create(label);
        labelComponent.paddingBuilder().rightSet(Space.MEDIUM).apply();
        myTable.add(labelComponent, TableLayout.cell(myRow, 0));
        myTable.add(component, TableLayout.cell(myRow, 1).fill());
        myRow++;
    }

    @RequiredUIAccess
    @Override
    public void addUnlabeled(Component component) {
        myTable.add(component, TableLayout.cell(myRow, 1).fill());
        myRow++;
    }

    @RequiredUIAccess
    @Override
    public void addBottom(Component component) {
        myBottom.add(component);
    }

    @RequiredUIAccess
    void addTo(VerticalLayout layout, List<Component> afterTable) {
        if (myRow > 0) {
            layout.add(myTable);
        }
        afterTable.forEach(layout::add);
        myBottom.forEach(layout::add);
    }
}
