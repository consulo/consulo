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
package consulo.ui.internal;

import consulo.ui.color.ColorValue;

/**
 * A soft radial spot of a {@link GradientColor}: {@link #getColor()} at the centre, fading along {@link #falloff(float)}
 * to transparent at the radius.
 * Centre and radius are fractions of the component size, the radius of its larger side.
 *
 * @author VISTALL
 * @since 2026-11-07
 */
public final class GradientColorBlob {
    /**
     * Offsets along the radius at which a frontend places its colour stops.
     */
    public static final float[] FALLOFF_OFFSETS = {0f, 0.15f, 0.3f, 0.45f, 0.6f, 0.75f, 0.9f, 1f};

    /**
     * How much of the colour's alpha is left at {@code offset} along the radius - a Gaussian curve, so the spot
     * looks blurred rather than cut, reaching nothing at the radius.
     */
    public static float falloff(float offset) {
        if (offset >= 1f) {
            return 0f;
        }
        double gauss = Math.exp(-4.5 * offset * offset);
        double edge = Math.exp(-4.5);
        return (float) ((gauss - edge) / (1 - edge));
    }

    private final float myCenterX;
    private final float myCenterY;
    private final float myRadius;
    private final ColorValue myColor;

    public GradientColorBlob(float centerX, float centerY, float radius, ColorValue color) {
        myCenterX = centerX;
        myCenterY = centerY;
        myRadius = radius;
        myColor = color;
    }

    public float getCenterX() {
        return myCenterX;
    }

    public float getCenterY() {
        return myCenterY;
    }

    public float getRadius() {
        return myRadius;
    }

    public ColorValue getColor() {
        return myColor;
    }
}
