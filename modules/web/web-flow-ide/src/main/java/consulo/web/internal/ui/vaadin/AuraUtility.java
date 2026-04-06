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
package consulo.web.internal.ui.vaadin;

/**
 * CSS utility class constants for the Aura theme, replacing LumoUtility.
 * CSS rules are defined in frontend/themes/consulo/aura-utility.css.
 *
 * @author VISTALL
 * @since 2026-04-06
 */
public final class AuraUtility {
    private AuraUtility() {
    }

    public static final class Accessibility {
        public static final String SCREEN_READER_ONLY = "sr-only";

        private Accessibility() {
        }
    }

    public static final class AlignContent {
        public static final String AROUND = "content-around";
        public static final String BETWEEN = "content-between";
        public static final String CENTER = "content-center";
        public static final String END = "content-end";
        public static final String EVENLY = "content-evenly";
        public static final String START = "content-start";
        public static final String STRETCH = "content-stretch";

        private AlignContent() {
        }
    }

    public static final class AlignItems {
        public static final String BASELINE = "items-baseline";
        public static final String CENTER = "items-center";
        public static final String END = "items-end";
        public static final String START = "items-start";
        public static final String STRETCH = "items-stretch";

        private AlignItems() {
        }
    }

    public static final class AlignSelf {
        public static final String AUTO = "self-auto";
        public static final String BASELINE = "self-baseline";
        public static final String CENTER = "self-center";
        public static final String END = "self-end";
        public static final String START = "self-start";
        public static final String STRETCH = "self-stretch";

        private AlignSelf() {
        }
    }

    public static final class AspectRatio {
        public static final String SQUARE = "aspect-square";
        public static final String VIDEO = "aspect-video";

        private AspectRatio() {
        }
    }

    public static final class BackdropBlur {
        public static final String NONE = "backdrop-blur-none";
        public static final String SMALL = "backdrop-blur-sm";
        public static final String DEFAULT = "backdrop-blur";
        public static final String MEDIUM = "backdrop-blur-md";
        public static final String LARGE = "backdrop-blur-lg";
        public static final String XLARGE = "backdrop-blur-xl";
        public static final String XXLARGE = "backdrop-blur-2xl";
        public static final String XXXLARGE = "backdrop-blur-3xl";

        private BackdropBlur() {
        }
    }

    public static final class Background {
        public static final String BASE = "bg-base";
        public static final String TRANSPARENT = "bg-transparent";
        public static final String CONTRAST = "bg-contrast";
        public static final String CONTRAST_90 = "bg-contrast-90";
        public static final String CONTRAST_80 = "bg-contrast-80";
        public static final String CONTRAST_70 = "bg-contrast-70";
        public static final String CONTRAST_60 = "bg-contrast-60";
        public static final String CONTRAST_50 = "bg-contrast-50";
        public static final String CONTRAST_40 = "bg-contrast-40";
        public static final String CONTRAST_30 = "bg-contrast-30";
        public static final String CONTRAST_20 = "bg-contrast-20";
        public static final String CONTRAST_10 = "bg-contrast-10";
        public static final String CONTRAST_5 = "bg-contrast-5";
        public static final String TINT = "bg-tint";
        public static final String TINT_90 = "bg-tint-90";
        public static final String TINT_80 = "bg-tint-80";
        public static final String TINT_70 = "bg-tint-70";
        public static final String TINT_60 = "bg-tint-60";
        public static final String TINT_50 = "bg-tint-50";
        public static final String TINT_40 = "bg-tint-40";
        public static final String TINT_30 = "bg-tint-30";
        public static final String TINT_20 = "bg-tint-20";
        public static final String TINT_10 = "bg-tint-10";
        public static final String TINT_5 = "bg-tint-5";
        public static final String SHADE = "bg-shade";
        public static final String SHADE_90 = "bg-shade-90";
        public static final String SHADE_80 = "bg-shade-80";
        public static final String SHADE_70 = "bg-shade-70";
        public static final String SHADE_60 = "bg-shade-60";
        public static final String SHADE_50 = "bg-shade-50";
        public static final String SHADE_40 = "bg-shade-40";
        public static final String SHADE_30 = "bg-shade-30";
        public static final String SHADE_20 = "bg-shade-20";
        public static final String SHADE_10 = "bg-shade-10";
        public static final String SHADE_5 = "bg-shade-5";
        public static final String PRIMARY = "bg-primary";
        public static final String PRIMARY_50 = "bg-primary-50";
        public static final String PRIMARY_10 = "bg-primary-10";
        public static final String ERROR = "bg-error";
        public static final String ERROR_50 = "bg-error-50";
        public static final String ERROR_10 = "bg-error-10";
        public static final String WARNING = "bg-warning";
        public static final String WARNING_10 = "bg-warning-10";
        public static final String SUCCESS = "bg-success";
        public static final String SUCCESS_50 = "bg-success-50";
        public static final String SUCCESS_10 = "bg-success-10";

        private Background() {
        }
    }

    public static final class Border {
        public static final String NONE = "border-0";
        public static final String DASHED = "border-dashed";
        public static final String DOTTED = "border-dotted";
        public static final String ALL = "border";
        public static final String BOTTOM = "border-b";
        public static final String END = "border-e";
        public static final String LEFT = "border-l";
        public static final String RIGHT = "border-r";
        public static final String START = "border-s";
        public static final String TOP = "border-t";

        private Border() {
        }
    }

    public static final class BorderColor {
        public static final String CONTRAST = "border-contrast";
        public static final String CONTRAST_90 = "border-contrast-90";
        public static final String CONTRAST_80 = "border-contrast-80";
        public static final String CONTRAST_70 = "border-contrast-70";
        public static final String CONTRAST_60 = "border-contrast-60";
        public static final String CONTRAST_50 = "border-contrast-50";
        public static final String CONTRAST_40 = "border-contrast-40";
        public static final String CONTRAST_30 = "border-contrast-30";
        public static final String CONTRAST_20 = "border-contrast-20";
        public static final String CONTRAST_10 = "border-contrast-10";
        public static final String CONTRAST_5 = "border-contrast-5";
        public static final String PRIMARY = "border-primary";
        public static final String PRIMARY_50 = "border-primary-50";
        public static final String PRIMARY_10 = "border-primary-10";
        public static final String ERROR = "border-error";
        public static final String ERROR_50 = "border-error-50";
        public static final String ERROR_10 = "border-error-10";
        public static final String WARNING = "border-warning";
        public static final String WARNING_10 = "border-warning-10";
        public static final String WARNING_STRONG = "border-warning-strong";
        public static final String SUCCESS = "border-success";
        public static final String SUCCESS_50 = "border-success-50";
        public static final String SUCCESS_10 = "border-success-10";

        private BorderColor() {
        }
    }

    public static final class BorderRadius {
        public static final String NONE = "rounded-none";
        public static final String SMALL = "rounded-s";
        public static final String MEDIUM = "rounded-m";
        public static final String LARGE = "rounded-l";
        public static final String FULL = "rounded-full";

        private BorderRadius() {
        }
    }

    public static final class BoxShadow {
        public static final String NONE = "shadow-none";
        public static final String XSMALL = "shadow-xs";
        public static final String SMALL = "shadow-s";
        public static final String MEDIUM = "shadow-m";
        public static final String LARGE = "shadow-l";
        public static final String XLARGE = "shadow-xl";

        private BoxShadow() {
        }
    }

    public static final class BoxSizing {
        public static final String BORDER = "box-border";
        public static final String CONTENT = "box-content";

        private BoxSizing() {
        }
    }

    public static final class Display {
        public static final String BLOCK = "block";
        public static final String FLEX = "flex";
        public static final String GRID = "grid";
        public static final String HIDDEN = "hidden";
        public static final String INLINE = "inline";
        public static final String INLINE_BLOCK = "inline-block";
        public static final String INLINE_FLEX = "inline-flex";
        public static final String INLINE_GRID = "inline-grid";

        private Display() {
        }
    }

    public static final class Divide {
        public static final String X = "divide-x";
        public static final String Y = "divide-y";

        private Divide() {
        }
    }

    public static final class Flex {
        public static final String ONE = "flex-1";
        public static final String AUTO = "flex-auto";
        public static final String NONE = "flex-none";
        public static final String GROW = "flex-grow";
        public static final String GROW_NONE = "flex-grow-0";
        public static final String SHRINK = "flex-shrink";
        public static final String SHRINK_NONE = "flex-shrink-0";

        private Flex() {
        }
    }

    public static final class FlexDirection {
        public static final String COLUMN = "flex-col";
        public static final String COLUMN_REVERSE = "flex-col-reverse";
        public static final String ROW = "flex-row";
        public static final String ROW_REVERSE = "flex-row-reverse";

        private FlexDirection() {
        }
    }

    public static final class FlexWrap {
        public static final String NOWRAP = "flex-nowrap";
        public static final String WRAP = "flex-wrap";
        public static final String WRAP_REVERSE = "flex-wrap-reverse";

        private FlexWrap() {
        }
    }

    public static final class FontSize {
        public static final String XXSMALL = "text-2xs";
        public static final String XSMALL = "text-xs";
        public static final String SMALL = "text-s";
        public static final String MEDIUM = "text-m";
        public static final String LARGE = "text-l";
        public static final String XLARGE = "text-xl";
        public static final String XXLARGE = "text-2xl";
        public static final String XXXLARGE = "text-3xl";

        private FontSize() {
        }
    }

    public static final class FontWeight {
        public static final String THIN = "font-thin";
        public static final String EXTRALIGHT = "font-extralight";
        public static final String LIGHT = "font-light";
        public static final String NORMAL = "font-normal";
        public static final String MEDIUM = "font-medium";
        public static final String SEMIBOLD = "font-semibold";
        public static final String BOLD = "font-bold";
        public static final String EXTRABOLD = "font-extrabold";
        public static final String BLACK = "font-black";

        private FontWeight() {
        }
    }

    public static final class Gap {
        public static final String XSMALL = "gap-xs";
        public static final String SMALL = "gap-s";
        public static final String MEDIUM = "gap-m";
        public static final String LARGE = "gap-l";
        public static final String XLARGE = "gap-xl";

        private Gap() {
        }

        public static final class Column {
            public static final String XSMALL = "gap-x-xs";
            public static final String SMALL = "gap-x-s";
            public static final String MEDIUM = "gap-x-m";
            public static final String LARGE = "gap-x-l";
            public static final String XLARGE = "gap-x-xl";

            private Column() {
            }
        }

        public static final class Row {
            public static final String XSMALL = "gap-y-xs";
            public static final String SMALL = "gap-y-s";
            public static final String MEDIUM = "gap-y-m";
            public static final String LARGE = "gap-y-l";
            public static final String XLARGE = "gap-y-xl";

            private Row() {
            }
        }
    }

    public static final class Grid {
        public static final String FLOW_COLUMN = "grid-flow-col";
        public static final String FLOW_ROW = "grid-flow-row";

        private Grid() {
        }

        public static final class Column {
            public static final String COLUMNS_1 = "grid-cols-1";
            public static final String COLUMNS_2 = "grid-cols-2";
            public static final String COLUMNS_3 = "grid-cols-3";
            public static final String COLUMNS_4 = "grid-cols-4";
            public static final String COLUMNS_5 = "grid-cols-5";
            public static final String COLUMNS_6 = "grid-cols-6";
            public static final String COLUMNS_7 = "grid-cols-7";
            public static final String COLUMNS_8 = "grid-cols-8";
            public static final String COLUMNS_9 = "grid-cols-9";
            public static final String COLUMNS_10 = "grid-cols-10";
            public static final String COLUMNS_11 = "grid-cols-11";
            public static final String COLUMNS_12 = "grid-cols-12";
            public static final String COLUMN_SPAN_1 = "col-span-1";
            public static final String COLUMN_SPAN_2 = "col-span-2";
            public static final String COLUMN_SPAN_3 = "col-span-3";
            public static final String COLUMN_SPAN_4 = "col-span-4";
            public static final String COLUMN_SPAN_5 = "col-span-5";
            public static final String COLUMN_SPAN_6 = "col-span-6";
            public static final String COLUMN_SPAN_7 = "col-span-7";
            public static final String COLUMN_SPAN_8 = "col-span-8";
            public static final String COLUMN_SPAN_9 = "col-span-9";
            public static final String COLUMN_SPAN_10 = "col-span-10";
            public static final String COLUMN_SPAN_11 = "col-span-11";
            public static final String COLUMN_SPAN_12 = "col-span-12";

            private Column() {
            }
        }

        public static final class Row {
            public static final String ROWS_1 = "grid-rows-1";
            public static final String ROWS_2 = "grid-rows-2";
            public static final String ROWS_3 = "grid-rows-3";
            public static final String ROWS_4 = "grid-rows-4";
            public static final String ROWS_5 = "grid-rows-5";
            public static final String ROWS_6 = "grid-rows-6";
            public static final String ROW_SPAN_1 = "row-span-1";
            public static final String ROW_SPAN_2 = "row-span-2";
            public static final String ROW_SPAN_3 = "row-span-3";
            public static final String ROW_SPAN_4 = "row-span-4";
            public static final String ROW_SPAN_5 = "row-span-5";
            public static final String ROW_SPAN_6 = "row-span-6";

            private Row() {
            }
        }
    }

    public static final class Height {
        public static final String NONE = "h-0";
        public static final String XSMALL = "h-xs";
        public static final String SMALL = "h-s";
        public static final String MEDIUM = "h-m";
        public static final String LARGE = "h-l";
        public static final String XLARGE = "h-xl";
        public static final String AUTO = "h-auto";
        public static final String FULL = "h-full";
        public static final String SCREEN = "h-screen";

        private Height() {
        }
    }

    public static final class IconSize {
        public static final String SMALL = "icon-s";
        public static final String MEDIUM = "icon-m";
        public static final String LARGE = "icon-l";

        private IconSize() {
        }
    }

    public static final class JustifyContent {
        public static final String AROUND = "justify-around";
        public static final String BETWEEN = "justify-between";
        public static final String CENTER = "justify-center";
        public static final String END = "justify-end";
        public static final String EVENLY = "justify-evenly";
        public static final String START = "justify-start";

        private JustifyContent() {
        }
    }

    public static final class LineHeight {
        public static final String NONE = "leading-none";
        public static final String XSMALL = "leading-xs";
        public static final String SMALL = "leading-s";
        public static final String MEDIUM = "leading-m";

        private LineHeight() {
        }
    }

    public static final class ListStyleType {
        public static final String NONE = "list-none";

        private ListStyleType() {
        }
    }

    public static final class Margin {
        public static final String NONE = "m-0";
        public static final String XSMALL = "m-xs";
        public static final String SMALL = "m-s";
        public static final String MEDIUM = "m-m";
        public static final String LARGE = "m-l";
        public static final String XLARGE = "m-xl";
        public static final String AUTO = "m-auto";

        private Margin() {
        }

        public static final class Bottom {
            public static final String NONE = "mb-0";
            public static final String XSMALL = "mb-xs";
            public static final String SMALL = "mb-s";
            public static final String MEDIUM = "mb-m";
            public static final String LARGE = "mb-l";
            public static final String XLARGE = "mb-xl";
            public static final String AUTO = "mb-auto";

            private Bottom() {
            }
        }

        public static final class End {
            public static final String NONE = "me-0";
            public static final String XSMALL = "me-xs";
            public static final String SMALL = "me-s";
            public static final String MEDIUM = "me-m";
            public static final String LARGE = "me-l";
            public static final String XLARGE = "me-xl";
            public static final String AUTO = "me-auto";

            private End() {
            }
        }

        public static final class Horizontal {
            public static final String NONE = "mx-0";
            public static final String XSMALL = "mx-xs";
            public static final String SMALL = "mx-s";
            public static final String MEDIUM = "mx-m";
            public static final String LARGE = "mx-l";
            public static final String XLARGE = "mx-xl";
            public static final String AUTO = "mx-auto";

            private Horizontal() {
            }
        }

        public static final class Left {
            public static final String NONE = "ml-0";
            public static final String XSMALL = "ml-xs";
            public static final String SMALL = "ml-s";
            public static final String MEDIUM = "ml-m";
            public static final String LARGE = "ml-l";
            public static final String XLARGE = "ml-xl";
            public static final String AUTO = "ml-auto";

            private Left() {
            }
        }

        public static final class Right {
            public static final String NONE = "mr-0";
            public static final String XSMALL = "mr-xs";
            public static final String SMALL = "mr-s";
            public static final String MEDIUM = "mr-m";
            public static final String LARGE = "mr-l";
            public static final String XLARGE = "mr-xl";
            public static final String AUTO = "mr-auto";

            private Right() {
            }
        }

        public static final class Start {
            public static final String NONE = "ms-0";
            public static final String XSMALL = "ms-xs";
            public static final String SMALL = "ms-s";
            public static final String MEDIUM = "ms-m";
            public static final String LARGE = "ms-l";
            public static final String XLARGE = "ms-xl";
            public static final String AUTO = "ms-auto";

            private Start() {
            }
        }

        public static final class Top {
            public static final String NONE = "mt-0";
            public static final String XSMALL = "mt-xs";
            public static final String SMALL = "mt-s";
            public static final String MEDIUM = "mt-m";
            public static final String LARGE = "mt-l";
            public static final String XLARGE = "mt-xl";
            public static final String AUTO = "mt-auto";

            private Top() {
            }
        }

        public static final class Vertical {
            public static final String NONE = "my-0";
            public static final String XSMALL = "my-xs";
            public static final String SMALL = "my-s";
            public static final String MEDIUM = "my-m";
            public static final String LARGE = "my-l";
            public static final String XLARGE = "my-xl";
            public static final String AUTO = "my-auto";

            private Vertical() {
            }
        }

        public static final class Minus {
            private Minus() {
            }

            public static final class Bottom {
                public static final String XSMALL = "-mb-xs";
                public static final String SMALL = "-mb-s";
                public static final String MEDIUM = "-mb-m";
                public static final String LARGE = "-mb-l";
                public static final String XLARGE = "-mb-xl";

                private Bottom() {
                }
            }

            public static final class End {
                public static final String XSMALL = "-me-xs";
                public static final String SMALL = "-me-s";
                public static final String MEDIUM = "-me-m";
                public static final String LARGE = "-me-l";
                public static final String XLARGE = "-me-xl";

                private End() {
                }
            }

            public static final class Horizontal {
                public static final String XSMALL = "-mx-xs";
                public static final String SMALL = "-mx-s";
                public static final String MEDIUM = "-mx-m";
                public static final String LARGE = "-mx-l";
                public static final String XLARGE = "-mx-xl";

                private Horizontal() {
                }
            }

            public static final class Left {
                public static final String XSMALL = "-ml-xs";
                public static final String SMALL = "-ml-s";
                public static final String MEDIUM = "-ml-m";
                public static final String LARGE = "-ml-l";
                public static final String XLARGE = "-ml-xl";

                private Left() {
                }
            }

            public static final class Right {
                public static final String XSMALL = "-mr-xs";
                public static final String SMALL = "-mr-s";
                public static final String MEDIUM = "-mr-m";
                public static final String LARGE = "-mr-l";
                public static final String XLARGE = "-mr-xl";

                private Right() {
                }
            }

            public static final class Start {
                public static final String XSMALL = "-ms-xs";
                public static final String SMALL = "-ms-s";
                public static final String MEDIUM = "-ms-m";
                public static final String LARGE = "-ms-l";
                public static final String XLARGE = "-ms-xl";

                private Start() {
                }
            }

            public static final class Top {
                public static final String XSMALL = "-mt-xs";
                public static final String SMALL = "-mt-s";
                public static final String MEDIUM = "-mt-m";
                public static final String LARGE = "-mt-l";
                public static final String XLARGE = "-mt-xl";

                private Top() {
                }
            }

            public static final class Vertical {
                public static final String XSMALL = "-my-xs";
                public static final String SMALL = "-my-s";
                public static final String MEDIUM = "-my-m";
                public static final String LARGE = "-my-l";
                public static final String XLARGE = "-my-xl";

                private Vertical() {
                }
            }
        }
    }

    public static final class MaxHeight {
        public static final String FULL = "max-h-full";
        public static final String SCREEN = "max-h-screen";

        private MaxHeight() {
        }
    }

    public static final class MaxWidth {
        public static final String FULL = "max-w-full";
        public static final String SCREEN_SMALL = "max-w-screen-sm";
        public static final String SCREEN_MEDIUM = "max-w-screen-md";
        public static final String SCREEN_LARGE = "max-w-screen-lg";
        public static final String SCREEN_XLARGE = "max-w-screen-xl";
        public static final String SCREEN_XXLARGE = "max-w-screen-2xl";

        private MaxWidth() {
        }
    }

    public static final class MinHeight {
        public static final String NONE = "min-h-0";
        public static final String FULL = "min-h-full";
        public static final String SCREEN = "min-h-screen";

        private MinHeight() {
        }
    }

    public static final class MinWidth {
        public static final String NONE = "min-w-0";
        public static final String FULL = "min-w-full";

        private MinWidth() {
        }
    }

    public static final class Overflow {
        public static final String AUTO = "overflow-auto";
        public static final String HIDDEN = "overflow-hidden";
        public static final String SCROLL = "overflow-scroll";

        private Overflow() {
        }
    }

    public static final class Padding {
        public static final String NONE = "p-0";
        public static final String XSMALL = "p-xs";
        public static final String SMALL = "p-s";
        public static final String MEDIUM = "p-m";
        public static final String LARGE = "p-l";
        public static final String XLARGE = "p-xl";

        private Padding() {
        }

        public static final class Bottom {
            public static final String NONE = "pb-0";
            public static final String XSMALL = "pb-xs";
            public static final String SMALL = "pb-s";
            public static final String MEDIUM = "pb-m";
            public static final String LARGE = "pb-l";
            public static final String XLARGE = "pb-xl";

            private Bottom() {
            }
        }

        public static final class End {
            public static final String NONE = "pe-0";
            public static final String XSMALL = "pe-xs";
            public static final String SMALL = "pe-s";
            public static final String MEDIUM = "pe-m";
            public static final String LARGE = "pe-l";
            public static final String XLARGE = "pe-xl";

            private End() {
            }
        }

        public static final class Horizontal {
            public static final String NONE = "px-0";
            public static final String XSMALL = "px-xs";
            public static final String SMALL = "px-s";
            public static final String MEDIUM = "px-m";
            public static final String LARGE = "px-l";
            public static final String XLARGE = "px-xl";

            private Horizontal() {
            }
        }

        public static final class Left {
            public static final String NONE = "pl-0";
            public static final String XSMALL = "pl-xs";
            public static final String SMALL = "pl-s";
            public static final String MEDIUM = "pl-m";
            public static final String LARGE = "pl-l";
            public static final String XLARGE = "pl-xl";

            private Left() {
            }
        }

        public static final class Right {
            public static final String NONE = "pr-0";
            public static final String XSMALL = "pr-xs";
            public static final String SMALL = "pr-s";
            public static final String MEDIUM = "pr-m";
            public static final String LARGE = "pr-l";
            public static final String XLARGE = "pr-xl";

            private Right() {
            }
        }

        public static final class Start {
            public static final String NONE = "ps-0";
            public static final String XSMALL = "ps-xs";
            public static final String SMALL = "ps-s";
            public static final String MEDIUM = "ps-m";
            public static final String LARGE = "ps-l";
            public static final String XLARGE = "ps-xl";

            private Start() {
            }
        }

        public static final class Top {
            public static final String NONE = "pt-0";
            public static final String XSMALL = "pt-xs";
            public static final String SMALL = "pt-s";
            public static final String MEDIUM = "pt-m";
            public static final String LARGE = "pt-l";
            public static final String XLARGE = "pt-xl";

            private Top() {
            }
        }

        public static final class Vertical {
            public static final String NONE = "py-0";
            public static final String XSMALL = "py-xs";
            public static final String SMALL = "py-s";
            public static final String MEDIUM = "py-m";
            public static final String LARGE = "py-l";
            public static final String XLARGE = "py-xl";

            private Vertical() {
            }
        }
    }

    public static final class Position {
        public static final String ABSOLUTE = "absolute";
        public static final String FIXED = "fixed";
        public static final String RELATIVE = "relative";
        public static final String STATIC = "static";
        public static final String STICKY = "sticky";

        private Position() {
        }

        public static final class Bottom {
            public static final String NONE = "bottom-0";
            public static final String XSMALL = "bottom-xs";
            public static final String SMALL = "bottom-s";
            public static final String MEDIUM = "bottom-m";
            public static final String LARGE = "bottom-l";
            public static final String XLARGE = "bottom-xl";
            public static final String AUTO = "bottom-auto";
            public static final String FULL = "bottom-full";

            private Bottom() {
            }
        }

        public static final class End {
            public static final String NONE = "end-0";
            public static final String XSMALL = "end-xs";
            public static final String SMALL = "end-s";
            public static final String MEDIUM = "end-m";
            public static final String LARGE = "end-l";
            public static final String XLARGE = "end-xl";
            public static final String AUTO = "end-auto";
            public static final String FULL = "end-full";

            private End() {
            }
        }

        public static final class Start {
            public static final String NONE = "start-0";
            public static final String XSMALL = "start-xs";
            public static final String SMALL = "start-s";
            public static final String MEDIUM = "start-m";
            public static final String LARGE = "start-l";
            public static final String XLARGE = "start-xl";
            public static final String AUTO = "start-auto";
            public static final String FULL = "start-full";

            private Start() {
            }
        }

        public static final class Top {
            public static final String NONE = "top-0";
            public static final String XSMALL = "top-xs";
            public static final String SMALL = "top-s";
            public static final String MEDIUM = "top-m";
            public static final String LARGE = "top-l";
            public static final String XLARGE = "top-xl";
            public static final String AUTO = "top-auto";
            public static final String FULL = "top-full";

            private Top() {
            }
        }

        public static final class Minus {
            private Minus() {
            }

            public static final class Bottom {
                public static final String XSMALL = "-bottom-xs";
                public static final String SMALL = "-bottom-s";
                public static final String MEDIUM = "-bottom-m";
                public static final String LARGE = "-bottom-l";
                public static final String XLARGE = "-bottom-xl";
                public static final String FULL = "-bottom-full";

                private Bottom() {
                }
            }

            public static final class End {
                public static final String XSMALL = "-end-xs";
                public static final String SMALL = "-end-s";
                public static final String MEDIUM = "-end-m";
                public static final String LARGE = "-end-l";
                public static final String XLARGE = "-end-xl";
                public static final String FULL = "-end-full";

                private End() {
                }
            }

            public static final class Start {
                public static final String XSMALL = "-start-xs";
                public static final String SMALL = "-start-s";
                public static final String MEDIUM = "-start-m";
                public static final String LARGE = "-start-l";
                public static final String XLARGE = "-start-xl";
                public static final String FULL = "-start-full";

                private Start() {
                }
            }

            public static final class Top {
                public static final String XSMALL = "-top-xs";
                public static final String SMALL = "-top-s";
                public static final String MEDIUM = "-top-m";
                public static final String LARGE = "-top-l";
                public static final String XLARGE = "-top-xl";
                public static final String FULL = "-top-full";

                private Top() {
                }
            }
        }
    }

    public static final class TextAlignment {
        public static final String LEFT = "text-left";
        public static final String CENTER = "text-center";
        public static final String RIGHT = "text-right";
        public static final String JUSTIFY = "text-justify";

        private TextAlignment() {
        }
    }

    public static final class TextColor {
        public static final String HEADER = "text-header";
        public static final String BODY = "text-body";
        public static final String SECONDARY = "text-secondary";
        public static final String TERTIARY = "text-tertiary";
        public static final String DISABLED = "text-disabled";
        public static final String PRIMARY = "text-primary";
        public static final String PRIMARY_CONTRAST = "text-primary-contrast";
        public static final String ERROR = "text-error";
        public static final String ERROR_CONTRAST = "text-error-contrast";
        public static final String WARNING = "text-warning";
        public static final String WARNING_CONTRAST = "text-warning-contrast";
        public static final String SUCCESS = "text-success";
        public static final String SUCCESS_CONTRAST = "text-success-contrast";

        private TextColor() {
        }
    }

    public static final class TextOverflow {
        public static final String CLIP = "overflow-clip";
        public static final String ELLIPSIS = "overflow-ellipsis";

        private TextOverflow() {
        }
    }

    public static final class TextTransform {
        public static final String CAPITALIZE = "capitalize";
        public static final String LOWERCASE = "lowercase";
        public static final String UPPERCASE = "uppercase";

        private TextTransform() {
        }
    }

    public static final class Transition {
        public static final String NONE = "transition-none";
        public static final String ALL = "transition-all";
        public static final String DEFAULT = "transition";
        public static final String COLORS = "transition-colors";
        public static final String OPACITY = "transition-opacity";
        public static final String SHADOW = "transition-shadow";
        public static final String TRANSFORM = "transition-transform";

        private Transition() {
        }
    }

    public static final class Whitespace {
        public static final String NORMAL = "whitespace-normal";
        public static final String NOWRAP = "whitespace-nowrap";
        public static final String PRE = "whitespace-pre";
        public static final String PRE_LINE = "whitespace-pre-line";
        public static final String PRE_WRAP = "whitespace-pre-wrap";

        private Whitespace() {
        }
    }

    public static final class Width {
        public static final String XSMALL = "w-xs";
        public static final String SMALL = "w-s";
        public static final String MEDIUM = "w-m";
        public static final String LARGE = "w-l";
        public static final String XLARGE = "w-xl";
        public static final String AUTO = "w-auto";
        public static final String FULL = "w-full";

        private Width() {
        }
    }

    public static final class ZIndex {
        public static final String NONE = "z-0";
        public static final String XSMALL = "z-10";
        public static final String SMALL = "z-20";
        public static final String MEDIUM = "z-30";
        public static final String LARGE = "z-40";
        public static final String XLARGE = "z-50";
        public static final String AUTO = "z-auto";

        private ZIndex() {
        }
    }
}
