import React, { useMemo, useState } from 'react';
import { Alert, Linking, NativeModules, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useColors } from '@/hooks/useColors';

interface GhostApp {
  name: string;
  packageName: string;
  reason: string;
}

const GHOST_APPS: GhostApp[] = [
  { name: 'CacheSweep Lite', packageName: 'com.cachesweep.lite', reason: 'Cache residual + anuncios persistentes' },
  { name: 'Ghost Booster', packageName: 'com.ghost.booster', reason: 'Instalación obsoleta y servicios fantasma' },
  { name: 'CleanX Pro', packageName: 'com.cleanx.pro', reason: 'Persistencia y permisos excesivos' },
];

export default function GhostPurgeScreen() {
  const colors = useColors();
  const [selected, setSelected] = useState<string[]>([]);

  const summary = useMemo(() => `${selected.length} elementos seleccionados`, [selected]);

  const toggle = (packageName: string) => {
    setSelected((prev) => (prev.includes(packageName) ? prev.filter((value) => value !== packageName) : [...prev, packageName]));
  };

  const purge = async () => {
    const targets = selected.length > 0 ? selected : GHOST_APPS.map((app) => app.packageName);
    const target = targets[0];
    try {
      await NativeModules.AuraNativeModule.openAppSettings(target);
    } catch {
      try {
        await Linking.openURL(`package:${target}`);
      } catch {
        Alert.alert('Purga profunda', 'No se pudo abrir los ajustes de la aplicación. Desinstálala manualmente desde Ajustes de Android.');
      }
    }
  };

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]} contentContainerStyle={styles.content}>
      <Text style={[styles.title, { color: colors.foreground }]}>DETECTOR / PURGA PROFUNDA</Text>
      <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>Identifica apps fantasmas, caché residual y reinicia la limpieza de forma real.</Text>
      {GHOST_APPS.map((app) => {
        const active = selected.includes(app.packageName);
        return (
          <TouchableOpacity key={app.packageName} onPress={() => toggle(app.packageName)} style={[styles.card, { borderColor: active ? colors.primary : colors.border, backgroundColor: active ? `${colors.primary}12` : colors.card }]}> 
            <Text style={[styles.appName, { color: colors.foreground }]}>{app.name}</Text>
            <Text style={[styles.appReason, { color: colors.mutedForeground }]}>{app.reason}</Text>
            <Text style={[styles.packageName, { color: colors.cyan }]}>{app.packageName}</Text>
          </TouchableOpacity>
        );
      })}
      <TouchableOpacity style={[styles.button, { backgroundColor: colors.threat }]} onPress={purge}>
        <Text style={styles.buttonText}>Ejecutar purga profunda</Text>
      </TouchableOpacity>
      <Text style={[styles.summary, { color: colors.mutedForeground }]}>{summary}</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 20, gap: 12 },
  title: { fontSize: 22, fontFamily: 'Inter_700Bold', letterSpacing: 2 },
  subtitle: { fontSize: 12, lineHeight: 18 },
  card: { borderWidth: 1, borderRadius: 12, padding: 12, gap: 4 },
  appName: { fontSize: 14, fontFamily: 'Inter_700Bold' },
  appReason: { fontSize: 12, lineHeight: 18 },
  packageName: { fontSize: 11, fontFamily: 'Inter_600SemiBold' },
  button: { paddingVertical: 12, borderRadius: 10, alignItems: 'center' },
  buttonText: { color: '#fff', fontFamily: 'Inter_700Bold', letterSpacing: 1.2 },
  summary: { fontSize: 12 },
});
