import React, { useEffect, useState } from 'react';
import {
  Platform,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';
import { MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import * as SecureStore from 'expo-secure-store';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useSecurity } from '@/contexts/SecurityContext';
import { useColors } from '@/hooks/useColors';
import RadarHUD from '@/components/RadarHUD';
import { useRouter } from 'expo-router';

function StatusIndicator({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  const colors = useColors();
  return (
    <View style={[styles.statCell, { borderColor: `${colors.border}` }]}>
      <View style={[styles.statDot, { backgroundColor: ok ? colors.primary : colors.threat }]} />
      <Text style={[styles.statLabel, { color: colors.mutedForeground }]}>{label}</Text>
      <Text style={[styles.statVal, { color: ok ? colors.primary : colors.threat }]} numberOfLines={1}>
        {value}
      </Text>
    </View>
  );
}

export default function ShieldScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { firewallEnabled, toggleFirewall, scanState, threats, threatCount, criticalCount, networkStatus, rootDetected } =
    useSecurity();
  const [profileName, setProfileName] = useState('Operador');

  const isScanning = scanState === 'scanning';
  const glowOpacity = useSharedValue(0.3);
  const fwGlow = useSharedValue(0);

  useEffect(() => {
    glowOpacity.value = withRepeat(withTiming(0.8, { duration: 1400 }), -1, true);
  }, []);

  useEffect(() => {
    fwGlow.value = withTiming(firewallEnabled ? 1 : 0, { duration: 400 });
  }, [firewallEnabled]);

  useEffect(() => {
    let active = true;
    SecureStore.getItemAsync('aura-user-name').then((value) => {
      if (active && value && value.trim()) {
        setProfileName(value.trim());
      }
    });
    return () => {
      active = false;
    };
  }, []);

  const glowStyle = useAnimatedStyle(() => ({
    opacity: glowOpacity.value,
    shadowOpacity: glowOpacity.value,
  }));

  const fwGlowStyle = useAnimatedStyle(() => ({
    shadowOpacity: fwGlow.value * 0.8,
    opacity: 0.5 + fwGlow.value * 0.5,
  }));

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  const handleToggle = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    toggleFirewall();
  };

  const overallOk = threatCount === 0 && !rootDetected;
  const statusColor = isScanning ? colors.warning : overallOk ? colors.primary : colors.threat;
  const statusText = isScanning
    ? 'ESCANEANDO...'
    : overallOk
    ? 'SEGURO'
    : `${threatCount} AMENAZA${threatCount !== 1 ? 'S' : ''}`;

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={[styles.content, { paddingTop: topPad + 16, paddingBottom: bottomPad + 100 }]}
      showsVerticalScrollIndicator={false}
    >
      {/* Cabecera */}
      <View style={styles.headerRow}>
        <View>
          <Text style={[styles.appTitle, { color: colors.cyan }]}>AURA</Text>
          <Text style={[styles.appSubtitle, { color: colors.foreground }]}>DEFENSA</Text>
        </View>
        <Animated.View
          style={[
            styles.statusBadge,
            { borderColor: statusColor, backgroundColor: `${statusColor}18` },
            isScanning && glowStyle,
          ]}
        >
          <Animated.View style={[styles.statusDot, { backgroundColor: statusColor }, isScanning && glowStyle]} />
          <Text style={[styles.statusText, { color: statusColor }]}>{statusText}</Text>
        </Animated.View>
      </View>

      <View style={[styles.quickActions, { backgroundColor: `${colors.card}DD`, borderColor: `${colors.border}` }]}>
        <Text style={[styles.quickTitle, { color: colors.foreground }]}>OPERADOR: {profileName.toUpperCase()}</Text>
        <Text style={[styles.quickSubtitle, { color: colors.mutedForeground }]}>Acceso directo a los módulos nativos del flujo de defensa.</Text>
        <View style={styles.quickGrid}>
          {[
            { label: 'RADAR', route: '/radar' },
            { label: 'P2P', route: '/p2p-console' },
            { label: 'EXIF', route: '/metadata' },
            { label: 'PURGA', route: '/ghost-purge' },
          ].map((item) => (
            <TouchableOpacity
              key={item.route}
              style={[styles.quickAction, { backgroundColor: `${colors.primary}12`, borderColor: `${colors.primary}40` }]}
              activeOpacity={0.8}
              onPress={() => router.push(item.route as any)}
            >
              <Text style={[styles.quickActionText, { color: colors.primary }]}>{item.label}</Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Radar HUD */}
      <View style={styles.radarContainer}>
        <Animated.View
          style={[
            styles.radarGlow,
            { shadowColor: statusColor, backgroundColor: `${statusColor}08` },
            glowStyle,
          ]}
        >
          <RadarHUD isScanning={isScanning} threatCount={threatCount} size={270} />
        </Animated.View>

        <Text style={[styles.radarLabel, { color: `${colors.cyan}80` }]}>
          {isScanning ? '[ BARRIDO ACTIVO ]' : '[ IDS HOLOGRÁFICO ]'}
        </Text>
      </View>

      {/* Tarjeta cortafuegos */}
      <Animated.View
        style={[
          styles.firewallCard,
          {
            backgroundColor: `${colors.card}DD`,
            borderColor: firewallEnabled ? `${colors.primary}80` : `${colors.border}`,
            shadowColor: colors.primary,
          },
          firewallEnabled && fwGlowStyle,
        ]}
      >
        <View style={styles.firewallLeft}>
          <MaterialCommunityIcons
            name={firewallEnabled ? 'shield-check' : 'shield-off'}
            size={28}
            color={firewallEnabled ? colors.primary : colors.mutedForeground}
          />
          <View>
            <Text style={[styles.fwTitle, { color: colors.foreground }]}>CORTAFUEGOS VPN</Text>
            <Text style={[styles.fwSub, { color: firewallEnabled ? colors.primary : colors.mutedForeground }]}>
              {firewallEnabled ? 'Tráfico enrutado — 10.0.0.1 activo' : 'Desactivado — tráfico sin protección'}
            </Text>
          </View>
        </View>
        <Switch
          value={firewallEnabled}
          onValueChange={handleToggle}
          trackColor={{ false: colors.border, true: `${colors.primary}60` }}
          thumbColor={firewallEnabled ? colors.primary : colors.mutedForeground}
          ios_backgroundColor={colors.border}
        />
      </Animated.View>

      {/* Cuadrícula de estado */}
      <View style={styles.statGrid}>
        <StatusIndicator
          label="RED"
          value={networkStatus ? 'SEGURA' : 'SIN ESCANEO'}
          ok={!!networkStatus && !networkStatus.mitm}
        />
        <StatusIndicator
          label="ROOT"
          value={rootDetected ? 'COMPROMETIDO' : 'LIMPIO'}
          ok={!rootDetected}
        />
        <StatusIndicator
          label="AMENAZAS"
          value={scanState === 'idle' ? '—' : `${threatCount} ACTIVAS`}
          ok={threatCount === 0}
        />
        <StatusIndicator
          label="CRÍTICO"
          value={scanState === 'idle' ? '—' : `${criticalCount} HALLADAS`}
          ok={criticalCount === 0}
        />
      </View>

      {/* Panel de ruta VPN */}
      {firewallEnabled && (
        <View style={[styles.infoPanel, { backgroundColor: `${colors.primary}10`, borderColor: `${colors.primary}30` }]}>
          <View style={styles.infoRow}>
            <Ionicons name="git-network" size={12} color={colors.primary} />
            <Text style={[styles.infoText, { color: colors.primary }]}>VPN TUN — IPv4: 10.0.0.2 → GW 10.0.0.1</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="lock-closed" size={12} color={colors.primary} />
            <Text style={[styles.infoText, { color: colors.primary }]}>Filtro de paquetes ACTIVO — 0.0.0.0/0 enrutado</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="shield-checkmark" size={12} color={colors.primary} />
            <Text style={[styles.infoText, { color: colors.primary }]}>Modo IDS: monitorizando anomalías de red</Text>
          </View>
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { paddingHorizontal: 18, gap: 20 },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  appTitle: {
    fontSize: 28,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 6,
    lineHeight: 30,
  },
  appSubtitle: {
    fontSize: 13,
    fontFamily: 'Inter_400Regular',
    letterSpacing: 6,
    opacity: 0.7,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 20,
    paddingHorizontal: 12,
    paddingVertical: 6,
    gap: 7,
    shadowOffset: { width: 0, height: 0 },
    shadowRadius: 10,
  },
  statusDot: {
    width: 7,
    height: 7,
    borderRadius: 3.5,
    shadowOffset: { width: 0, height: 0 },
    shadowRadius: 4,
  },
  statusText: {
    fontSize: 11,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1.5,
  },
  quickActions: {
    borderWidth: 1,
    borderRadius: 14,
    padding: 14,
    gap: 10,
  },
  quickTitle: {
    fontSize: 12,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2,
  },
  quickSubtitle: {
    fontSize: 11,
    fontFamily: 'Inter_400Regular',
    lineHeight: 16,
  },
  quickGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  quickAction: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  quickActionText: {
    fontSize: 10,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1.5,
  },
  radarContainer: {
    alignItems: 'center',
    gap: 10,
  },
  radarGlow: {
    borderRadius: 200,
    padding: 16,
    shadowOffset: { width: 0, height: 0 },
    shadowRadius: 30,
  },
  radarLabel: {
    fontSize: 10,
    fontFamily: 'Inter_500Medium',
    letterSpacing: 3,
  },
  firewallCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    borderRadius: 10,
    borderWidth: 1,
    shadowOffset: { width: 0, height: 0 },
    shadowRadius: 15,
    elevation: 4,
  },
  firewallLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    flex: 1,
  },
  fwTitle: {
    fontSize: 14,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1.5,
  },
  fwSub: {
    fontSize: 11,
    fontFamily: 'Inter_400Regular',
    marginTop: 2,
  },
  statGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  statCell: {
    flex: 1,
    minWidth: '44%',
    borderWidth: 1,
    borderRadius: 8,
    padding: 12,
    gap: 4,
    alignItems: 'flex-start',
  },
  statDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  statLabel: {
    fontSize: 9,
    fontFamily: 'Inter_500Medium',
    letterSpacing: 2,
  },
  statVal: {
    fontSize: 13,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 0.5,
  },
  infoPanel: {
    borderWidth: 1,
    borderRadius: 8,
    padding: 12,
    gap: 7,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  infoText: {
    fontSize: 11,
    fontFamily: 'Inter_400Regular',
    letterSpacing: 0.3,
  },
});
