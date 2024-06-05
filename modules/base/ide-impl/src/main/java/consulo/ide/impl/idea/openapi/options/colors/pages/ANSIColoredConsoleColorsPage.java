package consulo.ide.impl.idea.openapi.options.colors.pages;

import consulo.annotation.component.ExtensionImpl;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorDescriptor;
import consulo.configurable.internal.ConfigurableWeight;
import consulo.configurable.localize.ConfigurableLocalize;
import consulo.execution.process.ConsoleHighlighter;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author oleg, Roman.Chernyatchik
 */
@ExtensionImpl(id = "ansi")
public class ANSIColoredConsoleColorsPage implements ColorSettingsPage, ConfigurableWeight {

  private static final String DEMO_TEXT =
    "<stdsys>C:\\command.com</stdsys>\n" +
    "-<stdout> C:></stdout>\n" +
    "-<stdin> help</stdin>\n" +
    "<stderr>Bad command or file name</stderr>\n" +
    "\n" +
    "<logError>Log error</logError>\n" +
    "<logWarning>Log warning</logWarning>\n" +
    "<logExpired>An expired log entry</logExpired>\n" +
    "\n" +
    "# Process output highlighted using ANSI colors codes\n" +
    "<red>ANSI: red</red>\n" +
    "<green>ANSI: green</green>\n" +
    "<yellow>ANSI: yellow</yellow>\n" +
    "<blue>ANSI: blue</blue>\n" +
    "<magenta>ANSI: magenta</magenta>\n" +
    "<cyan>ANSI: cyan</cyan>\n" +
    "<gray>ANSI: gray</gray>\n" +
    "\n" +
    "<stdsys>Process finished with exit code 1</stdsys>\n";

  private static final AttributesDescriptor[] ATTRS = {
    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleStdout(),
      ConsoleViewContentType.NORMAL_OUTPUT_KEY
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleStderr(),
      ConsoleViewContentType.ERROR_OUTPUT_KEY
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleStdin(),
      ConsoleViewContentType.USER_INPUT_KEY
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleSystemOutput(),
      ConsoleViewContentType.SYSTEM_OUTPUT_KEY
    ),

    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleLogerror(),
      ConsoleViewContentType.LOG_ERROR_OUTPUT_KEY
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleWarning(),
      ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY
    ),
    new AttributesDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleExpired(),
      ConsoleViewContentType.LOG_EXPIRED_ENTRY
    ),

    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleRed(), ConsoleHighlighter.RED),
    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleGreen(), ConsoleHighlighter.GREEN),
    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleYellow(), ConsoleHighlighter.YELLOW),
    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleBlue(), ConsoleHighlighter.BLUE),
    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleMagenta(), ConsoleHighlighter.MAGENTA),
    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleCyan(), ConsoleHighlighter.CYAN),
    new AttributesDescriptor(ConfigurableLocalize.colorSettingsConsoleGray(), ConsoleHighlighter.GRAY),
  };

  private static final Map<String, TextAttributesKey> ADDITIONAL_HIGHLIGHT_DESCRIPTORS = new HashMap<>();
  static{
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stdsys", ConsoleViewContentType.SYSTEM_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stdout", ConsoleViewContentType.NORMAL_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stdin", ConsoleViewContentType.USER_INPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("stderr", ConsoleViewContentType.ERROR_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logError", ConsoleViewContentType.LOG_ERROR_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logWarning", ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("logExpired", ConsoleViewContentType.LOG_EXPIRED_ENTRY);

    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("red", ConsoleHighlighter.RED);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("green", ConsoleHighlighter.GREEN);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("yellow", ConsoleHighlighter.YELLOW);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("blue", ConsoleHighlighter.BLUE);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("magenta", ConsoleHighlighter.MAGENTA);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("cyan", ConsoleHighlighter.CYAN);
    ADDITIONAL_HIGHLIGHT_DESCRIPTORS.put("gray", ConsoleHighlighter.GRAY);
  }

  private static final ColorDescriptor[] COLORS = {
    new ColorDescriptor(
      ConfigurableLocalize.optionsGeneralColorDescriptorConsoleBackground(),
      ConsoleViewContentType.CONSOLE_BACKGROUND_KEY,
      ColorDescriptor.Kind.BACKGROUND
    ),
  };

  @Override
  @Nullable
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return ADDITIONAL_HIGHLIGHT_DESCRIPTORS;
  }

  @Override
  @Nonnull
  public String getDisplayName() {
    return ConfigurableLocalize.colorSettingsConsoleName().get();
  }

  @Override
  @Nonnull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  @Override
  @Nonnull
  public ColorDescriptor[] getColorDescriptors() {
    return COLORS;
  }

  @Override
  @Nonnull
  public SyntaxHighlighter getHighlighter() {
     return new DefaultSyntaxHighlighter();
  }

  @Override
  @Nonnull
  public String getDemoText() {
    return DEMO_TEXT;
  }

  @Override
  public int getConfigurableWeight() {
    return Integer.MAX_VALUE - 1;
  }
}
