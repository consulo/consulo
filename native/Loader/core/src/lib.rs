// Consulo native launcher — shared core crate.
// See PLAN.md for overall design.

pub mod args;
#[cfg(target_os = "windows")]
pub mod args_windows;
pub mod build_discovery;
pub mod jre;
pub mod jvm;
#[cfg(target_os = "windows")]
pub mod jvm_hooks;
pub mod logging;
pub mod path_ext;
pub mod paths;
pub mod single_instance;
pub mod ui;
pub mod variant;
pub mod vmoptions;
