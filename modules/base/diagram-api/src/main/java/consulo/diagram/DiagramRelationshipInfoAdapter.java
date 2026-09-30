/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram;

import consulo.diagram.presentation.DiagramArrow;
import consulo.diagram.presentation.DiagramLineType;
import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class DiagramRelationshipInfoAdapter implements DiagramRelationshipInfo {
    private final String myName;
    private final DiagramLineType myLineType;
    private final String myLabel;

    public DiagramRelationshipInfoAdapter(@Nullable String name, @Nullable DiagramLineType lineType, @Nullable String label) {
        myName = name == null ? "UNDEFINED" : name;
        myLineType = lineType == null ? DiagramLineType.SOLID : lineType;
        myLabel = label == null ? "" : label;
    }

    public DiagramRelationshipInfoAdapter(@Nullable String name, @Nullable DiagramLineType lineType) {
        this(name, lineType, null);
    }

    public DiagramRelationshipInfoAdapter(@Nullable String name) {
        this(name, null, null);
    }

    @Override
    public DiagramLineType getLineType() {
        return myLineType;
    }

    @Override
    public String getLabel() {
        return myLabel;
    }

    @Override
    public abstract @Nullable DiagramArrow getStartArrow();

    @Override
    public @Nullable DiagramArrow getEndArrow() {
        return null;
    }

    @Override
    public String toString() {
        return myName;
    }
}
