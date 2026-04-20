//! Error dialog — MessageBox on Windows, CFUserNotification on macOS,
//! stderr fallback on Linux (GUI dialogs via zenity/kdialog are v2 work).

const TITLE: &str = "Cannot start Consulo";
const FOOTER: &str = "\n\nVisit https://consulo.help/platform/boot for troubleshooting.";

pub fn show_error(gui: bool, error: anyhow::Error) {
    let msg = format!("{error:?}{FOOTER}");
    if gui {
        show_alert(TITLE, &msg);
    } else {
        eprintln!("\n=== {TITLE} ===\n{msg}");
    }
}

#[cfg(target_os = "windows")]
fn show_alert(title: &str, text: &str) {
    use windows::Win32::UI::WindowsAndMessaging;
    use windows::core::HSTRING;
    let c_title = HSTRING::from(title);
    let c_text = HSTRING::from(text);
    unsafe {
        let _ = WindowsAndMessaging::MessageBoxW(
            None,
            windows::core::PCWSTR(c_text.as_ptr()),
            windows::core::PCWSTR(c_title.as_ptr()),
            WindowsAndMessaging::MB_OK
                | WindowsAndMessaging::MB_ICONERROR
                | WindowsAndMessaging::MB_APPLMODAL,
        );
    }
}

#[cfg(target_os = "macos")]
#[allow(non_snake_case)]
fn show_alert(title: &str, text: &str) {
    use core_foundation::base::{CFOptionFlags, SInt32, TCFType};
    use core_foundation::date::CFTimeInterval;
    use core_foundation::string::{CFString, CFStringRef};
    use core_foundation::url::CFURLRef;
    unsafe extern "C" {
        fn CFUserNotificationDisplayAlert(
            timeout: CFTimeInterval,
            flags: CFOptionFlags,
            iconURL: CFURLRef,
            soundURL: CFURLRef,
            localizationURL: CFURLRef,
            alertHeader: CFStringRef,
            alertMessage: CFStringRef,
            defaultButtonTitle: CFStringRef,
            alternateButtonTitle: CFStringRef,
            otherButtonTitle: CFStringRef,
            responseFlags: *mut CFOptionFlags,
        ) -> SInt32;
    }
    let header = CFString::new(title);
    let body = CFString::new(text);
    unsafe {
        let _ = CFUserNotificationDisplayAlert(
            0.0,
            0,
            std::ptr::null(),
            std::ptr::null(),
            std::ptr::null(),
            header.as_concrete_TypeRef(),
            body.as_concrete_TypeRef(),
            std::ptr::null(),
            std::ptr::null(),
            std::ptr::null(),
            std::ptr::null_mut(),
        );
    }
}

#[cfg(all(target_family = "unix", not(target_os = "macos")))]
fn show_alert(title: &str, text: &str) {
    eprintln!("\n=== {title} ===\n{text}");
}
