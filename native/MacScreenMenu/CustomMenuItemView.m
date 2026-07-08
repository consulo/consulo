#include "CustomMenuItemView.h"
#include "Menu.h"

@implementation CustomMenuItemView

static CGFloat menuItemHeight = 18.f;
static CGFloat marginLeft = 20.f;
static CGFloat marginRight = 10.f;

static CGFloat gapTxtIcon = 5.f;
static CGFloat gapTxtShortcut = 23.f;

static NSFont * menuFont;
static NSFont * menuShortcutFont;

static NSColor * customBg = nil;

+ (void)initialize {
    // menu (dropdown) font, not the menu-bar font
    menuFont = [NSFont menuFontOfSize:(0)];
    menuShortcutFont = [NSFont menuFontOfSize:(0)];

    NSDictionary * attributes = [NSDictionary dictionaryWithObjectsAndKeys:menuFont, NSFontAttributeName, nil];
    NSSize qSize = [[[[NSAttributedString alloc] initWithString:@"Q" attributes:attributes] autorelease] size];
    CGFloat fontHeight = qSize.height;

    // native-like row height; margins are based on the font (not the row height) so the text indent stays stable
    menuItemHeight = fontHeight * 1.35f;
    marginLeft = fontHeight * 1.2f;
    marginRight = fontHeight * 0.6f;

    gapTxtIcon = fontHeight * 0.3f;
    gapTxtShortcut = fontHeight * 1.2f;

    // Initialize custom bg color (for light theme with enabled accessibility.reduceTransparency)
    // If we use transparent bg than we will see visual inconsistency
    // And it seems that we can't obtain this color from system
    NSUserDefaults * defs = [NSUserDefaults standardUserDefaults];
    NSDictionary<NSString *,id> * dict = [defs persistentDomainForName:@"com.apple.universalaccess.plist"];
    if (dict != nil) {
        id reduceVal = [dict valueForKey:@"reduceTransparency"];
        if (reduceVal != nil && [reduceVal isKindOfClass:[NSNumber class]] && [reduceVal intValue] != 0) {
            NSString * mode = [defs stringForKey:@"AppleInterfaceStyle"];
            if (mode == nil) { // light system theme
                customBg = [NSColor colorWithCalibratedWhite:246.f/255 alpha:1.f];
                [customBg retain];
                // NSLog(@"\treduceTransparency is enabled (use custom background color for menu items)");
            }
        }
    }

    // NSLog(@"\tmenuItemHeight=%1.2f, marginLeft=%1.2f, marginRight=%1.2f, gapTxtIcon=%1.2f, gapTxtShortcut=%1.2f",
    //      menuItemHeight, marginLeft, marginRight, gapTxtIcon, gapTxtShortcut);
}

- (id)initWithOwner:(MenuItem *)menuItem {
    NSRect viewRect = NSMakeRect(0, 0, /* width autoresizes */ 1, menuItemHeight);
    self = [super initWithFrame:viewRect];
    if (self == nil) {
        return self;
    }

    owner = menuItem;

    self.autoresizingMask = NSViewWidthSizable;
    self.keyShortcut = nil;

    fireTimes = 0;
    isSelected = NO;
    trackingArea = nil;
    shortcutSize = NSZeroSize;
    textSize = NSZeroSize;

    return self;
}

- (void)dealloc {
    if(trackingArea != nil) {
        [trackingArea release];
    }

    [super dealloc];
}

- (void)setSelected:(BOOL)selected {
    if (isSelected == selected) return;
    if (owner == nil || owner->nsMenuItem == nil) return;
    if (isSelected) {
        isSelected = NO;
    } else if (owner->nsMenuItem.enabled) {
        isSelected = YES;
    }

    [self setNeedsDisplay:YES];
}

- (void)mouseEntered:(NSEvent*)event {
    [self setSelected:YES];
}

- (void)mouseExited:(NSEvent *)event {
    [self setSelected:NO];
}

- (void)mouseUp:(NSEvent*)event {
    if (owner == nil || owner->nsMenuItem == nil) return;
    if (!(owner->nsMenuItem.enabled)) return;

    [self setSelected:!isSelected];

    fireTimes = 0;
    NSTimer *timer = [NSTimer timerWithTimeInterval:0.05 target:self selector:@selector(animateDismiss:) userInfo:nil repeats:YES];
    [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSEventTrackingRunLoopMode];
}

- (void)keyDown:(NSEvent *)event {
    [[self nextResponder] keyDown:event];
    if ((event.keyCode == 36)  ||
        (event.keyCode == 76)  ||
        [[event characters] isEqualToString:@" "]
    )
        [self sendAction];
}

- (BOOL)acceptsFirstResponder {
    return YES;
}

-(void)updateTrackingAreas {
    if (owner == nil || owner->nsMenuItem == nil) return;
    [super updateTrackingAreas];
    if(trackingArea != nil) {
        [self removeTrackingArea:trackingArea];
        [trackingArea release];
    }

    int opts = (NSTrackingMouseEnteredAndExited | NSTrackingActiveAlways);
    trackingArea = [[NSTrackingArea alloc] initWithRect:[self bounds]
                                                options:opts
                                                  owner:self
                                               userInfo:nil];
    [self addTrackingArea:trackingArea];
}

-(void)animateDismiss:(NSTimer *)aTimer {
    if (fireTimes <= 2) {
        isSelected = !isSelected;
        [self setNeedsDisplay:YES];
    } else {
        [aTimer invalidate];
        [self sendAction];
    }

    fireTimes++;
}

- (void)sendAction {
    if (owner == nil || owner->nsMenuItem == nil) return;
    NSMenuItem * mi = owner->nsMenuItem;
    [NSApp sendAction:[mi action] to:[mi target] from:mi];

    NSMenu *menu = [mi menu];
    [menu cancelTracking];

    // NOTE: we can also invoke handler directly [owner handleAction:[owner menuItem]];
}

//#define VISUAL_DEBUG_CUSTOM_ITEM_VIEW

- (void) drawRect:(NSRect)dirtyRect {
    if (owner == nil || owner->nsMenuItem == nil) return;
    NSRect rectBounds = [self bounds];
    NSString * text = owner->nsMenuItem.title;

    // resolve system colors against the menu's (possibly dark) appearance, otherwise text/arrow can be dark-on-dark
    NSAppearance *previousAppearance = nil;
    if (@available(macOS 10.14, *)) {
        previousAppearance = [NSAppearance currentAppearance];
        [NSAppearance setCurrentAppearance:[self effectiveAppearance]];
    }

    const BOOL isEnabled = owner->nsMenuItem.enabled;

    NSColor * textColor = [NSColor textColor];
    NSColor * bgColor = customBg != nil ? customBg : [NSColor clearColor];
    if (!isEnabled) {
        textColor = [NSColor disabledControlTextColor];
    } else if (isSelected) {
        if (@available(macOS 10.14, *)) {
            bgColor = [NSColor controlAccentColor];
        } else {
            bgColor = [NSColor selectedControlColor];
        }
        textColor = [NSColor selectedMenuItemTextColor];
    }

    // 1. draw bg: opaque only when reduceTransparency requires it; the selection is a rounded, inset rect like native
    if (customBg != nil) {
        [customBg set];
        NSRectFill(rectBounds);
    }
    if (isSelected && isEnabled) {
        NSRect selRect = NSInsetRect(rectBounds, marginRight * 0.6f, 1.0f);
        NSBezierPath * selPath = [NSBezierPath bezierPathWithRoundedRect:selRect xRadius:5.0f yRadius:5.0f];
        [bgColor set];
        [selPath fill];
    }

    // 2. draw icon if presented
    NSImage * image = owner->nsMenuItem.image;
    // reserve the left icon column only when there actually is an icon, otherwise the text is over-indented
    CGFloat x = rectBounds.origin.x + (image != nil ? marginLeft : marginRight);
    if (image != nil) {
        NSRect imageBounds = rectBounds;
        imageBounds.origin.x = x;
        imageBounds.origin.y = rectBounds.origin.y + (rectBounds.size.height - image.size.height) / 2;
        imageBounds.size = image.size;
        [image drawInRect:imageBounds];

        x += image.size.width + gapTxtIcon;
    }

    // 3. draw text (vertically centered)
    NSDictionary *attributes = [NSDictionary dictionaryWithObjectsAndKeys:
            menuFont, NSFontAttributeName,
            textColor, NSForegroundColorAttributeName,
                    nil];
    NSRect txtBounds = NSMakeRect(x,
                                  rectBounds.origin.y + (rectBounds.size.height - textSize.height) / 2,
                                  textSize.width, textSize.height);
    [text drawInRect:txtBounds withAttributes:attributes];

    if (self.keyShortcut != nil) {
        // 4.1 draw shortcut (vertically centered)
        NSRect keyBounds = NSMakeRect(rectBounds.size.width - marginRight - shortcutSize.width,
                                      rectBounds.origin.y + (rectBounds.size.height - shortcutSize.height) / 2,
                                      shortcutSize.width, shortcutSize.height);
        NSDictionary *keyAttr = [NSDictionary dictionaryWithObjectsAndKeys:
                menuShortcutFont, NSFontAttributeName,
                textColor, NSForegroundColorAttributeName,
                        nil];
        [self.keyShortcut drawInRect:keyBounds withAttributes:keyAttr];
    } else if ([owner isKindOfClass:Menu.class]) {
        // 4.2 draw submenu arrow: template image tinted with the text color and sized to the menu font
        NSImage *arrow = [[[NSImage imageNamed:NSImageNameRightFacingTriangleTemplate] copy] autorelease];
        CGFloat arrowH = menuItemHeight * 0.5f;
        CGFloat arrowW = arrow.size.height > 0 ? arrowH * (arrow.size.width / arrow.size.height) : arrowH;
        NSRect arrowBounds = NSMakeRect(rectBounds.size.width - marginRight - arrowW,
                                        rectBounds.origin.y + (rectBounds.size.height - arrowH) / 2,
                                        arrowW, arrowH);
        [arrow setTemplate:NO];
        [arrow lockFocus];
        [textColor set];
        NSRectFillUsingOperation(NSMakeRect(0, 0, arrow.size.width, arrow.size.height), NSCompositingOperationSourceAtop);
        [arrow unlockFocus];
        [arrow drawInRect:arrowBounds];
    }

    if (@available(macOS 10.14, *)) {
        [NSAppearance setCurrentAppearance:previousAppearance];
    }
}

- (void)recalcSizes {
    if (owner == nil || owner->nsMenuItem == nil) return;
    NSString * text = owner->nsMenuItem.title;
    NSImage * image = owner->nsMenuItem.image;

    NSDictionary * attributes = [NSDictionary dictionaryWithObjectsAndKeys:menuFont, NSFontAttributeName, nil];
    textSize = [[[[NSAttributedString alloc] initWithString:text attributes:attributes] autorelease] size];

    CGFloat leftInset = (image != nil) ? marginLeft : marginRight;
    NSSize resultSize = NSMakeSize(textSize.width + leftInset + marginRight, menuItemHeight);

    if (image != nil) {
        NSSize imgSize = image.size;
        resultSize.width += imgSize.width + gapTxtIcon;
    }

    if (self.keyShortcut != nil) {
        NSDictionary * ksa = [NSDictionary dictionaryWithObjectsAndKeys:menuShortcutFont, NSFontAttributeName, nil];
        shortcutSize = [[[[NSAttributedString alloc] initWithString:self.keyShortcut attributes:ksa] autorelease] size];
        resultSize.width += shortcutSize.width + gapTxtShortcut;
    }

    [self.widthAnchor constraintGreaterThanOrEqualToConstant:resultSize.width].active = YES;
}

@end