package consulo.ui.event.details;

import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyStrokeTest {
    @Test
    void testParsesKeymapForms() {
        assertThat(KeyStroke.parse("control shift A"))
            .isEqualTo(KeyStroke.pressed(KeyCode.A, Modifier.CTRL, Modifier.SHIFT));
        assertThat(KeyStroke.parse("ctrl pressed BACK_SPACE"))
            .isEqualTo(KeyStroke.pressed(KeyCode.BACK_SPACE, Modifier.CTRL));
        assertThat(KeyStroke.parse("alt released F1"))
            .isEqualTo(KeyStroke.released(KeyCode.F1, Modifier.ALT));
        assertThat(KeyStroke.parse("altGraph typed x"))
            .isEqualTo(KeyStroke.typed('x', Modifier.ALT_GRAPH));
        assertThat(KeyStroke.parse("META 0")).isEqualTo(KeyStroke.pressed(KeyCode.DIGIT_0, Modifier.META));
    }

    @Test
    void testIgnoresCase() {
        assertThat(KeyStroke.parse("control x")).isEqualTo(KeyStroke.pressed(KeyCode.X, Modifier.CTRL));
        assertThat(KeyStroke.parse("Shift Page_Down")).isEqualTo(KeyStroke.pressed(KeyCode.PAGE_DOWN, Modifier.SHIFT));
        assertThat(KeyStroke.parse("altgr separater")).isEqualTo(KeyStroke.pressed(KeyCode.SEPARATOR, Modifier.ALT_GRAPH));
    }

    @Test
    void testParsesKeyCodes() {
        KeyStroke stroke = KeyStroke.parse("shift #10b");
        assertThat(stroke).isNotNull();
        assertThat(stroke.getKeyCode().key()).isEqualTo(0x10b);
        assertThat(stroke.toString()).isEqualTo("shift #10b");
        assertThat(KeyStroke.parse("10")).isEqualTo(KeyStroke.pressed(KeyCode.ENTER));
    }

    @Test
    void testRejectsMalformed() {
        assertThat(KeyStroke.parse("")).isNull();
        assertThat(KeyStroke.parse("ctrl")).isNull();
        assertThat(KeyStroke.parse("ctrl NO_SUCH_KEY")).isNull();
        assertThat(KeyStroke.parse("super A")).isNull();
        assertThat(KeyStroke.parse("typed ab")).isNull();
        assertThat(KeyStroke.parse("released A B")).isNull();
        assertThat(KeyStroke.parse("UNDEFINED")).isNull();
    }

    @Test
    void testRoundTripsEveryKey() throws Exception {
        for (Field field : KeyCode.class.getFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType() != KeyCode.class) {
                continue;
            }
            KeyCode keyCode = (KeyCode) field.get(null);
            if (keyCode == KeyCode.UNDEFINED) {
                continue;
            }

            assertThat(KeyCode.of(keyCode.key())).isSameAs(keyCode);
            assertThat(KeyCode.byName(keyCode.name())).isSameAs(keyCode);

            KeyStroke pressed = KeyStroke.pressed(keyCode, Modifier.CTRL, Modifier.ALT);
            assertThat(KeyStroke.parse(pressed.toString())).isEqualTo(pressed);

            KeyStroke released = KeyStroke.released(keyCode, Modifier.SHIFT, Modifier.META, Modifier.ALT_GRAPH);
            assertThat(KeyStroke.parse(released.toString())).isEqualTo(released);
        }
    }

    @Test
    void testWritesModifiersInKeymapOrder() {
        assertThat(KeyStroke.released(KeyCode.F1, Modifier.ALT_GRAPH, Modifier.ALT, Modifier.META, Modifier.CTRL, Modifier.SHIFT))
            .hasToString("shift ctrl meta alt altGraph released F1");
        assertThat(KeyStroke.typed('a', Modifier.SHIFT)).hasToString("shift typed a");
        assertThat(KeyStroke.pressed(KeyCode.DIGIT_5)).hasToString("5");
    }
}
