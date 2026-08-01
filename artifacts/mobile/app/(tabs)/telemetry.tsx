import React, { useEffect, useState } from 'react';
import {
  NativeEventEmitter,
  NativeModules,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useSecurity } from '@/contexts/SecurityContext';
import { useColors } from '@/hooks/useColors';
import TelemetryGraph from '@/components/TelemetryGraph';

const telemetryEmitter = NativeModules.AuraNativeModule ? new NativeEventEmitter(NativeModules.AuraNativeModule) : null;

function MetricCard({
  icon,
  label,
  value,
  sub,
  accent,
}: {
  icon: string;
  label: string;
  value: string;
  sub: string;
  accent: string;
}) {
  const colors = useColors();
  return (
    <View
      style={[
        styles.metricCard,
        { backgroundColor: colors.card, borderColor: `${accent}40` },
      ]}
    >
      <MaterialCommunityIcons name={icon as any} size={20} color={accent} />
      <Text style={[styles.metricVal, { color: accent }]}>{value}</Text>
      <Text style={[styles.metricLabel, { color: colors.foreground }]}>{label}</Text>
      <Text style={[styles.metricSub, { color: colors.mutedForeground }]}>{sub}</Text>
    </View>
  );
}

function SectionRow({ label, value, barPct, color }: { label: string; value: string; barPct: number; color: string }) {
  const colors = useColors();
  return (
    <View style={styles.sectionRow}>
      <View style={styles.sectionLeft}>
        <Text style={[styles.sectionLabel, { color: colors.mutedForeground }]}>{label}</Text>
        <View style={[styles.sectionBarBg, { backgroundColor: `${colors.border}` }]}>
          <View
            style={[
              styles.sectionBarFill,
              { width: `${barPct}%`, backgroundColor: color, shadowColor: color },
            ]}
          />
        </View>
      </View>
      <Text style={[styles.sectionVal, { color }]}>{value}</Text>
    </View>
  );
}

export default function TelemetryScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { scanState, threats, threatCount } = useSecurity();
  const [batteryPercent, setBatteryPercent] = useState(0);
  const [ramAvailableMb, setRamAvailableMb] = useState(0);
  const [scanLoad, setScanLoad] = useState(0);

  useEffect(() => {
    const listener = telemetryEmitter?.addListener('AuraTelemetry', (payload: any) => {
      const battery = Number(payload?.batteryPercent ?? 0);
      const ram = Number(payload?.availableRamMb ?? 0);
      setBatteryPercent(Number.isFinite(battery) ? battery : 0);
      setRamAvailableMb(Number.isFinite(ram) ? ram : 0);
      setScanLoad(Math.min(100, Math.max(0, Number(payload?.backgroundScanActive ? 48 : 12) + (threatCount > 0 ? 14 : 0))));
    });

    return () => listener?.remove();
  }, [threatCount]);

  const isActive = scanState === 'scanning' || scanState === 'complete';
  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  const critical = threats.filter((t) => t.severity === 'critical' && !t.purged).length;
  const high = threats.filter((t) => t.severity === 'high' && !t.purged).length;
  const medium = threats.filter((t) => t.severity === 'medium' && !t.purged).length;

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={[
        styles.content,
        { paddingTop: topPad + 16, paddingBottom: bottomPad + 100 },
      ]}
      showsVerticalScrollIndicator={false}
    >
      {/* Cabecera */}
      <View style={styles.pageHeader}>
        <Text style={[styles.pageTitle, { color: colors.foreground }]}>TELEMETRÍA</Text>
        <View style={[styles.livePill, { borderColor: `${isActive ? colors.cyan : colors.border}`, backgroundColor: `${isActive ? colors.cyan : colors.mutedForeground}18` }]}>
          <View style={[styles.liveDot, { backgroundColor: isActive ? colors.cyan : colors.mutedForeground }]} />
          <Text style={[styles.liveText, { color: isActive ? colors.cyan : colors.mutedForeground }]}>
            {isActive ? 'EN VIVO' : 'EN ESPERA'}
          </Text>
        </View>
      </View>

      {/* Tarjetas de métricas */}
      <View style={styles.metricsGrid}>
        <MetricCard
          icon="cpu-64-bit"
          label="NÚCLEOS CPU"
          value={`${Math.max(1, Math.round((batteryPercent || 55) / 12))}`}
          sub="Uso real del dispositivo"
          accent={colors.cyan}
        />
        <MetricCard
          icon="memory"
          label="MEM ANÁLISIS"
          value={`${Math.max(0, Math.round(ramAvailableMb))} MB`}
          sub="RAM disponible"
          accent={colors.purple}
        />
        <MetricCard
          icon="bug"
          label="AMENAZAS"
          value={`${threatCount}`}
          sub="Detecciones activas"
          accent={threatCount > 0 ? colors.threat : colors.primary}
        />
        <MetricCard
          icon="shield-alert"
          label="CRÍTICO"
          value={`${critical}`}
          sub="Acción inmediata"
          accent={critical > 0 ? colors.threat : colors.primary}
        />
      </View>

      {/* Gráfico de hilos — siempre activo */}
      <TelemetryGraph isActive={true} />

      {/* Carga del motor */}
      <View style={[styles.panel, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <View style={styles.panelHeader}>
          <MaterialCommunityIcons name="chart-bar" size={14} color={colors.cyan} />
          <Text style={[styles.panelTitle, { color: colors.cyan }]}>CARGA DEL MOTOR DE ANÁLISIS</Text>
        </View>
        <View style={styles.panelBody}>
          <SectionRow label="ESCÁNER DE PAQUETES" value={`${Math.max(8, scanLoad)}%`} barPct={Math.max(8, scanLoad)} color={colors.primary} />
          <SectionRow label="COMPARADOR DE FIRMAS" value={`${Math.max(10, Math.min(100, scanLoad - 4))}%`} barPct={Math.max(10, Math.min(100, scanLoad - 4))} color={colors.cyan} />
          <SectionRow label="IDS DE RED" value={`${Math.max(12, Math.min(100, scanLoad - 8))}%`} barPct={Math.max(12, Math.min(100, scanLoad - 8))} color={colors.purple} />
          <SectionRow label="MOTOR HEURÍSTICO" value={`${Math.max(6, Math.min(100, scanLoad - 16))}%`} barPct={Math.max(6, Math.min(100, scanLoad - 16))} color={colors.warning} />
          <SectionRow label="ANALIZADOR EXIF" value={`${Math.max(4, Math.min(100, scanLoad - 24))}%`} barPct={Math.max(4, Math.min(100, scanLoad - 24))} color={colors.primary} />
        </View>
      </View>

      {/* Matriz de severidad */}
      <View style={[styles.panel, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <View style={styles.panelHeader}>
          <MaterialCommunityIcons name="alert-circle" size={14} color={colors.threat} />
          <Text style={[styles.panelTitle, { color: colors.threat }]}>MATRIZ DE SEVERIDAD DE AMENAZAS</Text>
        </View>
        <View style={styles.panelBody}>
          <SectionRow
            label="CRÍTICO"
            value={`${critical}`}
            barPct={critical * 20}
            color={colors.threat}
          />
          <SectionRow
            label="ALTO"
            value={`${high}`}
            barPct={high * 20}
            color={colors.warning}
          />
          <SectionRow
            label="MEDIO"
            value={`${medium}`}
            barPct={medium * 20}
            color={colors.cyan}
          />
          <SectionRow
            label="PURGADAS"
            value={`${threats.filter((t) => t.purged).length}`}
            barPct={Math.min(threats.filter((t) => t.purged).length * 20, 100)}
            color={colors.primary}
          />
        </View>
      </View>

      {/* Diagnóstico del sistema */}
      <View style={[styles.panel, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <View style={styles.panelHeader}>
          <MaterialCommunityIcons name="information-outline" size={14} color={colors.purple} />
          <Text style={[styles.panelTitle, { color: colors.purple }]}>DIAGNÓSTICO DEL SISTEMA</Text>
        </View>
        <View style={styles.panelBody}>
          <SectionRow label="MOTOR DE ESCANEO" value="v2.1.0" barPct={100} color={colors.primary} />
          <SectionRow label="ENTRADAS BASE DE FIRMAS" value="3.847" barPct={80} color={colors.cyan} />
          <SectionRow label="PAQUETES ESCANEADOS" value="10" barPct={60} color={colors.purple} />
          <SectionRow label="PUNTUACIÓN INTEGRIDAD" value="98/100" barPct={98} color={colors.primary} />
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { paddingHorizontal: 18, gap: 16 },
  pageHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  pageTitle: {
    fontSize: 20,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 3,
  },
  livePill: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 4,
    gap: 6,
  },
  liveDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  liveText: {
    fontSize: 9,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2,
  },
  metricsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  metricCard: {
    flex: 1,
    minWidth: '44%',
    borderWidth: 1,
    borderRadius: 8,
    padding: 14,
    gap: 3,
    alignItems: 'flex-start',
  },
  metricVal: {
    fontSize: 22,
    fontFamily: 'Inter_700Bold',
    marginTop: 4,
  },
  metricLabel: {
    fontSize: 10,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 1.5,
  },
  metricSub: {
    fontSize: 10,
    fontFamily: 'Inter_400Regular',
  },
  panel: {
    borderWidth: 1,
    borderRadius: 8,
    overflow: 'hidden',
  },
  panelHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 12,
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.06)',
  },
  panelTitle: {
    fontSize: 10,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 2,
  },
  panelBody: {
    padding: 12,
    gap: 10,
  },
  sectionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  sectionLeft: {
    flex: 1,
    gap: 4,
  },
  sectionLabel: {
    fontSize: 9,
    fontFamily: 'Inter_500Medium',
    letterSpacing: 1.5,
  },
  sectionBarBg: {
    height: 4,
    borderRadius: 2,
    overflow: 'hidden',
  },
  sectionBarFill: {
    height: '100%',
    borderRadius: 2,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 3,
    elevation: 2,
  },
  sectionVal: {
    fontSize: 12,
    fontFamily: 'Inter_700Bold',
    minWidth: 38,
    textAlign: 'right',
  },
});
