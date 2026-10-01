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
package consulo.ui.impl.graph;

import consulo.localize.LocalizeValue;
import consulo.ui.TextItemPresentation;
import consulo.ui.graph.GraphNodePresentation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public final class GraphNodeContent<P extends TextItemPresentation> implements GraphNodePresentation {
    private final Supplier<P> myFactory;
    private final P myHeader;
    private final List<List<P>> mySections = new ArrayList<>();
    private @Nullable List<P> myCurrentSection;
    private LocalizeValue myTooltip = LocalizeValue.empty();

    public GraphNodeContent(Supplier<P> factory) {
        myFactory = factory;
        myHeader = factory.get();
    }

    @Override
    public P header() {
        return myHeader;
    }

    @Override
    public GraphNodePresentation withTooltip(LocalizeValue tooltip) {
        myTooltip = tooltip;
        return this;
    }

    @Override
    public P addRow() {
        List<P> section = myCurrentSection;
        if (section == null) {
            section = new ArrayList<>();
            mySections.add(section);
            myCurrentSection = section;
        }
        P row = myFactory.get();
        section.add(row);
        return row;
    }

    @Override
    public void addSeparator() {
        myCurrentSection = null;
    }

    public P getHeader() {
        return myHeader;
    }

    public List<List<P>> getSections() {
        return mySections;
    }

    public LocalizeValue getTooltip() {
        return myTooltip;
    }
}
