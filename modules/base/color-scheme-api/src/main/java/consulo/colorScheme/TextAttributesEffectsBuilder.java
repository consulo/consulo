// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.colorScheme;

import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import org.jetbrains.annotations.Contract;

import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

import static consulo.colorScheme.EffectType.*;
import static consulo.colorScheme.TextAttributesEffectsBuilder.EffectSlot.*;

/**
 * Allows to build effects for the TextAttributes. Allows to cover effects on the current state and slip effects under it.
 */
public class TextAttributesEffectsBuilder {
    private static final Logger LOG = Logger.getInstance(TextAttributesEffectsBuilder.class);

    public enum EffectSlot {
        FRAME_SLOT,
        UNDERLINE_SLOT,
        STRIKE_SLOT
    }

    // this probably could be a property of the EffectType
    private static final Map<EffectType, EffectSlot> EFFECT_SLOTS_MAP = Map.of(
        STRIKEOUT, STRIKE_SLOT,
        BOXED, FRAME_SLOT,
        ROUNDED_BOX, FRAME_SLOT,
        BOLD_LINE_UNDERSCORE, UNDERLINE_SLOT,
        LINE_UNDERSCORE, UNDERLINE_SLOT,
        WAVE_UNDERSCORE, UNDERLINE_SLOT,
        BOLD_DOTTED_LINE, UNDERLINE_SLOT
    );

    private final Map<EffectSlot, EffectDescriptor> myEffectsMap = new HashMap<>(EffectSlot.values().length);

    private TextAttributesEffectsBuilder() {
    }

    /**
     * Creates a builder without any effects
     */
    public static TextAttributesEffectsBuilder create() {
        return new TextAttributesEffectsBuilder();
    }

    /**
     * Creates a builder with effects from {@code deepestAttributes}
     */
    public static TextAttributesEffectsBuilder create(TextAttributes deepestAttributes) {
        return create().coverWith(deepestAttributes);
    }

    /**
     * Applies effects from {@code attributes} above current state of the merger. Effects may override mutually exclusive ones. E.g
     * If current state has underline and we applying attributes with wave underline, underline effect will be removed.
     */
    public final TextAttributesEffectsBuilder coverWith(TextAttributes attributes) {
        attributes.forEachAdditionalEffect(this::coverWith);
        coverWith(attributes.getEffectType(), attributes.getEffectColor());
        return this;
    }

    /**
     * Applies effects from {@code attributes} if effect slots are not used.
     */
    public final TextAttributesEffectsBuilder slipUnder(TextAttributes attributes) {
        slipUnder(attributes.getEffectType(), attributes.getEffectColor());
        attributes.forEachAdditionalEffect(this::slipUnder);
        return this;
    }

    /**
     * Applies effect with {@code effectType} and {@code effectColor} to the current state. Effects may override mutually exclusive ones. E.g
     * If current state has underline and we applying attributes with wave underline, underline effect will be removed.
     */
    public TextAttributesEffectsBuilder coverWith(@Nullable EffectType effectType, @Nullable ColorValue effectColor) {
        return mutateBuilder(effectType, effectColor, myEffectsMap::put);
    }

    /**
     * Applies effect with {@code effectType} and {@code effectColor} to the current state if effect slot is not used.
     */
    public TextAttributesEffectsBuilder slipUnder(@Nullable EffectType effectType, @Nullable ColorValue effectColor) {
        return mutateBuilder(effectType, effectColor, myEffectsMap::putIfAbsent);
    }

    
    private TextAttributesEffectsBuilder mutateBuilder(
        @Nullable EffectType effectType,
        @Nullable ColorValue effectColor,
        BiConsumer<? super EffectSlot, ? super EffectDescriptor> slotMutator
    ) {
        if (effectColor != null && effectType != null) {
            EffectSlot slot = EFFECT_SLOTS_MAP.get(effectType);
            if (slot != null) {
                slotMutator.accept(slot, EffectDescriptor.create(effectType, effectColor));
            }
            else {
                LOG.debug("Effect " + effectType + " is not supported by builder");
            }
        }
        return this;
    }

    /**
     * @return map of {@link EffectType} => {@link Color} representation of builder state
     */
    Map<EffectType, ColorValue> getEffectsMap() {
        if (myEffectsMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<EffectType, ColorValue> result = new HashMap<>();
        myEffectsMap.forEach((key, val) -> {
            if (val != null) {
                result.put(val.effectType, val.effectColor);
            }
        });
        return result;
    }

    /**
     * Applies effects from the current state to the target attributes
     *
     * @param targetAttributes passed targetAttributes
     * @apiNote this method is not a thread safe, builder can't be modified in some other thread when applying to something
     */
    public TextAttributes applyTo(TextAttributes targetAttributes) {
        Iterator<EffectDescriptor> effectsIterator = myEffectsMap.values().iterator();
        if (!effectsIterator.hasNext()) {
            targetAttributes.setEffectColor(null);
            targetAttributes.setEffectType(BOXED);
            targetAttributes.setAdditionalEffects(Collections.emptyMap());
        }
        else {
            EffectDescriptor mainEffectDescriptor = effectsIterator.next();
            targetAttributes.setEffectType(mainEffectDescriptor.effectType);
            targetAttributes.setEffectColor(mainEffectDescriptor.effectColor);

            int effectsLeft = myEffectsMap.size() - 1;
            if (effectsLeft == 0) {
                targetAttributes.setAdditionalEffects(Collections.emptyMap());
            }
            else if (effectsLeft == 1) {
                EffectDescriptor additionalEffect = effectsIterator.next();
                targetAttributes.setAdditionalEffects(Collections.singletonMap(additionalEffect.effectType, additionalEffect.effectColor));
            }
            else {
                Map<EffectType, ColorValue> effectsMap = new HashMap<>(effectsLeft);
                effectsIterator.forEachRemaining(it -> effectsMap.put(it.effectType, it.effectColor));
                targetAttributes.setAdditionalEffects(effectsMap);
            }
        }
        return targetAttributes;
    }

    @Contract("null -> null")
    public @Nullable EffectDescriptor getEffectDescriptor(@Nullable EffectSlot effectSlot) {
        return myEffectsMap.get(effectSlot);
    }

    public static class EffectDescriptor {
        
        public final EffectType effectType;
        
        public final ColorValue effectColor;

        private EffectDescriptor(EffectType effectType, ColorValue effectColor) {
            this.effectType = effectType;
            this.effectColor = effectColor;
        }

        
        static EffectDescriptor create(EffectType effectType, ColorValue effectColor) {
            return new EffectDescriptor(effectType, effectColor);
        }
    }
}
