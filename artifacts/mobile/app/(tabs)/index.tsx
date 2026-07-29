import React, { useEffect } from 'react';
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
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useSecurity } from '@/contexts/SecurityContext';
import { useColors } from '@/hooks/useColors';
import RadarHUD from '@/components/RadarHUD';

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
  const { firewallEnabled, toggleFirewall, scanState, threats, threatCount, criticalCount, networkStatus, rootDetected } =
    useSecurity();

  const isScanning = scanState === 'scanning';
  const glowOpacity = useSharedValue(0.3);
  const fwGlow = useSharedValue(0);

  useEffect(() => {
    glowOpacity.value = withRepeat(withTiming(0.8, { duration: 1400 }), -1, true);
  }, []);

  useEffect(() => {
    fwGlow.value = withTiming(firewallEnabled ? 1 : 0, { duration: 400 });
  }, [firewallEnabled]);

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
  const statusText = isScanning ? 'SCANNING...' : overallOk ? 'SECURE' : `${threatCount} THREAT${threatCount !== 1 ? 'S' : ''}`;

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={[styles.content, { paddingTop: topPad + 16, paddingBottom: bottomPad + 100 }]}
      showsVerticalScrollIndicator={false}
    >
      {/* Top header */}
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

        {/* Scanning ring label */}
        <Text style={[styles.radarLabel, { color: `${colors.cyan}80` }]}>
          {isScanning ? '[ ACTIVE SWEEP ]' : '[ HOLOGRAPHIC IDS ]'}
        </Text>
      </View>

      {/* Firewall toggle card */}
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
            <Text style={[styles.fwTitle, { color: colors.foreground }]}>VPN FIREWALL</Text>
            <Text style={[styles.fwSub, { color: firewallEnabled ? colors.primary : colors.mutedForeground }]}>
              {firewallEnabled ? 'Traffic routed — 10.0.0.1 active' : 'Disabled — unprotected traffic'}
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

      {/* Status grid */}
      <View style={styles.statGrid}>
        <StatusIndicator
          label="NETWORK"
          value={networkStatus ? 'SECURED' : 'NO SCAN'}
          ok={!!networkStatus && !networkStatus.mitm}
        />
        <StatusIndicator
          label="ROOT"
          value={rootDetected ? 'COMPROMISED' : 'CLEAN'}
          ok={!rootDetected}
        />
        <StatusIndicator
          label="THREATS"
          value={scanState === 'idle' ? '—' : `${threatCount} ACTIVE`}
          ok={threatCount === 0}
        />
        <StatusIndicator
          label="CRITICAL"
          value={scanState === 'idle' ? '—' : `${criticalCount} FOUND`}
          ok={criticalCount === 0}
        />
      </View>

      {/* Firewall route info */}
      {firewallEnabled && (
        <View style={[styles.infoPanel, { backgroundColor: `${colors.primary}10`, borderColor: `${colors.primary}30` }]}>
          <View style={styles.infoRow}>
            <Ionicons name="git-network" size={12} color={colors.primary} />
            <Text style={[styles.infoText, { color: colors.primary }]}>VPN TUN — IPv4: 10.0.0.2 → GW 10.0.0.1</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="lock-closed" size={12} color={colors.primary} />
            <Text style={[styles.infoText, { color: colors.primary }]}>Packet filter ACTIVE — 0.0.0.0/0 routed</Text>
          </View>
          <View style={styles.infoRow}>
            <Ionicons name="shield-checkmark" size={12} color={colors.primary} />
            <Text style={[styles.infoText, { color: colors.primary }]}>IDS mode: monitoring network anomalies</Text>
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
