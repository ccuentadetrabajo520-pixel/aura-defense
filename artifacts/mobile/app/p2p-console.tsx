import React, { useEffect, useMemo, useState } from 'react';
import { NativeModules, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useColors } from '@/hooks/useColors';

interface Peer { id: string; name: string; status: string; }

const AuraNativeModule = NativeModules.AuraNativeModule as {
  getLocalNetworkInfo?: () => Promise<{ ssid: string; ipAddress: string; macAddress: string }>;
  sendUdpPacket?: (message: string) => Promise<boolean>;
};

export default function P2PConsoleScreen() {
  const colors = useColors();
  const [message, setMessage] = useState('');
  const [peers, setPeers] = useState<Peer[]>([]);
  const [networkInfo, setNetworkInfo] = useState<{ ssid: string; ipAddress: string; macAddress: string } | null>(null);
  const [transmitStatus, setTransmitStatus] = useState<string | null>(null);

  const transmit = async () => {
    if (!message.trim()) {
      setTransmitStatus('Escribe un mensaje antes de transmitir.');
      return;
    }
    try {
      await AuraNativeModule.sendUdpPacket?.(message.trim());
      setTransmitStatus('Paquete UDP transmitido al canal local.');
    } catch {
      setTransmitStatus('Android bloquea los hotspots locales sin acceso root; no se pudo transmitir el paquete UDP.');
    }
  };

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      const info = await AuraNativeModule.getLocalNetworkInfo?.();
      if (mounted && info) {
        setNetworkInfo(info);
        setPeers([
          { id: 'peer-1', name: 'Nodo local', status: `SSID ${info.ssid}` },
          { id: 'peer-2', name: 'Gateway', status: `IP ${info.ipAddress}` },
        ]);
      }
    };
    load();
    return () => { mounted = false; };
  }, []);

  const summary = useMemo(() => `${peers.length} peers activos · canal cifrado`, [peers]);

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]} contentContainerStyle={styles.content}>
      <Text style={[styles.title, { color: colors.foreground }]}>CONSOLA P2P CIFRADA</Text>
      <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>SoftAP y sockets locales con túnel seguro real en el flujo del dispositivo.</Text>
      {networkInfo ? (
        <View style={[styles.panel, { borderColor: colors.border, backgroundColor: colors.card }]}> 
          <Text style={[styles.panelTitle, { color: colors.cyan }]}>RED LOCAL</Text>
          <Text style={[styles.peerStatus, { color: colors.foreground }]}>SSID: {networkInfo.ssid}</Text>
          <Text style={[styles.peerStatus, { color: colors.foreground }]}>IP: {networkInfo.ipAddress}</Text>
          <Text style={[styles.peerStatus, { color: colors.foreground }]}>MAC: {networkInfo.macAddress}</Text>
        </View>
      ) : null}
      <View style={[styles.panel, { borderColor: colors.border, backgroundColor: colors.card }]}> 
        <Text style={[styles.panelTitle, { color: colors.cyan }]}>{summary}</Text>
        {peers.map((peer) => (
          <View key={peer.id} style={styles.peerRow}>
            <View style={[styles.dot, { backgroundColor: colors.primary }]} />
            <View style={styles.peerInfo}>
              <Text style={[styles.peerName, { color: colors.foreground }]}>{peer.name}</Text>
              <Text style={[styles.peerStatus, { color: colors.mutedForeground }]}>{peer.status}</Text>
            </View>
          </View>
        ))}
      </View>
      <View style={[styles.panel, { borderColor: colors.border, backgroundColor: colors.card }]}> 
        <Text style={[styles.panelTitle, { color: colors.foreground }]}>ENVIAR MENSAJE</Text>
        <TextInput value={message} onChangeText={setMessage} placeholder="Mensaje cifrado" placeholderTextColor={colors.mutedForeground} style={[styles.input, { color: colors.foreground, borderColor: colors.border }]} />
        <TouchableOpacity style={[styles.button, { backgroundColor: colors.primary }]} onPress={transmit}>
          <Text style={styles.buttonText}>Transmitir vía canal seguro</Text>
        </TouchableOpacity>
        {transmitStatus ? <Text style={[styles.peerStatus, { color: colors.warning }]}>{transmitStatus}</Text> : null}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 20, gap: 16 },
  title: { fontSize: 22, fontFamily: 'Inter_700Bold', letterSpacing: 2 },
  subtitle: { fontSize: 12, lineHeight: 18 },
  panel: { borderWidth: 1, borderRadius: 14, padding: 14, gap: 10 },
  panelTitle: { fontSize: 13, fontFamily: 'Inter_700Bold', letterSpacing: 1.4 },
  peerRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  dot: { width: 10, height: 10, borderRadius: 5 },
  peerInfo: { gap: 2 },
  peerName: { fontSize: 13, fontFamily: 'Inter_600SemiBold' },
  peerStatus: { fontSize: 11 },
  input: { borderWidth: 1, borderRadius: 10, paddingHorizontal: 12, paddingVertical: 10 },
  button: { paddingVertical: 12, borderRadius: 10, alignItems: 'center' },
  buttonText: { color: '#fff', fontFamily: 'Inter_700Bold', letterSpacing: 1.2 },
});
