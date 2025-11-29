import { NativeEventEmitter, NativeModules } from 'react-native';
import PrinterService from './NativePrinterService';

export interface PrinterEvent {
  type: 'connected' | 'disconnected' | 'status' | 'printed' | 'error' | 'permission_required';
  data?: string;
}

const eventEmitter = new NativeEventEmitter(NativeModules.PrinterService);

export default {
  /**
   * Add a listener for printer events
   * @param callback Function to handle printer events
   * @returns Subscription object with remove() method
   */
  addListener: (callback: (event: PrinterEvent) => void) => {
    return eventEmitter.addListener('PrinterEvent', callback as any);
  },

  /**
   * Remove all printer event listeners
   */
  removeAllListeners: () => {
    eventEmitter.removeAllListeners('PrinterEvent');
  },

  /**
   * Connect to a printer via TCP
   * @param host IP address or hostname
   * @param port Port number (default: 9100)
   * @returns Promise<boolean> - true if connected successfully
   */
  connectTCP: (host: string, port: number = 9100): Promise<boolean> => {
    return PrinterService.connectTCP(host, port);
  },

  /**
   * Connect to a printer via Bluetooth
   * @param mac Bluetooth MAC address (e.g., "00:11:22:33:44:55")
   * @returns Promise<boolean> - true if connected successfully
   */
  connectBT: (mac: string): Promise<boolean> => {
    return PrinterService.connectBT(mac);
  },

  /**
   * Connect to a printer via USB
   * Note: Connects to the first available USB device
   * @returns Promise<boolean> - true if connected successfully
   */
  connectUSB: (): Promise<boolean> => {
    return PrinterService.connectUSB();
  },

  /**
   * Disconnect from the current printer
   * @returns Promise<boolean> - true if disconnected successfully
   */
  disconnect: (): Promise<boolean> => {
    return PrinterService.disconnect();
  },

  /**
   * Send data to the printer
   * @param base64 Base64-encoded data to send (e.g., ESC/POS commands)
   * @returns Promise<boolean> - true if sent successfully
   */
  send: (base64: string): Promise<boolean> => {
    return PrinterService.send(base64);
  },

  /**
   * Poll the printer status
   * @returns Promise<string | null> - Hex string of status bytes or null
   */
  pollStatus: (): Promise<string | null> => {
    return PrinterService.pollStatus();
  },

  /**
   * Example multiply method (for testing)
   */
  multiply: (a: number, b: number): number => {
    return PrinterService.multiply(a, b);
  },
};

