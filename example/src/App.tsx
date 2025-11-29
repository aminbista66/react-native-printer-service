import { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Platform,
  PermissionsAndroid,
} from 'react-native';
import PrinterService, { type PrinterEvent } from 'react-native-printer-service';
import { Buffer } from 'buffer';

export default function App() {
  const [tcpHost, setTcpHost] = useState('10.0.2.2');
  const [tcpPort, setTcpPort] = useState('9100');
  const [btMac, setBtMac] = useState('00:11:22:33:44:55');
  const [connected, setConnected] = useState(false);
  const [logs, setLogs] = useState<string[]>([]);

  useEffect(() => {
    requestPermissions();

    const subscription = PrinterService.addListener((event: PrinterEvent) => {
      console.log('Printer Event:', event);
      addLog(`Event: ${event.type} - ${event.data || ''}`);
      
      if (event.type === 'connected') {
        setConnected(true);
      } else if (event.type === 'disconnected') {
        setConnected(false);
      }
    });

    return () => {
      subscription.remove();
      PrinterService.removeAllListeners();
    };
  }, []);

  const requestPermissions = async () => {
    if (Platform.OS === 'android' && Platform.Version >= 31) {
      try {
        await PermissionsAndroid.requestMultiple([
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
          PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        ]);
      } catch (err) {
        console.warn(err);
      }
    }
  };

  const addLog = (message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs((prev) => [`[${timestamp}] ${message}`, ...prev].slice(0, 50));
  };

  const handleConnectTCP = async () => {
    try {
      addLog(`Connecting to TCP ${tcpHost}:${tcpPort}...`);
      const result = await PrinterService.connectTCP(tcpHost, parseInt(tcpPort));
      addLog(`TCP connect result: ${result}`);
    } catch (error) {
      addLog(`TCP connect error: ${error}`);
    }
  };

  const handleConnectBT = async () => {
    try {
      addLog(`Connecting to Bluetooth ${btMac}...`);
      const result = await PrinterService.connectBT(btMac);
      addLog(`Bluetooth connect result: ${result}`);
    } catch (error) {
      addLog(`Bluetooth connect error: ${error}`);
    }
  };

  const handleConnectUSB = async () => {
    try {
      addLog('Connecting to USB...');
      const result = await PrinterService.connectUSB();
      addLog(`USB connect result: ${result}`);
    } catch (error) {
      addLog(`USB connect error: ${error}`);
    }
  };

  const handleDisconnect = async () => {
    try {
      addLog('Disconnecting...');
      const result = await PrinterService.disconnect();
      addLog(`Disconnect result: ${result}`);
    } catch (error) {
      addLog(`Disconnect error: ${error}`);
    }
  };

  const handlePrintTest = async () => {
    try {
      const testContent = [
        '\x1B\x40',           // Initialize
        '\x1B\x61\x01',       // Center align
        'PRINTER TEST\n',
        '================\n',
        '\x1B\x61\x00',       // Left align
        'Item 1      $10.00\n',
        'Item 2      $15.00\n',
        '================\n',
        'Total:      $25.00\n',
        '\n\n\n',
      ].join('');

      const base64Data = Buffer.from(testContent, 'utf8').toString('base64');
      addLog('Sending print data...');
      const result = await PrinterService.send(base64Data);
      addLog(`Print result: ${result}`);
    } catch (error) {
      addLog(`Print error: ${error}`);
    }
  };

  const handlePollStatus = async () => {
    try {
      addLog('Polling status...');
      const status = await PrinterService.pollStatus();
      addLog(`Status: ${status || 'null'}`);
    } catch (error) {
      addLog(`Status error: ${error}`);
    }
  };

  const clearLogs = () => {
    setLogs([]);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Printer Service Demo</Text>
      <Text style={styles.status}>
        Status: {connected ? '🟢 Connected' : '🔴 Disconnected'}
      </Text>

      <ScrollView style={styles.scrollView}>
        {/* TCP Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>TCP Connection</Text>
          <TextInput
            style={styles.input}
            placeholder="IP Address"
            value={tcpHost}
            onChangeText={setTcpHost}
          />
          <TextInput
            style={styles.input}
            placeholder="Port"
            value={tcpPort}
            onChangeText={setTcpPort}
            keyboardType="numeric"
          />
          <TouchableOpacity style={styles.button} onPress={handleConnectTCP}>
            <Text style={styles.buttonText}>Connect TCP</Text>
          </TouchableOpacity>
        </View>

        {/* Bluetooth Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Bluetooth Connection</Text>
          <TextInput
            style={styles.input}
            placeholder="MAC Address (00:11:22:33:44:55)"
            value={btMac}
            onChangeText={setBtMac}
          />
          <TouchableOpacity style={styles.button} onPress={handleConnectBT}>
            <Text style={styles.buttonText}>Connect Bluetooth</Text>
          </TouchableOpacity>
        </View>

        {/* USB Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>USB Connection</Text>
          <TouchableOpacity style={styles.button} onPress={handleConnectUSB}>
            <Text style={styles.buttonText}>Connect USB</Text>
          </TouchableOpacity>
        </View>

        {/* Actions Section */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Actions</Text>
          <TouchableOpacity
            style={[styles.button, styles.buttonSuccess]}
            onPress={handlePrintTest}
            // disabled={!connected}
          >
            <Text style={styles.buttonText}>Print Test Receipt</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.button}
            onPress={handlePollStatus}
            // disabled={!connected}
          >
            <Text style={styles.buttonText}>Poll Status</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.buttonDanger]}
            onPress={handleDisconnect}
            // disabled={!connected}
          >
            <Text style={styles.buttonText}>Disconnect</Text>
          </TouchableOpacity>
        </View>

        {/* Logs Section */}
        <View style={styles.section}>
          <View style={styles.logHeader}>
            <Text style={styles.sectionTitle}>Logs</Text>
            <TouchableOpacity onPress={clearLogs}>
              <Text style={styles.clearButton}>Clear</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.logContainer}>
            {logs.length === 0 ? (
              <Text style={styles.logEmpty}>No logs yet</Text>
            ) : (
              logs.map((log, index) => (
                <Text key={index} style={styles.logText}>
                  {log}
                </Text>
              ))
            )}
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    paddingTop: 50,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 10,
    color: '#333',
  },
  status: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 20,
    color: '#666',
  },
  scrollView: {
    flex: 1,
    paddingHorizontal: 20,
  },
  section: {
    backgroundColor: 'white',
    borderRadius: 10,
    padding: 15,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 10,
    color: '#333',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 10,
    fontSize: 16,
  },
  button: {
    backgroundColor: '#007AFF',
    borderRadius: 8,
    padding: 15,
    alignItems: 'center',
    marginTop: 5,
  },
  buttonSuccess: {
    backgroundColor: '#34C759',
  },
  buttonDanger: {
    backgroundColor: '#FF3B30',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  logHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  clearButton: {
    color: '#007AFF',
    fontSize: 14,
  },
  logContainer: {
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
    padding: 10,
    maxHeight: 300,
  },
  logEmpty: {
    color: '#999',
    textAlign: 'center',
    fontStyle: 'italic',
  },
  logText: {
    fontSize: 12,
    fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace',
    color: '#333',
    marginBottom: 5,
  },
});
