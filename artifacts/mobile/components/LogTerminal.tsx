import React, { useEffect, useRef } from 'react';
import { Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useColors } from '@/hooks/useColors';
import { LogEntry, LogLevel } from '@/contexts/SecurityContext';

interface LogTerminalProps {
  logs: LogEntry[];
  maxHeight?: number;
}

function levelColor(level: LogLevel, colors: ReturnType<typeof useColors>): string {
  switch (level) {
    case 'OK': return colors.primary;      // neon green
    case 'THREAT': return colors.threat;   // crimson
    case 'WARN': return colors.warning;    // amber
    case 'AUDIT': return colors.cyan;      // cyan
    case 'SYS': return colors.purple;      // purple
    case 'INFO': return colors.mutedForeground;
    default: return colors.foreground;
  }
}

function levelTag(level: LogLevel): string {
  switch (level) {
    case 'OK': return '[ OK ]';
    case 'THREAT': return '[THRT]';
    case 'WARN': return '[WARN]';
    case 'AUDIT': return '[AUDT]';
    case 'SYS': return '[SYS ]';
    case 'INFO': return '[INFO]';
    default: return '[----]';
  }
}

export default function LogTerminal({ logs, maxHeight = 300 }: LogTerminalProps) {
  const colors = useColors();
  const scrollRef = useRef<ScrollView>(null);

  useEffect(() => {
    if (logs.length > 0) {
      setTimeout(() => {
        scrollRef.current?.scrollToEnd({ animated: true });
      }, 50);
    }
  }, [logs.length]);

  const topInset = Platform.OS === 'web' ? 0 : 0;

  return (
    <View
      style={[
        styles.container,
        {
          maxHeight,
          backgroundColor: '#060910',
          borderColor: `${colors.cyan}40`,
          borderRadius: colors.radius,
        },
      ]}
    >
      {/* Header bar */}
      <View style={[styles.header, { borderBottomColor: `${colors.cyan}30` }]}>
        <View style={[styles.dot, { backgroundColor: colors.threat }]} />
        <View style={[styles.dot, { backgroundColor: colors.warning, marginLeft: 6 }]} />
        <View style={[styles.dot, { backgroundColor: colors.primary, marginLeft: 6 }]} />
        <Text style={[styles.headerText, { color: colors.cyan }]}>
          AUDIT LOG — {logs.length} entries
        </Text>
      </View>

      <ScrollView
        ref={scrollRef}
        style={styles.scroll}
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
      >
        {logs.length === 0 ? (
          <Text style={[styles.empty, { color: colors.mutedForeground }]}>
            {'> Awaiting scan initiation...\n> Run THREAT HUNT to begin.'}
          </Text>
        ) : (
          logs.map((entry) => (
            <View key={entry.id} style={styles.row}>
              <Text style={[styles.ts, { color: `${colors.mutedForeground}` }]}>
                {entry.timestamp}
              </Text>
              <Text style={[styles.tag, { color: levelColor(entry.level, colors) }]}>
                {levelTag(entry.level)}
              </Text>
              <Text
                style={[
                  styles.msg,
                  { color: entry.level === 'THREAT' ? colors.threat : entry.level === 'OK' ? colors.primary : colors.foreground },
                ]}
                numberOfLines={3}
              >
                {entry.message}
              </Text>
            </View>
          ))
        )}
        {/* Blinking cursor */}
        {logs.length > 0 && (
          <Text style={[styles.cursor, { color: colors.cyan }]}>{'> _'}</Text>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderWidth: 1,
    overflow: 'hidden',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderBottomWidth: 1,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  headerText: {
    fontFamily: 'Inter_500Medium',
    fontSize: 10,
    letterSpacing: 1.5,
    marginLeft: 10,
    textTransform: 'uppercase',
  },
  scroll: {
    flex: 1,
  },
  content: {
    padding: 10,
    paddingBottom: 16,
  },
  row: {
    flexDirection: 'row',
    marginBottom: 2,
    flexWrap: 'wrap',
    gap: 4,
  },
  ts: {
    fontFamily: Platform.select({ ios: 'Courier New', android: 'monospace', default: 'monospace' }),
    fontSize: 9,
    opacity: 0.6,
    alignSelf: 'flex-start',
    paddingTop: 1,
    minWidth: 68,
  },
  tag: {
    fontFamily: Platform.select({ ios: 'Courier New', android: 'monospace', default: 'monospace' }),
    fontSize: 10,
    fontWeight: '700',
    alignSelf: 'flex-start',
    minWidth: 46,
  },
  msg: {
    fontFamily: Platform.select({ ios: 'Courier New', android: 'monospace', default: 'monospace' }),
    fontSize: 10.5,
    flex: 1,
    lineHeight: 15,
  },
  empty: {
    fontFamily: Platform.select({ ios: 'Courier New', android: 'monospace', default: 'monospace' }),
    fontSize: 11,
    lineHeight: 18,
    padding: 4,
  },
  cursor: {
    fontFamily: Platform.select({ ios: 'Courier New', android: 'monospace', default: 'monospace' }),
    fontSize: 11,
    marginTop: 4,
  },
});
