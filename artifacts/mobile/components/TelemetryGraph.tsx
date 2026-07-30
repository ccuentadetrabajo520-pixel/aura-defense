import React, { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';
import { useColors } from '@/hooks/useColors';

interface BarConfig {
  label: string;
  color: string;
  /** ms per animation step */
  speed: number;
  initial: number;
  min: number;
  max: number;
}

interface AnimatedBarProps {
  config: BarConfig;
}

// Each bar is its own component — never call useAnimatedStyle inside a .map()
function AnimatedBar({ config }: AnimatedBarProps) {
  const heightPct = useSharedValue(config.initial);

  useEffect(() => {
    // cancelled flag prevents the recursive chain from touching shared values
    // after the component unmounts, eliminating the memory leak.
    let cancelled = false;
    let timerId: ReturnType<typeof setTimeout>;

    const scheduleNext = () => {
      if (cancelled) return;
      const range = config.max - config.min;
      const target = config.min + Math.random() * range;
      const duration = config.speed * (0.55 + Math.random() * 0.9);
      heightPct.value = withTiming(target, {
        duration,
        easing: Easing.inOut(Easing.quad),
      });
      timerId = setTimeout(scheduleNext, duration);
    };

    // Staggered start so bars don't all move in sync
    timerId = setTimeout(scheduleNext, Math.random() * config.speed);

    return () => {
      cancelled = true;
      clearTimeout(timerId);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const barStyle = useAnimatedStyle(() => ({
    height: `${heightPct.value}%`,
  }));

  return (
    <View style={styles.barWrapper}>
      <View style={styles.barTrack}>
        <Animated.View
          style={[
            styles.barFill,
            { backgroundColor: config.color, shadowColor: config.color },
            barStyle,
          ]}
        />
      </View>
      <Text style={[styles.barLabel, { color: config.color }]}>{config.label}</Text>
    </View>
  );
}

interface TelemetryGraphProps {
  isActive: boolean;
}

export default function TelemetryGraph({ isActive }: TelemetryGraphProps) {
  const colors = useColors();

  // Bars always animate — isActive only changes the status pill label
  const BARS: BarConfig[] = [
    { label: 'T-0', color: colors.primary, speed: 320, initial: 45, min: 8,  max: 95  },
    { label: 'T-1', color: colors.cyan,    speed: 210, initial: 72, min: 12, max: 100 },
    { label: 'T-2', color: colors.purple,  speed: 390, initial: 30, min: 5,  max: 88  },
    { label: 'T-3', color: colors.primary, speed: 260, initial: 58, min: 15, max: 92  },
    { label: 'T-4', color: colors.warning, speed: 180, initial: 88, min: 20, max: 100 },
    { label: 'T-5', color: colors.cyan,    speed: 340, initial: 22, min: 6,  max: 85  },
    { label: 'T-6', color: colors.purple,  speed: 270, initial: 65, min: 10, max: 96  },
    { label: 'T-7', color: colors.primary, speed: 230, initial: 40, min: 8,  max: 90  },
  ];

  return (
    <View style={[styles.container, { backgroundColor: colors.card, borderColor: `${colors.cyan}30` }]}>
      <View style={styles.headerRow}>
        <Text style={[styles.title, { color: colors.cyan }]}>ANÁLISIS DE HILOS CPU</Text>
        <View style={[
          styles.activePill,
          {
            backgroundColor: isActive ? `${colors.primary}25` : `${colors.mutedForeground}20`,
            borderColor: isActive ? colors.primary : colors.mutedForeground,
          },
        ]}>
          <View style={[styles.activeDot, { backgroundColor: isActive ? colors.primary : colors.mutedForeground }]} />
          <Text style={[styles.activeLabel, { color: isActive ? colors.primary : colors.mutedForeground }]}>
            {isActive ? 'EN VIVO' : 'ACTIVO'}
          </Text>
        </View>
      </View>

      <View style={styles.graphArea}>
        {BARS.map((cfg) => (
          <AnimatedBar key={cfg.label} config={cfg} />
        ))}
      </View>

      {/* Etiquetas del eje Y */}
      <View style={styles.yAxis}>
        {['100', '75', '50', '25', '0'].map((v) => (
          <Text key={v} style={[styles.yLabel, { color: colors.mutedForeground }]}>{v}</Text>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderWidth: 1,
    borderRadius: 6,
    padding: 14,
    paddingBottom: 8,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  title: {
    fontSize: 11,
    fontFamily: 'Inter_600SemiBold',
    letterSpacing: 2,
  },
  activePill: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 3,
    gap: 5,
  },
  activeDot: {
    width: 5,
    height: 5,
    borderRadius: 2.5,
  },
  activeLabel: {
    fontSize: 9,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 1.5,
  },
  graphArea: {
    flexDirection: 'row',
    height: 100,
    gap: 4,
    alignItems: 'flex-end',
    marginLeft: 32,
  },
  barWrapper: {
    flex: 1,
    alignItems: 'center',
    height: '100%',
    justifyContent: 'flex-end',
  },
  barTrack: {
    flex: 1,
    width: '80%',
    justifyContent: 'flex-end',
    backgroundColor: 'rgba(255,255,255,0.04)',
    borderRadius: 2,
    overflow: 'hidden',
  },
  barFill: {
    width: '100%',
    borderRadius: 2,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 5,
    elevation: 3,
  },
  barLabel: {
    fontSize: 8,
    fontFamily: 'Inter_600SemiBold',
    marginTop: 4,
    letterSpacing: 0.5,
  },
  yAxis: {
    position: 'absolute',
    left: 14,
    top: 44,
    height: 100,
    justifyContent: 'space-between',
  },
  yLabel: {
    fontSize: 8,
    fontFamily: 'Inter_400Regular',
    lineHeight: 10,
  },
});
