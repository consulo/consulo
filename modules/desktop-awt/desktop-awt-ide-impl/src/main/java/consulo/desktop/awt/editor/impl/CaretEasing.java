// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.EditorSettings;

class CaretEasing {
    private enum Type {
        Ninja,
        Ease
    }

    private final Type myType;

    private CaretEasing(Type type) {
        myType = type;
    }

    double apply(double t) {
        return switch (myType) {
            case Ninja -> {
                double u = Math.cbrt(t);
                yield 3 * u - 3 * Math.pow(u, 2) + t;
            }
            // Horner form of rounded Hermite + α, β approx of cubic-bezier(0.25,0.1,0.25,1.0); monotone on [0,1], max dev ≈ 0.0176.
            case Ease -> t * ((((-5.4 * t + 17.6) * t - 20.6) * t + 9.0) * t + 0.4);
        };
    }

    static CaretEasing fromSettings(EditorSettings settings) {
        Type type = switch (settings.getCaretEasing()) {
            case NINJA -> Type.Ninja;
            case EASE -> Type.Ease;
        };
        return new CaretEasing(type);
    }
}
