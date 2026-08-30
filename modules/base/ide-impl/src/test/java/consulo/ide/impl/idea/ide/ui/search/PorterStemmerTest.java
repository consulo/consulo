/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package consulo.ide.impl.idea.ide.ui.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@SuppressWarnings("SpellCheckingInspection")
public class PorterStemmerTest {
    @Test
    void misc() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.stem("names")).isEqualTo("name");
            softly.assertThat(PorterStemmerUtil.stem("j2ee")).isEqualTo("j2ee");
            softly.assertThat(PorterStemmerUtil.stem("keyword1")).isEqualTo("keyword1");
            softly.assertThat(PorterStemmerUtil.stem("go2file")).isEqualTo("go2file");
        });
    }

    /**
     * The following british-spelled words aren't supported:
     * initialisation, initialisations, initialise, initialised,
     * initialises, initialising, initialled, initialling,
     * initiatory.
     */
    @Test
    void initi() {
        String[] words = {
            "initial", "initialed", "initialing", "initialization",
            "initializations", "initialize", "initialized", "initializes",
            "initializing", "initially", "initials", "initiate",
            "initiated", "initiates", "initiating", "initiation",
            "initiations", "initiative", "initiatives", "initiator",
            "initiators"
        };
        assertSoftly(softly -> {
            for (String word : words) {
                softly.assertThat(PorterStemmerUtil.stem(word)).as(word).isEqualTo("initi");
            }
        });
    }

    @Test
    void emptyString() {
        assertThat(PorterStemmerUtil.stem("")).isNull();
    }

    @Test
    void nonLetter() {
        assertThat(PorterStemmerUtil.stem("-0_xs")).isEqualTo("-0_x");
    }

    @Test
    void nonLetterOrDigit() {
        assertThat(PorterStemmerUtil.stem("#")).isNull();
    }

    @Test
    void step1a() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step1a("caresses")).isEqualTo("caress");
            softly.assertThat(PorterStemmerUtil.step1a("ponies")).isEqualTo("poni");
            softly.assertThat(PorterStemmerUtil.step1a("ties")).isEqualTo("ti");
            softly.assertThat(PorterStemmerUtil.step1a("caress")).isEqualTo("caress");
            softly.assertThat(PorterStemmerUtil.step1a("cats")).isEqualTo("cat");
        });
    }

    @Test
    void step1b() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step1b("feed")).isEqualTo("feed");
            softly.assertThat(PorterStemmerUtil.step1b("agreed")).isEqualTo("agree");
            softly.assertThat(PorterStemmerUtil.step1b("plastered")).isEqualTo("plaster");
            softly.assertThat(PorterStemmerUtil.step1b("bled")).isEqualTo("bled");
            softly.assertThat(PorterStemmerUtil.step1b("motoring")).isEqualTo("motor");
            softly.assertThat(PorterStemmerUtil.step1b("sing")).isEqualTo("sing");
        });
    }

    @Test
    void step1b2() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step1b("conflated")).isEqualTo("conflate");
            softly.assertThat(PorterStemmerUtil.step1b("troubled")).isEqualTo("trouble");
            softly.assertThat(PorterStemmerUtil.step1b("sized")).isEqualTo("size");
            softly.assertThat(PorterStemmerUtil.step1b("hopping")).isEqualTo("hop");
            softly.assertThat(PorterStemmerUtil.step1b("tanned")).isEqualTo("tan");
            softly.assertThat(PorterStemmerUtil.step1b("falling")).isEqualTo("fall");
            softly.assertThat(PorterStemmerUtil.step1b("hissing")).isEqualTo("hiss");
            softly.assertThat(PorterStemmerUtil.step1b("fizzed")).isEqualTo("fizz");
            softly.assertThat(PorterStemmerUtil.step1b("failing")).isEqualTo("fail");
            softly.assertThat(PorterStemmerUtil.step1b("filing")).isEqualTo("file");
        });
    }

    @Test
    void step1c() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step1c("happy")).isEqualTo("happi");
            softly.assertThat(PorterStemmerUtil.step1c("sky")).isEqualTo("sky");
        });
    }

    @Test
    void step2() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step2("relational")).isEqualTo("relate");
            softly.assertThat(PorterStemmerUtil.step2("conditional")).isEqualTo("condition");
            softly.assertThat(PorterStemmerUtil.step2("rational")).isEqualTo("rational");
            softly.assertThat(PorterStemmerUtil.step2("valenci")).isEqualTo("valence");
            softly.assertThat(PorterStemmerUtil.step2("hesitanci")).isEqualTo("hesitance");
            softly.assertThat(PorterStemmerUtil.step2("digitizer")).isEqualTo("digitize");
            softly.assertThat(PorterStemmerUtil.step2("conformabli")).isEqualTo("conformable");
            softly.assertThat(PorterStemmerUtil.step2("radicalli")).isEqualTo("radical");
            softly.assertThat(PorterStemmerUtil.step2("differentli")).isEqualTo("different");
            softly.assertThat(PorterStemmerUtil.step2("vileli")).isEqualTo("vile");
            softly.assertThat(PorterStemmerUtil.step2("analogousli")).isEqualTo("analogous");
            softly.assertThat(PorterStemmerUtil.step2("vietnamization")).isEqualTo("vietnamize");
            softly.assertThat(PorterStemmerUtil.step2("predication")).isEqualTo("predicate");
            softly.assertThat(PorterStemmerUtil.step2("operator")).isEqualTo("operate");
            softly.assertThat(PorterStemmerUtil.step2("feudalism")).isEqualTo("feudal");
            softly.assertThat(PorterStemmerUtil.step2("decisiveness")).isEqualTo("decisive");
            softly.assertThat(PorterStemmerUtil.step2("hopefulness")).isEqualTo("hopeful");
            softly.assertThat(PorterStemmerUtil.step2("callousness")).isEqualTo("callous");
            softly.assertThat(PorterStemmerUtil.step2("formaliti")).isEqualTo("formal");
            softly.assertThat(PorterStemmerUtil.step2("sensitiviti")).isEqualTo("sensitive");
            softly.assertThat(PorterStemmerUtil.step2("sensibiliti")).isEqualTo("sensible");
        });
    }

    @Test
    void step3() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step3("triplicate")).isEqualTo("triplic");
            softly.assertThat(PorterStemmerUtil.step3("formative")).isEqualTo("form");
            softly.assertThat(PorterStemmerUtil.step3("formalize")).isEqualTo("formal");
            softly.assertThat(PorterStemmerUtil.step3("electriciti")).isEqualTo("electric");
            softly.assertThat(PorterStemmerUtil.step3("electrical")).isEqualTo("electric");
            softly.assertThat(PorterStemmerUtil.step3("hopeful")).isEqualTo("hope");
            softly.assertThat(PorterStemmerUtil.step3("goodness")).isEqualTo("good");
        });
    }

    @Test
    void step4() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step4("revival")).isEqualTo("reviv");
            softly.assertThat(PorterStemmerUtil.step4("allowance")).isEqualTo("allow");
            softly.assertThat(PorterStemmerUtil.step4("inference")).isEqualTo("infer");
            softly.assertThat(PorterStemmerUtil.step4("airliner")).isEqualTo("airlin");
            softly.assertThat(PorterStemmerUtil.step4("gyroscopic")).isEqualTo("gyroscop");
            softly.assertThat(PorterStemmerUtil.step4("adjustable")).isEqualTo("adjust");
            softly.assertThat(PorterStemmerUtil.step4("defensible")).isEqualTo("defens");
            softly.assertThat(PorterStemmerUtil.step4("irritant")).isEqualTo("irrit");
            softly.assertThat(PorterStemmerUtil.step4("replacement")).isEqualTo("replac");
            softly.assertThat(PorterStemmerUtil.step4("adjustment")).isEqualTo("adjust");
            softly.assertThat(PorterStemmerUtil.step4("dependent")).isEqualTo("depend");
            softly.assertThat(PorterStemmerUtil.step4("adoption")).isEqualTo("adopt");
            softly.assertThat(PorterStemmerUtil.step4("homologou")).isEqualTo("homolog");
            softly.assertThat(PorterStemmerUtil.step4("communism")).isEqualTo("commun");
            softly.assertThat(PorterStemmerUtil.step4("activate")).isEqualTo("activ");
            softly.assertThat(PorterStemmerUtil.step4("angulariti")).isEqualTo("angular");
            softly.assertThat(PorterStemmerUtil.step4("homologous")).isEqualTo("homolog");
            softly.assertThat(PorterStemmerUtil.step4("effective")).isEqualTo("effect");
            softly.assertThat(PorterStemmerUtil.step4("bowdlerize")).isEqualTo("bowdler");
        });
    }

    @Test
    void step5a() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step5a("probate")).isEqualTo("probat");
            softly.assertThat(PorterStemmerUtil.step5a("rate")).isEqualTo("rate");
            softly.assertThat(PorterStemmerUtil.step5a("cease")).isEqualTo("ceas");
        });
    }

    @Test
    void step5b() {
        assertSoftly(softly -> {
            softly.assertThat(PorterStemmerUtil.step5b("controll")).isEqualTo("control");
            softly.assertThat(PorterStemmerUtil.step5b("roll")).isEqualTo("roll");
        });
    }
}
