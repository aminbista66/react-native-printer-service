import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  multiply(a: number, b: number): number;
  connectTCP(host: string, port: number): Promise<boolean>;
  connectBT(mac: string): Promise<boolean>;
  connectUSB(): Promise<boolean>;
  disconnect(): Promise<boolean>;
  send(base64: string): Promise<boolean>;
  pollStatus(): Promise<string | null>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('PrinterService');
