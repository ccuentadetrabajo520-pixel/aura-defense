import React from 'react';
import { Alert, Linking, Platform, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
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
    case 'high': return colors.warning;
    case 'medium': return colors.cyan;
    case 'low': return colors.primary;
  }
}

function severityLabel(severity: ThreatSeverity): string {
  return severity.toUpperCase();
}

export default function ThreatCard({ threat, onPurge }: ThreatCardProps) {
  const colors = useColors();
  const borderColor = severityColor(threat.severity, colors);

  const handlePurge = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);

    const execute = () => {
      onPurge(threat.id);
      // On Android, open the app's uninstall dialog via Linking
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
      ]
    );
  };

  if (threat.purged) {
    return (
      <View style={[styles.card, { backgroundColor: `${colors.card}80`, borderColor: `${colors.primary}40` }]}>
        <View style={styles.purgedRow}>
          <MaterialCommunityIcons name="check-circle" size={18} color={colors.primary} />
          <Text style={[styles.purgedText, { color: colors.primary }]}>
            THREAT PURGED — {threat.name}
          </Text>
        </View>
      </View>
    );
  }

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: `${colors.card}CC`,
          borderColor: `${borderColor}70`,
          shadowColor: borderColor,
        },
      ]}
    >
      {/* Severity stripe */}
      <View style={[styles.stripe, { backgroundColor: borderColor }]} />

      <View style={styles.body}>
        {/* Header row */}
        <View style={styles.headerRow}>
          <View style={styles.nameGroup}>
            <Text style={[styles.severity, { color: borderColor }]}>
              {severityLabel(threat.severity)}
            </Text>
            <Text style={[styles.name, { color: colors.foreground }]}>{threat.name}</Text>
          </View>
          <View style={[styles.scoreBadge, { borderColor: `${borderColor}80`, backgroundColor: `${borderColor}15` }]}>
            <Text style={[styles.scoreValue, { color: borderColor }]}>{threat.riskScore}</Text>
            <Text style={[styles.scoreUnit, { color: `${borderColor}90` }]}>/100</Text>
          </View>
        </View>

        {/* Package */}
        <Text style={[styles.pkg, { color: colors.mutedForeground }]}>{threat.packageName}</Text>

        {/* Threat type */}
        <View style={[styles.typeRow, { backgroundColor: `${borderColor}15` }]}>
          <MaterialCommunityIcons name="alert-octagon" size={12} color={borderColor} />
          <Text style={[styles.typeText, { color: borderColor }]}>{threat.threatType}</Text>
        </View>

        {/* Description */}
        <Text style={[styles.desc, { color: colors.foreground }]}>{threat.description}</Text>

        {/* Permissions */}
        <View style={styles.permsRow}>
          {threat.permissions.slice(0, 4).map((p) => (
            <View key={p} style={[styles.permTag, { backgroundColor: `${colors.border}` }]}>
              <Text style={[styles.permText, { color: colors.mutedForeground }]}>
                {p.replace('ACCESS_', '').replace('READ_', 'R:').replace('WRITE_', 'W:').replace('RECEIVE_', 'RX:')}
              </Text>
            </View>
          ))}
          {threat.permissions.length > 4 && (
            <Text style={[styles.morePerms, { color: colors.mutedForeground }]}>
              +{threat.permissions.length - 4}
            </Text>
          )}
        </View>

        {/* Purge button */}
        <TouchableOpacity
          style={[styles.purgeBtn, { borderColor: colors.threat, backgroundColor: `${colors.threat}18` }]}
          onPress={handlePurge}
          activeOpacity={0.7}
        >
          <MaterialCommunityIcons name="delete-sweep" size={16} color={colors.threat} />
          <Text style={[styles.purgeBtnText, { color: colors.threat }]}>EJECUTAR PURGA</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderRadius: 8,
    marginBottom: 14,
    flexDirection: 'row',
    overflow: 'hidden',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.25,
    shadowRadius: 12,
    elevation: 4,
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
  severity: {
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
