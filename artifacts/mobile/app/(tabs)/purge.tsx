import React from 'react';
import {
  FlatList,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useSecurity } from '@/contexts/SecurityContext';
import { useColors } from '@/hooks/useColors';
import ThreatCard from '@/components/ThreatCard';

export default function PurgeScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { threats, purgeThreat, purgeAll, scanState, threatCount, criticalCount } = useSecurity();

  const topPad = Platform.OS === 'web' ? 67 : insets.top;
  const bottomPad = Platform.OS === 'web' ? 34 : insets.bottom;

  const active = threats.filter((t) => !t.purged);
  const purged = threats.filter((t) => t.purged);

  const handlePurgeAll = () => {
    if (active.length === 0) return;
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
    purgeAll();
  };

  if (scanState === 'idle') {
    return (
      <View style={[styles.container, { backgroundColor: colors.background }]}>
        <View style={[styles.empty, { paddingTop: topPad + 60 }]}>
          <MaterialCommunityIcons name="shield-search" size={60} color={`${colors.mutedForeground}60`} />
          <Text style={[styles.emptyTitle, { color: colors.foreground }]}>NO SCAN DATA</Text>
          <Text style={[styles.emptySub, { color: colors.mutedForeground }]}>
            Run a Threat Hunt from the Scanner tab to populate the Purge Console.
          </Text>
        </View>
      </View>
    );
  }

  return (
    <FlatList
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={[
        styles.content,
        { paddingTop: topPad + 16, paddingBottom: bottomPad + 100 },
      ]}
      showsVerticalScrollIndicator={false}
      data={threats}
      keyExtractor={(item) => item.id}
      ListHeaderComponent={
        <View style={styles.listHeader}>
          {/* Page title */}
          <View style={styles.pageHeader}>
            <Text style={[styles.pageTitle, { color: colors.foreground }]}>PURGE CONSOLE</Text>
            <View style={[styles.badge, { backgroundColor: `${threatCount > 0 ? colors.threat : colors.primary}20`, borderColor: `${threatCount > 0 ? colors.threat : colors.primary}50` }]}>
              <Text style={[styles.badgeText, { color: threatCount > 0 ? colors.threat : colors.primary }]}>
                {active.length} ACTIVE
              </Text>
            </View>
          </View>

          {/* Summary row */}
          <View style={[styles.summaryRow, { borderColor: colors.border, backgroundColor: colors.card }]}>
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryVal, { color: colors.threat }]}>
                {threats.filter((t) => t.severity === 'critical' && !t.purged).length}
              </Text>
              <Text style={[styles.summaryLabel, { color: colors.mutedForeground }]}>CRITICAL</Text>
            </View>
            <View style={[styles.summaryDivider, { backgroundColor: colors.border }]} />
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryVal, { color: colors.warning }]}>
                {threats.filter((t) => t.severity === 'high' && !t.purged).length}
              </Text>
              <Text style={[styles.summaryLabel, { color: colors.mutedForeground }]}>HIGH</Text>
            </View>
            <View style={[styles.summaryDivider, { backgroundColor: colors.border }]} />
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryVal, { color: colors.cyan }]}>
                {threats.filter((t) => t.severity === 'medium' && !t.purged).length}
              </Text>
              <Text style={[styles.summaryLabel, { color: colors.mutedForeground }]}>MEDIUM</Text>
            </View>
            <View style={[styles.summaryDivider, { backgroundColor: colors.border }]} />
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryVal, { color: colors.primary }]}>
                {purged.length}
              </Text>
              <Text style={[styles.summaryLabel, { color: colors.mutedForeground }]}>PURGED</Text>
            </View>
          </View>

          {/* Purge all button */}
          {active.length > 0 && (
            <TouchableOpacity
              style={[styles.purgeAllBtn, { borderColor: colors.threat, backgroundColor: `${colors.threat}15` }]}
              onPress={handlePurgeAll}
              activeOpacity={0.7}
            >
              <MaterialCommunityIcons name="nuke" size={18} color={colors.threat} />
              <Text style={[styles.purgeAllText, { color: colors.threat }]}>
                PURGE ALL {active.length} THREATS
              </Text>
            </TouchableOpacity>
          )}

          {/* Section label */}
          {threats.length > 0 && (
            <View style={styles.sectionLabel}>
              <View style={[styles.sectionLine, { backgroundColor: colors.border }]} />
              <Text style={[styles.sectionText, { color: colors.mutedForeground }]}>
                DETECTED THREATS
              </Text>
              <View style={[styles.sectionLine, { backgroundColor: colors.border }]} />
            </View>
          )}

          {/* All-clear state */}
          {active.length === 0 && scanState === 'complete' && (
            <View style={[styles.allClear, { backgroundColor: `${colors.primary}10`, borderColor: `${colors.primary}40` }]}>
              <MaterialCommunityIcons name="shield-check" size={36} color={colors.primary} />
              <Text style={[styles.allClearTitle, { color: colors.primary }]}>ALL THREATS PURGED</Text>
              <Text style={[styles.allClearSub, { color: `${colors.primary}80` }]}>
                System is clean. Run a new scan to monitor for new threats.
              </Text>
            </View>
          )}
        </View>
      }
      renderItem={({ item }) => (
        <ThreatCard threat={item} onPurge={purgeThreat} />
      )}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { paddingHorizontal: 18 },
  listHeader: {
    gap: 14,
    marginBottom: 6,
  },
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
  badge: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  badgeText: {
    fontSize: 10,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1.5,
  },
  summaryRow: {
    flexDirection: 'row',
    borderWidth: 1,
    borderRadius: 8,
    overflow: 'hidden',
  },
  summaryItem: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 14,
    gap: 3,
  },
  summaryVal: {
    fontSize: 24,
    fontFamily: 'Inter_700Bold',
  },
  summaryLabel: {
    fontSize: 8,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 1.5,
  },
  summaryDivider: {
    width: 1,
    marginVertical: 12,
  },
  purgeAllBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
    paddingVertical: 14,
    borderRadius: 8,
    borderWidth: 1.5,
  },
  purgeAllText: {
    fontSize: 13,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2.5,
  },
  sectionLabel: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  sectionLine: {
    flex: 1,
    height: 1,
  },
  sectionText: {
    fontSize: 9,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 2.5,
  },
  allClear: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 30,
    alignItems: 'center',
    gap: 10,
  },
  allClearTitle: {
    fontSize: 16,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2,
  },
  allClearSub: {
    fontSize: 12,
    fontFamily: 'Inter_400Regular',
    textAlign: 'center',
    lineHeight: 18,
  },
  empty: {
    flex: 1,
    alignItems: 'center',
    paddingHorizontal: 40,
    gap: 14,
  },
  emptyTitle: {
    fontSize: 18,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 3,
    marginTop: 8,
  },
  emptySub: {
    fontSize: 13,
    fontFamily: 'Inter_400Regular',
    textAlign: 'center',
    lineHeight: 20,
  },
});
