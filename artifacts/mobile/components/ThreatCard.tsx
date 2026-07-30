import React, { useEffect } from 'react';
import { Alert, Linking, Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withSequence,
  withTiming,
} from 'react-native-reanimated';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { ThreatItem, ThreatSeverity } from '@/contexts/SecurityContext';
import { useColors } from '@/hooks/useColors';

interface ThreatCardProps {
  threat: ThreatItem;
  onPurge: (id: string) => void;
}

function severityColor(severity: ThreatSeverity, colors: ReturnType<typeof useColors>): string {
  switch (severity) {
    case 'critical': return colors.threat;
    case 'high':     return colors.warning;
    case 'medium':   return colors.cyan;
    case 'low':      return colors.primary;
  }
}

function severityLabel(severity: ThreatSeverity): string {
  switch (severity) {
    case 'critical': return 'CRÍTICO';
    case 'high':     return 'ALTO';
    case 'medium':   return 'MEDIO';
    case 'low':      return 'BAJO';
  }
}

export default function ThreatCard({ threat, onPurge }: ThreatCardProps) {
  const colors = useColors();
  const accentColor = severityColor(threat.severity, colors);
  const isCritical = threat.severity === 'critical' && !threat.purged;

  // Pulsing neon glow for critical cards.
  // Only shadowOpacity is animated — keeps the worklet pure numeric (no string ops).
  const glowOpacity = useSharedValue(isCritical ? 0.2 : 0.25);

  useEffect(() => {
    if (isCritical) {
      glowOpacity.value = withRepeat(
        withSequence(
          withTiming(0.95, { duration: 550 }),
          withTiming(0.10, { duration: 650 }),
          withTiming(0.75, { duration: 380 }),
          withTiming(0.08, { duration: 480 }),
        ),
        -1,
        false,
      );
    } else {
      glowOpacity.value = withTiming(0.25, { duration: 300 });
    }
  }, [isCritical]);

  // Worklet: only numeric values — no string concatenation / toString / padStart
  const glowStyle = useAnimatedStyle(() => ({
    shadowOpacity: glowOpacity.value,
  }));

  const handlePurge = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);

    const execute = () => {
      onPurge(threat.id);
      if (Platform.OS === 'android') {
        Linking.openURL(`package:${threat.packageName}`).catch(() => {});
      }
    };

    if (Platform.OS === 'web') {
      execute();
      return;
    }

    Alert.alert(
      'EJECUTAR PURGA',
      `¿Desinstalar "${threat.name}"?\n\nEsta acción abrirá el desinstalador del sistema Android.`,
      [
        { text: 'CANCELAR', style: 'cancel' },
        { text: 'PURGAR', style: 'destructive', onPress: execute },
      ],
    );
  };

  if (threat.purged) {
    return (
      <View style={[styles.card, styles.cardPurged, { borderColor: `${colors.primary}40` }]}>
        <View style={styles.purgedRow}>
          <MaterialCommunityIcons name="check-circle" size={18} color={colors.primary} />
          <Text style={[styles.purgedText, { color: colors.primary }]}>
            AMENAZA PURGADA — {threat.name}
          </Text>
        </View>
      </View>
    );
  }

  return (
    // Outer wrapper carries the animated shadow glow — no overflow:hidden here
    // so the shadow is never clipped on any platform.
    <Animated.View
      style={[
        styles.card,
        {
          backgroundColor: `${colors.card}CC`,
          borderColor: `${accentColor}70`,
          shadowColor: accentColor,
          shadowRadius: isCritical ? 18 : 10,
          shadowOffset: { width: 0, height: 0 },
        },
        glowStyle,
      ]}
    >
      {/* Franja lateral de severidad — rounded via borderRadius on the stripe itself */}
      <View style={[styles.stripe, { backgroundColor: accentColor }]} />

      <View style={styles.body}>
        {/* Fila de cabecera */}
        <View style={styles.headerRow}>
          <View style={styles.nameGroup}>
            <Text style={[styles.severityLabel, { color: accentColor }]}>
              {severityLabel(threat.severity)}
            </Text>
            <Text style={[styles.name, { color: colors.foreground }]}>{threat.name}</Text>
          </View>
          <View style={[styles.scoreBadge, { borderColor: `${accentColor}80`, backgroundColor: `${accentColor}15` }]}>
            <Text style={[styles.scoreValue, { color: accentColor }]}>{threat.riskScore}</Text>
            <Text style={[styles.scoreUnit, { color: `${accentColor}90` }]}>/100</Text>
          </View>
        </View>

        {/* Paquete */}
        <Text style={[styles.pkg, { color: colors.mutedForeground }]}>{threat.packageName}</Text>

        {/* Tipo de amenaza */}
        <View style={[styles.typeRow, { backgroundColor: `${accentColor}15` }]}>
          <MaterialCommunityIcons name="alert-octagon" size={12} color={accentColor} />
          <Text style={[styles.typeText, { color: accentColor }]}>{threat.threatType}</Text>
        </View>

        {/* Descripción */}
        <Text style={[styles.desc, { color: colors.foreground }]}>{threat.description}</Text>

        {/* Permisos */}
        <View style={styles.permsRow}>
          {threat.permissions.slice(0, 4).map((p) => (
            <View key={p} style={[styles.permTag, { backgroundColor: colors.border }]}>
              <Text style={[styles.permText, { color: colors.mutedForeground }]}>
                {p
                  .replace('ACCESS_', '')
                  .replace('READ_', 'R:')
                  .replace('WRITE_', 'W:')
                  .replace('RECEIVE_', 'RX:')}
              </Text>
            </View>
          ))}
          {threat.permissions.length > 4 && (
            <Text style={[styles.morePerms, { color: colors.mutedForeground }]}>
              +{threat.permissions.length - 4}
            </Text>
          )}
        </View>

        {/* Botón de purga */}
        <TouchableOpacity
          style={[styles.purgeBtn, { borderColor: colors.threat, backgroundColor: `${colors.threat}18` }]}
          onPress={handlePurge}
          activeOpacity={0.7}
        >
          <MaterialCommunityIcons name="delete-sweep" size={16} color={colors.threat} />
          <Text style={[styles.purgeBtnText, { color: colors.threat }]}>EJECUTAR PURGA</Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderRadius: 8,
    marginBottom: 14,
    flexDirection: 'row',
    // No overflow:hidden — keeps shadow/glow unclipped on all platforms
    elevation: 4,
  },
  cardPurged: {
    backgroundColor: 'transparent',
    shadowOpacity: 0,
    elevation: 0,
  },
  stripe: {
    width: 3,
    borderTopLeftRadius: 8,
    borderBottomLeftRadius: 8,
  },
  body: {
    flex: 1,
    padding: 14,
    gap: 8,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  nameGroup: {
    flex: 1,
    gap: 2,
  },
  severityLabel: {
    fontSize: 9,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2,
  },
  name: {
    fontSize: 15,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 0.3,
  },
  scoreBadge: {
    flexDirection: 'row',
    alignItems: 'baseline',
    borderWidth: 1,
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 4,
    gap: 1,
  },
  scoreValue: {
    fontSize: 18,
    fontFamily: 'Inter_700Bold',
  },
  scoreUnit: {
    fontSize: 10,
    fontFamily: 'Inter_500Medium',
  },
  pkg: {
    fontSize: 10,
    fontFamily: 'Inter_400Regular',
    letterSpacing: 0.3,
  },
  typeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 4,
  },
  typeText: {
    fontSize: 11,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 0.5,
  },
  desc: {
    fontSize: 12,
    fontFamily: 'Inter_400Regular',
    lineHeight: 17,
    opacity: 0.85,
  },
  permsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 5,
  },
  permTag: {
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: 3,
  },
  permText: {
    fontSize: 9,
    fontFamily: 'Inter_500Medium',
    letterSpacing: 0.5,
  },
  morePerms: {
    fontSize: 10,
    fontFamily: 'Inter_500Medium',
    alignSelf: 'center',
  },
  purgeBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 10,
    borderRadius: 6,
    borderWidth: 1,
    marginTop: 4,
  },
  purgeBtnText: {
    fontSize: 12,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2,
  },
  purgedRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 14,
  },
  purgedText: {
    fontSize: 12,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 1,
  },
});
