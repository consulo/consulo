package consulo.endpoint.impl.internal.view;

import consulo.endpoint.localize.EndpointLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.MultiSelectComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class EndpointFilterComboBox {
    private final LocalizeValue myName;
    private final Predicate<String> myVisible;
    private final BiConsumer<Set<String>, Set<String>> myApplier;
    private final MutableFlatDataModel<EndpointFilterChoice> myModel;
    private final MultiSelectComboBox<EndpointFilterChoice> myComboBox;

    private List<EndpointFilterChoice> myChoices = List.of();
    private boolean myUpdating;

    @RequiredUIAccess
    public EndpointFilterComboBox(
        LocalizeValue name,
        LocalizeValue description,
        Predicate<String> visible,
        BiConsumer<Set<String>, Set<String>> applier
    ) {
        myName = name;
        myVisible = visible;
        myApplier = applier;

        myModel = FlatDataModel.of(List.of());
        myComboBox = MultiSelectComboBox.create(myModel);
        myComboBox.setPlaceholder(name);
        myComboBox.setToolTipText(description);
        myComboBox.setRender((presentation, item) -> {
            EndpointFilterChoice choice = item.getValue();
            if (choice != null) {
                presentation.withIcon(choice.icon());
                presentation.append(choice.text());
            }
        });
        myComboBox.setSpeedSearchConverter(choice -> choice.text().get());
        myComboBox.setSummaryRenderer(this::summary);
        myComboBox.addValueListener(event -> {
            if (!myUpdating) {
                selectionChanged(event.getValue());
            }
        });
    }

    public Component getComponent() {
        return myComboBox;
    }

    @RequiredUIAccess
    public void update(List<EndpointFilterChoice> choices) {
        myUpdating = true;
        try {
            if (!isSameChoices(myChoices, choices)) {
                myChoices = List.copyOf(choices);
                myModel.replaceAll(myChoices);
                myComboBox.setPlaceholder(myChoices.isEmpty() ? myName : EndpointLocalize.endpointsFilterSelectionCount(myName, 0));
            }

            List<EndpointFilterChoice> visible = new ArrayList<>();
            for (EndpointFilterChoice choice : myChoices) {
                if (myVisible.test(choice.id())) {
                    visible.add(choice);
                }
            }

            if (!visible.equals(myComboBox.getValue())) {
                myComboBox.setValue(visible, false);
            }
        }
        finally {
            myUpdating = false;
        }
    }

    @RequiredUIAccess
    private void selectionChanged(List<EndpointFilterChoice> selected) {
        Set<String> shown = new HashSet<>();
        for (EndpointFilterChoice choice : selected) {
            shown.add(choice.id());
        }

        Set<String> hidden = new HashSet<>();
        for (EndpointFilterChoice choice : myChoices) {
            if (!shown.contains(choice.id())) {
                hidden.add(choice.id());
            }
        }

        myApplier.accept(shown, hidden);
    }

    private LocalizeValue summary(List<EndpointFilterChoice> selected) {
        if (selected.size() >= myChoices.size()) {
            return myName;
        }
        if (selected.size() == 1) {
            return EndpointLocalize.endpointsFilterSelectionSingle(myName, selected.get(0).text());
        }
        return EndpointLocalize.endpointsFilterSelectionCount(myName, selected.size());
    }

    private static boolean isSameChoices(List<EndpointFilterChoice> current, List<EndpointFilterChoice> choices) {
        if (current.size() != choices.size()) {
            return false;
        }

        for (int i = 0; i < current.size(); i++) {
            EndpointFilterChoice left = current.get(i);
            EndpointFilterChoice right = choices.get(i);
            if (!left.id().equals(right.id())
                || !left.text().get().equals(right.text().get())
                || !Objects.equals(left.icon(), right.icon())) {
                return false;
            }
        }
        return true;
    }
}
