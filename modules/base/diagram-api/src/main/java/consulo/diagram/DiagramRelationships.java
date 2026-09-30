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

/**
 * @author Konstantin Bulenkov
 */
public final class DiagramRelationships {
    public static final DiagramRelationshipInfo DEPENDENCY = new DiagramRelationshipInfoAdapter("DEPENDENCY", DiagramLineType.DASHED) {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.ANGLE;
        }
    };

    public static final DiagramRelationshipInfo CREATE = new DiagramRelationshipInfoAdapter("CREATE", DiagramLineType.DASHED, "\u00abcreate\u00bb") {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.ANGLE;
        }
    };

    public static final DiagramRelationshipInfo TO_ONE = new DiagramRelationshipInfoAdapter("TO_ONE", DiagramLineType.SOLID, "1:1") {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.DIAMOND;
        }

        @Override
        public DiagramArrow getEndArrow() {
            return DiagramArrow.ANGLE;
        }
    };

    public static final DiagramRelationshipInfo TO_MANY = new DiagramRelationshipInfoAdapter("TO_MANY", DiagramLineType.SOLID, "1:*") {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.DIAMOND;
        }

        @Override
        public DiagramArrow getEndArrow() {
            return DiagramArrow.ANGLE;
        }
    };

    public static final DiagramRelationshipInfo GENERALIZATION = new DiagramRelationshipInfoAdapter("GENERALIZATION") {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.DELTA;
        }
    };

    public static final DiagramRelationshipInfo INTERFACE_GENERALIZATION = new DiagramRelationshipInfoAdapter("INTERFACE_GENERALIZATION") {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.DELTA;
        }
    };

    public static final DiagramRelationshipInfo REALIZATION = new DiagramRelationshipInfoAdapter("REALIZATION", DiagramLineType.DASHED) {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.DELTA;
        }
    };

    public static final DiagramRelationshipInfo ANNOTATION = new DiagramRelationshipInfoAdapter("ANNOTATION", DiagramLineType.DOTTED) {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.NONE;
        }
    };

    public static final DiagramRelationshipInfo INNER_CLASS = new DiagramRelationshipInfoAdapter("INNER_CLASS") {
        @Override
        public DiagramArrow getStartArrow() {
            return DiagramArrow.INNER_CLASS;
        }
    };

    private DiagramRelationships() {
    }
}
