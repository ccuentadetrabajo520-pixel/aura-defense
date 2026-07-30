import React, { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useColors } from '@/hooks/useColors';

interface NodeItem {
  id: string;
  label: string;
  color: string;
  status: string;
}

const NETWORKS = [
  { id: 'net-1', label: 'SEGMENTO RED-01', color: '#4dd0e1', nodes: [{ id: 'n1', label: 'Nodo A', color: '#4dd0e1', status: 'VIVO' }, { id: 'n2', label: 'Nodo B', color: '#4dd0e1', status: 'MONITOREADO' }] },
  { id: 'net-2', label: 'SEGMENTO RED-02', color: '#8e6cff', nodes: [{ id: 'n3', label: 'Nodo C', color: '#8e6cff', status: 'VIVO' }, { id: 'n4', label: 'Nodo D', color: '#8e6cff', status: 'RIESGO' }] },
  { id: 'net-3', label: 'SEGMENTO RED-03', color: '#ff7b54', nodes: [{ id: 'n5', label: 'Nodo E', color: '#ff7b54', status: 'AISLADO' }] },
];

export default function RadarScreen() {
  const colors = useColors();
  const [selectedNetwork, setSelectedNetwork] = useState(NETWORKS[0].id);

  const selected = useMemo(() => NETWORKS.find((network) => network.id === selectedNetwork) ?? NETWORKS[0], [selectedNetwork]);

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]} contentContainerStyle={styles.content}>
      <Text style={[styles.title, { color: colors.foreground }]}>RADAR TÁCTICO ACTIVO</Text>
      <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>Mapa de redes y nodos P2P por colores con selección activa.</Text>
      <View style={styles.networkList}>
        {NETWORKS.map((network) => (
          <TouchableOpacity key={network.id} style={[styles.badge, { borderColor: selectedNetwork === network.id ? network.color : colors.border, backgroundColor: selectedNetwork === network.id ? `${network.color}16` : 'transparent' }]} onPress={() => setSelectedNetwork(network.id)}>
            <Text style={[styles.badgeText, { color: selectedNetwork === network.id ? network.color : colors.foreground }]}>{network.label}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <View style={[styles.panel, { borderColor: colors.border, backgroundColor: colors.card }]}> 
        <Text style={[styles.panelTitle, { color: selected.color }]}>RED ACTIVA: {selected.label}</Text>
        {selected.nodes.map((node) => (
          <View key={node.id} style={styles.nodeRow}>
            <View style={[styles.nodeDot, { backgroundColor: node.color }]} />
            <View style={styles.nodeInfo}>
              <Text style={[styles.nodeLabel, { color: colors.foreground }]}>{node.label}</Text>
              <Text style={[styles.nodeStatus, { color: colors.mutedForeground }]}>{node.status}</Text>
            </View>
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 20, gap: 16 },
  title: { fontSize: 22, fontFamily: 'Inter_700Bold', letterSpacing: 2 },
  subtitle: { fontSize: 12, lineHeight: 18 },
  networkList: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  badge: { borderWidth: 1, borderRadius: 999, paddingHorizontal: 12, paddingVertical: 8 },
  badgeText: { fontSize: 10, fontFamily: 'Inter_700Bold', letterSpacing: 1.4 },
  panel: { borderWidth: 1, borderRadius: 14, padding: 14, gap: 10 },
  panelTitle: { fontSize: 14, fontFamily: 'Inter_700Bold', letterSpacing: 1.4 },
  nodeRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  nodeDot: { width: 10, height: 10, borderRadius: 5 },
  nodeInfo: { gap: 2 },
  nodeLabel: { fontSize: 13, fontFamily: 'Inter_600SemiBold' },
  nodeStatus: { fontSize: 11 },
});
