//! Compile-time UI variant selection (AWT / SWT / future).
//!
//! Variant is chosen via Cargo features. Exactly one must be enabled.
//! Constants here bake the flavor into both `loader` and `library` binaries.

#[cfg(all(feature = "awt", feature = "swt"))]
compile_error!("exactly one of `awt` / `swt` features must be enabled, not both");

#[cfg(not(any(feature = "awt", feature = "swt")))]
compile_error!("one of `awt` / `swt` features must be enabled");

#[cfg(feature = "awt")]
pub const VARIANT_NAME: &str = "awt";
#[cfg(feature = "awt")]
pub const MAIN_CLASS: &str = "consulo/desktop/awt/boot/main/Main";
#[cfg(feature = "awt")]
pub const MAIN_MODULE: &str = "consulo.desktop.awt.bootstrap";

#[cfg(feature = "swt")]
pub const VARIANT_NAME: &str = "swt";
#[cfg(feature = "swt")]
pub const MAIN_CLASS: &str = "consulo/desktop/swt/boot/main/Main";
#[cfg(feature = "swt")]
pub const MAIN_MODULE: &str = "consulo.desktop.swt.bootstrap";

/// Filename suffix appended to binary / library names per variant.
/// AWT is the default, no suffix; other variants use `-<name>`.
pub const fn filename_suffix() -> &'static str {
    #[cfg(feature = "awt")]
    {
        ""
    }
    #[cfg(feature = "swt")]
    {
        "-swt"
    }
}
