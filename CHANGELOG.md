# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2025-11-28

### Added
- Initial release
- TCP/IP printer connection support (port 9100)
- Bluetooth Classic (SPP) printer connection support
- USB Host printer connection support
- Persistent connection management
- Real-time event emission for connection status, errors, and print feedback
- ESC/POS command support via base64 encoding
- Printer status polling
- TypeScript type definitions
- Comprehensive documentation and examples
- React Native Turbo Module implementation for optimal performance

### Features
- `connectTCP()` - Connect to network printers
- `connectBT()` - Connect to Bluetooth printers
- `connectUSB()` - Connect to USB printers
- `disconnect()` - Disconnect from current printer
- `send()` - Send base64-encoded data to printer
- `pollStatus()` - Query printer status
- `addListener()` - Subscribe to printer events
- `removeAllListeners()` - Clean up event listeners

### Platform Support
- Android: ✅ Full support
- iOS: ❌ Not supported (future consideration)

### Known Limitations
- Single connection at a time
- No built-in print queue
- No automatic reconnection
- USB permission flow requires manual implementation
- Android only
