import React, { useEffect, useRef } from 'react';
import { Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';
import { useColors } from '@/hooks/useColors';
import { LogEntry, LogLevel } from '@/contexts/SecurityContext';

interface LogTerminalProps {
  logs: LogEntry[];
  maxHeight?: number;
}

function levelColor(level: LogLevel, colors: ReturnType<typeof useColors>): string {
  switch (level) {
    case 'OK': return colors.primary;
    case 'THREAT': return colors.threat;
    case 'WARN': return colors.warning;
    case 'AUDIT': return colors.cyan;
    case 'SYS': return colors.purple;
    case 'INFO': return colors.mutedForeground;
    default: return colors.foreground;
  }
}

function levelTag(level: LogLevel): string {
  switch (level) {
    case 'OK': return '[ OK ]';
    case 'THREAT': return '[AMZT]';
    case 'WARN': return '[AVSO]';
    case 'AUDIT': return '[AUDT]';
    case 'SYS': return '[SIS ]';
    case 'INFO': return '[INFO]';
    default: return '[----]';
  }
}

// Componente individual por línea — anima su entrada con fade + slide
function LogRow({ entry, isNew, colors }: { entry: LogEntry; isNew: boolean; colors: ReturnType<typeof useColors> }) {
  const opacity = useSharedValue(isNew ? 0 : 1);
  const translateX = useSharedValue(isNew ? -10 : 0);

  useEffect(() => {
    if (isNew) {
      opacity.value = withTiming(1, { duration: 160 });
      translateX.value = withTiming(0, { duration: 160 });
    }
  }, []);

  const rowStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
    transform: [{ translateX: translateX.value }],
  }));

  return (
    <Animated.View style={[styles.row, rowStyle]}>
      <Text style={[styles.ts, { color: colors.mutedForeground }]}>
        {entry.timestamp}
      </Text>
      <Text style={[styles.tag, { color: levelColor(entry.level, colors) }]}>
        {levelTag(entry.level)}
      </Text>
      <Text
        style={[
          styles.msg,
          {
            color:
              entry.level === 'THREAT'
                ? colors.threat
                : entry.level === 'OK'
                ? colors.primary
                : colors.foreground,
          },
        ]}
        numberOfLines={3}
      >
        {entry.message}
      </Text>
    </Animated.View>
  );
}

export default function LogTerminal({ logs, maxHeight = 300 }: LogTerminalProps) {
  const colors = useColors();
  const scrollRef = useRef<ScrollView>(null);
  // Rastrear qué entradas ya habían sido renderizadas antes
  const prevLengthRef = useRef(0);

  useEffect(() => {
    if (logs.length > 0) {
      const timer = setTimeout(() => {
        scrollRef.current?.scrollToEnd({ animated: true });
      }, 40);
      return () => clearTimeout(timer);
    }
  }, [logs.length]);

  const newFromIndex = prevLengthRef.current;
  // Actualizar después del render para la próxima comparación
  useEffect(() => {
    prevLengthRef.current = logs.length;
  });

  return (
    <View
      style={[
        styles.container,
        {
          maxHeight,
          backgroundColor: '#060910',
          borderColor: `${colors.cyan}40`,
          borderRadius: (colors as any).radius ?? 6,
        },
      ]}
    >
      {/* Barra de título estilo terminal */}
      <View style={[styles.header, { borderBottomColor: `${colors.cyan}30` }]}>
        <View style={[styles.dot, { backgroundColor: colors.threat }]} />
        <View style={[styles.dot, { backgroundColor: colors.warning, marginLeft: 6 }]} />
        <View style={[styles.dot, { backgroundColor: colors.primary, marginLeft: 6 }]} />
        <Text style={[styles.headerText, { color: colors.cyan }]}>
          REGISTRO DE AUDITORÍA — {logs.length} entradas
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
            {'> Esperando inicio de escaneo...\n> Ejecuta CAZA DE AMENAZAS para comenzar.'}
          </Text>
        ) : (
          logs.map((entry, index) => (
            <LogRow
              key={entry.id}
              entry={entry}
              isNew={index >= newFromIndex}
              colors={colors}
            />
          ))
        )}
        {/* Cursor parpadeante */}
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
    marginBottom: 3,
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
