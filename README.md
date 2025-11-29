# react-native-printer-service

Native Android printer service with persistent connections for TCP, Bluetooth Classic (SPP), and USB thermal printers. Built with Kotlin and React Native Turbo Modules for high performance.

## Features

- ✨ **Multiple Connection Types**: TCP (port 9100), Bluetooth Classic, USB Host
- 🔄 **Persistent Connections**: Maintain long-lived connections for faster printing
- 📡 **Real-time Events**: EventEmitter for connection status, errors, and print feedback
- ⚡ **Turbo Module**: Built on React Native's new architecture for optimal performance
- 🎯 **TypeScript Support**: Fully typed API for better developer experience
- 📄 **ESC/POS Compatible**: Send raw ESC/POS commands to thermal printers

## Installation

```sh
npm install react-native-printer-service
```

or

```sh
yarn add react-native-printer-service
```

### Android Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-feature android:name="android.hardware.usb.host" />
```

For Android 12+ (API 31+), you'll need to request runtime permissions for Bluetooth:

```typescript
import { PermissionsAndroid, Platform } from 'react-native';

if (Platform.OS === 'android' && Platform.Version >= 31) {
  await PermissionsAndroid.requestMultiple([
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
  ]);
}
```

## Usage

```typescript
import PrinterService, { type PrinterEvent } from 'react-native-printer-service';

// Subscribe to printer events
const subscription = PrinterService.addListener((event: PrinterEvent) => {
  console.log('Printer event:', event);
  // event.type: 'connected' | 'disconnected' | 'status' | 'printed' | 'error' | 'permission_required'
  // event.data: optional string with additional information
});

// Connect via TCP
const connected = await PrinterService.connectTCP('192.168.1.100', 9100);

// Connect via Bluetooth
const connected = await PrinterService.connectBT('00:11:22:33:44:55');

// Connect via USB (connects to first available USB device)
const connected = await PrinterService.connectUSB();

// Send ESC/POS commands (base64 encoded)
const escPosData = Buffer.from('Hello World\n\n\n', 'utf8').toString('base64');
await PrinterService.send(escPosData);

// Poll printer status
const statusHex = await PrinterService.pollStatus();
console.log('Printer status:', statusHex);

// Disconnect
await PrinterService.disconnect();

// Cleanup
subscription.remove();
PrinterService.removeAllListeners();
```

## API Reference

### Methods

#### `connectTCP(host: string, port?: number): Promise<boolean>`

Connect to a printer via TCP/IP.

- `host`: IP address or hostname of the printer
- `port`: Port number (default: 9100)
- Returns: `Promise<boolean>` - true if connected successfully

#### `connectBT(mac: string): Promise<boolean>`

Connect to a printer via Bluetooth Classic.

- `mac`: Bluetooth MAC address (format: "00:11:22:33:44:55")
- Returns: `Promise<boolean>` - true if connected successfully

#### `connectUSB(): Promise<boolean>`

Connect to a printer via USB.

- Note: Connects to the first available USB printer device
- Returns: `Promise<boolean>` - true if connected successfully

#### `disconnect(): Promise<boolean>`

Disconnect from the current printer.

- Returns: `Promise<boolean>` - true if disconnected successfully

#### `send(base64: string): Promise<boolean>`

Send data to the printer.

- `base64`: Base64-encoded data (typically ESC/POS commands)
- Returns: `Promise<boolean>` - true if sent successfully

#### `pollStatus(): Promise<string | null>`

Poll the printer status.

- Returns: `Promise<string | null>` - Hex string of status bytes or null

#### `addListener(callback: (event: PrinterEvent) => void): EmitterSubscription`

Add a listener for printer events.

- `callback`: Function to handle printer events
- Returns: Subscription object with `remove()` method

#### `removeAllListeners(): void`

Remove all printer event listeners.

### Events

The library emits the following events via the event listener:

```typescript
interface PrinterEvent {
  type: 'connected' | 'disconnected' | 'status' | 'printed' | 'error' | 'permission_required';
  data?: string;
}
```

- **connected**: Successfully connected to printer (data: connection info)
- **disconnected**: Disconnected from printer
- **printed**: Data sent successfully (data: bytes sent)
- **status**: Status poll response (data: status value)
- **error**: Error occurred (data: error message)
- **permission_required**: USB permission needed (USB only)

## Example: Printing a Receipt

```typescript
import PrinterService from 'react-native-printer-service';

async function printReceipt() {
  // Connect to TCP printer
  await PrinterService.connectTCP('192.168.1.100');

  // Build ESC/POS commands
  const commands = [
    '\x1B\x40',           // Initialize printer
    '\x1B\x61\x01',       // Center align
    'MY STORE\n',
    '\x1B\x61\x00',       // Left align
    '------------------------\n',
    'Item 1          $10.00\n',
    'Item 2          $15.00\n',
    '------------------------\n',
    'Total:          $25.00\n',
    '\n\n\n',             // Feed paper
    '\x1D\x56\x00',       // Cut paper
  ].join('');

  // Convert to base64
  const base64Data = Buffer.from(commands, 'utf8').toString('base64');
  
  // Send to printer
  await PrinterService.send(base64Data);
  
  // Disconnect
  await PrinterService.disconnect();
}
```

## Platform Support

| Platform | TCP | Bluetooth | USB |
|----------|-----|-----------|-----|
| Android  | ✅  | ✅        | ✅  |
| iOS      | ❌  | ❌        | ❌  |

**Note**: This is currently an Android-only library. iOS support may be added in future versions.

## Limitations & Considerations

This is a proof-of-concept (PoC) implementation focused on core functionality:

- ❌ No built-in print queue or retry logic
- ❌ USB permission flow requires manual implementation
- ❌ Single connection at a time
- ❌ Limited error recovery

For production use, consider implementing:
- Print queue management
- Automatic reconnection with exponential backoff
- Multiple simultaneous connections
- Enhanced error handling and logging
- USB permission request flow using `PendingIntent`

## Troubleshooting

### Bluetooth Connection Issues

- Ensure the device is paired before connecting
- Check that location permissions are granted (required for Bluetooth on Android)
- Verify the MAC address format is correct

### USB Permission Required

When connecting to USB printers, you may receive a `permission_required` event. Implement a proper permission request flow:

```typescript
// You'll need to implement USB permission handling
// This requires native Android code with PendingIntent
```

### Network Printer Not Found

- Verify the printer is on the same network
- Check the IP address and port (default: 9100)
- Ensure firewall rules allow the connection

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
