import AppKit

let outputURL = URL(fileURLWithPath: "android/play-store-assets/inciteam-feature-graphic-1024x500.png")
let iconURL = URL(fileURLWithPath: "/Users/parthsarvan/Desktop/InciTeam-iOS-Default-1024x1024@1x.png")

let canvasSize = NSSize(width: 1024, height: 500)
let image = NSImage(size: canvasSize)

func drawText(_ text: String, in rect: NSRect, size: CGFloat, weight: NSFont.Weight, color: NSColor, alignment: NSTextAlignment = .left) {
    let paragraph = NSMutableParagraphStyle()
    paragraph.alignment = alignment
    paragraph.lineSpacing = 4
    let attributes: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: size, weight: weight),
        .foregroundColor: color,
        .paragraphStyle: paragraph
    ]
    text.draw(in: rect, withAttributes: attributes)
}

func roundedPath(_ rect: NSRect, radius: CGFloat) -> NSBezierPath {
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
}

image.lockFocus()

let context = NSGraphicsContext.current!.cgContext
context.setFillColor(NSColor(red: 0.03, green: 0.12, blue: 0.34, alpha: 1).cgColor)
context.fill(CGRect(origin: .zero, size: canvasSize))

let gradient = NSGradient(colors: [
    NSColor(red: 0.16, green: 0.47, blue: 0.95, alpha: 1),
    NSColor(red: 0.03, green: 0.18, blue: 0.56, alpha: 1),
    NSColor(red: 0.02, green: 0.08, blue: 0.25, alpha: 1)
])!
gradient.draw(in: NSRect(origin: .zero, size: canvasSize), angle: 0)

NSColor.white.withAlphaComponent(0.10).setFill()
roundedPath(NSRect(x: 620, y: -70, width: 520, height: 360), radius: 180).fill()
NSColor(red: 0.32, green: 0.78, blue: 1.0, alpha: 0.13).setFill()
roundedPath(NSRect(x: 710, y: 245, width: 370, height: 210), radius: 105).fill()
NSColor.white.withAlphaComponent(0.08).setStroke()
let arc = NSBezierPath()
arc.lineWidth = 8
arc.appendArc(withCenter: NSPoint(x: 740, y: 245), radius: 175, startAngle: 205, endAngle: 58)
arc.stroke()

if let icon = NSImage(contentsOf: iconURL) {
    let iconRect = NSRect(x: 664, y: 92, width: 300, height: 300)
    NSShadow().apply {
        $0.shadowColor = NSColor.black.withAlphaComponent(0.28)
        $0.shadowBlurRadius = 28
        $0.shadowOffset = NSSize(width: 0, height: -12)
    }
    icon.draw(in: iconRect)
}

drawText(
    "InciTeam",
    in: NSRect(x: 68, y: 312, width: 540, height: 72),
    size: 64,
    weight: .heavy,
    color: .white
)

drawText(
    "ServiceNow incident assignment and team operations",
    in: NSRect(x: 72, y: 238, width: 560, height: 78),
    size: 30,
    weight: .semibold,
    color: NSColor.white.withAlphaComponent(0.88)
)

let pillTexts = ["Roster", "Schedules", "Alerts", "Logs"]
var pillX: CGFloat = 72
for pill in pillTexts {
    let width = CGFloat(82 + pill.count * 8)
    let rect = NSRect(x: pillX, y: 174, width: width, height: 42)
    NSColor.white.withAlphaComponent(0.16).setFill()
    roundedPath(rect, radius: 21).fill()
    NSColor.white.withAlphaComponent(0.28).setStroke()
    roundedPath(rect, radius: 21).stroke()
    drawText(pill, in: NSRect(x: pillX, y: 184, width: width, height: 22), size: 16, weight: .bold, color: .white, alignment: .center)
    pillX += width + 12
}

drawText(
    "Operational visibility for the teams who keep incidents moving.",
    in: NSRect(x: 72, y: 112, width: 560, height: 42),
    size: 20,
    weight: .medium,
    color: NSColor.white.withAlphaComponent(0.74)
)

image.unlockFocus()

guard let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff),
      let pngData = bitmap.representation(using: .png, properties: [:]) else {
    fatalError("Unable to render feature graphic")
}

try pngData.write(to: outputURL)

extension NSShadow {
    func apply(_ configure: (NSShadow) -> Void) {
        configure(self)
        self.set()
    }
}
