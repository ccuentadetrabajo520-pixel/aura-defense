import React from 'react';
import {
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
  withSequence,
} from 'react-native-reanimated';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useSecurity } from '@/contexts/SecurityContext';
import { useColors } from '@/hooks/useColors';
import LogTerminal from '@/components/LogTerminal';

function NetStatusRow({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  const colors = useColors();
  return (
    <View style={[styles.netRow, { borderBottomColor: colors.border }]}>
      <Text style={[styles.netLabel, { color: colors.mutedForeground }]}>{label}</Text>
      <View style={styles.netRight}>
        <View style={[styles.netDot, { backgroundColor: ok ? colors.primary : colors.threat }]} />
        <Text style={[styles.netValue, { color: ok ? colors.primary : colors.threat }]}>{value}</Text>
      </View>
    </View>
  );
}

export default function ScannerScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { scanState, logs, startScan, networkStatus, rootDetected, debugDetected, threatCount } =
    useSecurity();

  const isScanning = scanState === 'scanning';
  const isComplete = scanState === 'complete';
  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  // Animación de pulso para el botón de escaneo
  const pulseScale = useSharedValue(1);
  const pulseOpacity = useSharedValue(1);
  // Animación de destello al iniciar escaneo
  const flashBorder = useSharedValue(0.5);

  React.useEffect(() => {
    if (isScanning) {
      pulseScale.value = withRepeat(
        withSequence(withTiming(1.04, { duration: 500 }), withTiming(1, { duration: 500 })),
        -1,
        false
      );
      pulseOpacity.value = withRepeat(withTiming(0.65, { duration: 500 }), -1, true);
      // Borde parpadeante rápido
      flashBorder.value = withRepeat(
        withSequence(withTiming(1, { duration: 200 }), withTiming(0.3, { duration: 200 })),
        -1,
        false
      );
    } else {
      pulseScale.value = withTiming(1, { duration: 200 });
      pulseOpacity.value = withTiming(1, { duration: 200 });
      flashBorder.value = withTiming(0.5, { duration: 300 });
    }
  }, [isScanning]);

  const btnStyle = useAnimatedStyle(() => ({
    transform: [{ scale: pulseScale.value }],
    opacity: pulseOpacity.value,
  }));

  const btnBorderStyle = useAnimatedStyle(() => ({
    borderWidth: 1.5 + flashBorder.value,
    shadowOpacity: flashBorder.value * 0.8,
  }));

  const handleScan = () => {
    if (isScanning) return;
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
    startScan();
  };

  const scanBtnColor = isScanning ? colors.warning : isComplete ? colors.cyan : colors.primary;
  const scanBtnLabel = isScanning ? 'ESCANEANDO...' : isComplete ? 'RE-ESCANEAR' : 'CAZA DE AMENAZAS';

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: topPad + 16, paddingBottom: bottomPad + 100 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        {/* Cabecera */}
        <View style={styles.pageHeader}>
          <Text style={[styles.pageTitle, { color: colors.foreground }]}>CAZA DE AMENAZAS</Text>
          <View style={[styles.countBadge, { backgroundColor: `${threatCount > 0 ? colors.threat : colors.primary}20`, borderColor: `${threatCount > 0 ? colors.threat : colors.primary}50` }]}>
            <Text style={[styles.countText, { color: threatCount > 0 ? colors.threat : colors.primary }]}>
              {threatCount} AMENAZAS
            </Text>
          </View>
        </View>

        {/* BOTÓN DE ESCANEO */}
        <Animated.View style={btnStyle}>
          <Animated.View
            style={[
              styles.scanBtn,
              {
                borderColor: scanBtnColor,
                backgroundColor: `${scanBtnColor}15`,
                shadowColor: scanBtnColor,
              },
              btnBorderStyle,
            ]}
          >
            <TouchableOpacity
              style={styles.scanBtnInner}
              onPress={handleScan}
              disabled={isScanning}
              activeOpacity={0.75}
            >
              <MaterialCommunityIcons
                name={isScanning ? 'radar' : isComplete ? 'reload' : 'shield-search'}
                size={34}
                color={scanBtnColor}
              />
              <Text style={[styles.scanBtnText, { color: scanBtnColor }]}>{scanBtnLabel}</Text>
              {isScanning && (
                <Text style={[styles.scanSub, { color: `${scanBtnColor}90` }]}>
                  Analizando paquetes · verificando red · escaneando EXIF
                </Text>
              )}
              {!isScanning && !isComplete && (
                <Text style={[styles.scanSub, { color: `${scanBtnColor}60` }]}>
                  Toca para iniciar análisis completo del sistema
                </Text>
              )}
            </TouchableOpacity>
          </Animated.View>
        </Animated.View>

        {/* Panel de análisis de red */}
        <View style={[styles.panel, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <View style={styles.panelHeader}>
            <MaterialCommunityIcons name="wifi-lock" size={14} color={colors.cyan} />
            <Text style={[styles.panelTitle, { color: colors.cyan }]}>ANÁLISIS IDS DE RED</Text>
          </View>
          {networkStatus ? (
            <>
              <NetStatusRow label="SSID" value={networkStatus.ssid} ok={true} />
              <NetStatusRow label="PUERTA DE ENLACE" value={networkStatus.gateway} ok={true} />
              <NetStatusRow label="DNS" value={networkStatus.dns} ok={true} />
              <NetStatusRow label="SONDA MitM" value={networkStatus.mitm ? 'ATAQUE DETECTADO' : 'LIMPIO'} ok={!networkStatus.mitm} />
              <NetStatusRow label="CIFRADO" value={networkStatus.encrypted ? 'WPA3 VÁLIDO' : 'DÉBIL'} ok={networkStatus.encrypted} />
            </>
          ) : (
            <Text style={[styles.noPanelText, { color: colors.mutedForeground }]}>
              Ejecuta un escaneo para analizar la seguridad de red
            </Text>
          )}
        </View>

        {/* Panel de integridad del sistema */}
        <View style={[styles.panel, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <View style={styles.panelHeader}>
            <MaterialCommunityIcons name="shield-lock" size={14} color={colors.purple} />
            <Text style={[styles.panelTitle, { color: colors.purple }]}>NÚCLEO DE INTEGRIDAD DEL SISTEMA</Text>
          </View>
          {scanState !== 'idle' ? (
            <>
              <NetStatusRow label="BINARIOS ROOT" value={rootDetected ? 'COMPROMETIDO' : 'LIMPIO'} ok={!rootDetected} />
              <NetStatusRow label="CLAVES ROM" value="VERSIÓN OFICIAL" ok={true} />
              <NetStatusRow label="MODO DEBUG" value={debugDetected ? 'ACTIVO — RIESGO' : 'INACTIVO'} ok={!debugDetected} />
              <NetStatusRow label="ANTI-MANIPULACIÓN" value="ACTIVO" ok={true} />
            </>
          ) : (
            <Text style={[styles.noPanelText, { color: colors.mutedForeground }]}>
              Ejecuta un escaneo para auditar la integridad del sistema
            </Text>
          )}
        </View>

        {/* Terminal de log */}
        <View>
          <View style={styles.termHeader}>
            <MaterialCommunityIcons name="console" size={14} color={colors.cyan} />
            <Text style={[styles.panelTitle, { color: colors.cyan }]}>CONSOLA DE AUDITORÍA</Text>
            <Text style={[styles.logCount, { color: colors.mutedForeground }]}>{logs.length} entradas</Text>
          </View>
          <LogTerminal logs={logs} maxHeight={340} />
        </View>
      </ScrollView>
    </View>
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
  countBadge: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  countText: {
    fontSize: 10,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1.5,
  },
  scanBtn: {
    borderRadius: 12,
    shadowOffset: { width: 0, height: 0 },
    shadowRadius: 20,
    elevation: 6,
    overflow: 'hidden',
  },
  scanBtnInner: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 26,
    gap: 10,
  },
  scanBtnText: {
    fontSize: 18,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 4,
  },
  scanSub: {
    fontSize: 11,
    fontFamily: 'Inter_400Regular',
    letterSpacing: 0.5,
    textAlign: 'center',
    paddingHorizontal: 20,
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
    flex: 1,
  },
  netRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 9,
    borderBottomWidth: 1,
  },
  netLabel: {
    fontSize: 10,
    fontFamily: 'Inter_500Medium',
    letterSpacing: 1.5,
  },
  netRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  netDot: {
    width: 5,
    height: 5,
    borderRadius: 2.5,
  },
  netValue: {
    fontSize: 11,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 0.5,
  },
  noPanelText: {
    padding: 14,
    fontSize: 12,
    fontFamily: 'Inter_400Regular',
    fontStyle: 'italic',
  },
  termHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
  },
  logCount: {
    fontSize: 10,
    fontFamily: 'Inter_400Regular',
    marginLeft: 'auto',
  },
});
