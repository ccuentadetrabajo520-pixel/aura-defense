import React, { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withTiming,
} from 'react-native-reanimated';
import { useColors } from '@/hooks/useColors';

interface RadarHUDProps {
  isScanning: boolean;
  threatCount?: number;
  size?: number;
}

const BLIP_POSITIONS = [
  { angle: 42, r: 0.62 },
  { angle: 118, r: 0.41 },
  { angle: 228, r: 0.73 },
  { angle: 305, r: 0.52 },
  { angle: 172, r: 0.32 },
];

export default function RadarHUD({ isScanning, threatCount = 0, size = 270 }: RadarHUDProps) {
  const colors = useColors();
  const rotation = useSharedValue(0);
  const pulseVal = useSharedValue(0.5);

  const half = size / 2;
  const armColor = isScanning ? colors.cyan : colors.primary;

  useEffect(() => {
    const dur = isScanning ? 1100 : 3200;
    rotation.value = withRepeat(
      withTiming(360, { duration: dur, easing: Easing.linear }),
      -1,
      false
    );
  }, [isScanning]);

  useEffect(() => {
    pulseVal.value = withRepeat(withTiming(1, { duration: 900 }), -1, true);
  }, []);

  // Three sweep arms — trail effect
  const sweepStyle = useAnimatedStyle(() => ({
    transform: [{ rotate: `${rotation.value}deg` }],
  }));
  const trail1Style = useAnimatedStyle(() => ({
    transform: [{ rotate: `${rotation.value - 25}deg` }],
    opacity: 0.35,
  }));
  const trail2Style = useAnimatedStyle(() => ({
    transform: [{ rotate: `${rotation.value - 55}deg` }],
    opacity: 0.12,
  }));
  const blipOpacity = useAnimatedStyle(() => ({
    opacity: pulseVal.value,
  }));

  const RING_SCALES = [0.28, 0.52, 0.76, 1.0];

  return (
    <View style={[styles.outer, { width: size, height: size, borderRadius: half }]}>
      {/* Concentric rings */}
      {RING_SCALES.map((s, i) => (
        <View
          key={i}
          style={{
            position: 'absolute',
            width: size * s,
            height: size * s,
            borderRadius: (size * s) / 2,
            borderWidth: i === 3 ? 1.5 : 0.8,
            borderColor: i === 3 ? armColor : `${armColor}55`,
            left: half - (size * s) / 2,
            top: half - (size * s) / 2,
          }}
        />
      ))}

      {/* Crosshairs */}
      <View style={[styles.lineH, { top: half - 0.5, width: size, backgroundColor: `${armColor}35` }]} />
      <View style={[styles.lineV, { left: half - 0.5, height: size, backgroundColor: `${armColor}35` }]} />

      {/* Diagonal crosses */}
      <View
        style={{
          position: 'absolute',
          width: size * 0.65,
          height: 0.8,
          backgroundColor: `${armColor}18`,
          left: half - (size * 0.65) / 2,
          top: half - 0.4,
          transform: [{ rotate: '45deg' }],
        }}
      />
      <View
        style={{
          position: 'absolute',
          width: size * 0.65,
          height: 0.8,
          backgroundColor: `${armColor}18`,
          left: half - (size * 0.65) / 2,
          top: half - 0.4,
          transform: [{ rotate: '-45deg' }],
        }}
      />

      {/* Sweep trail 2 (faintest) */}
      <Animated.View style={[StyleSheet.absoluteFill, trail2Style]}>
        <View
          style={{
            position: 'absolute',
            left: half,
            top: half - 0.75,
            width: half - 8,
            height: 1.5,
            backgroundColor: `${armColor}20`,
          }}
        />
      </Animated.View>

      {/* Sweep trail 1 */}
      <Animated.View style={[StyleSheet.absoluteFill, trail1Style]}>
        <View
          style={{
            position: 'absolute',
            left: half,
            top: half - 0.75,
            width: half - 8,
            height: 1.5,
            backgroundColor: `${armColor}55`,
          }}
        />
      </Animated.View>

      {/* Main sweep arm */}
      <Animated.View style={[StyleSheet.absoluteFill, sweepStyle]}>
        <View
          style={{
            position: 'absolute',
            left: half,
            top: half - 1,
            width: half - 8,
            height: 2,
            backgroundColor: armColor,
            shadowColor: armColor,
            shadowOffset: { width: 0, height: 0 },
            shadowOpacity: 0.9,
            shadowRadius: 6,
          }}
        />
      </Animated.View>

      {/* Threat blips */}
      {BLIP_POSITIONS.slice(0, Math.min(threatCount, 5)).map((blip, i) => {
        const rad = (blip.angle * Math.PI) / 180;
        const bx = half + Math.cos(rad) * half * blip.r - 5;
        const by = half + Math.sin(rad) * half * blip.r - 5;
        return (
          <Animated.View
            key={i}
            style={[
              {
                position: 'absolute',
                width: 10,
                height: 10,
                borderRadius: 5,
                backgroundColor: colors.threat,
                left: bx,
                top: by,
                shadowColor: colors.threat,
                shadowOffset: { width: 0, height: 0 },
                shadowOpacity: 1,
                shadowRadius: 8,
                elevation: 4,
              },
              blipOpacity,
            ]}
          />
        );
      })}

      {/* Center pivot */}
      <View
        style={{
          position: 'absolute',
          width: 8,
          height: 8,
          borderRadius: 4,
          backgroundColor: armColor,
          left: half - 4,
          top: half - 4,
          shadowColor: armColor,
          shadowOffset: { width: 0, height: 0 },
          shadowOpacity: 1,
          shadowRadius: 10,
          elevation: 6,
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  outer: {
    overflow: 'hidden',
    position: 'relative',
    alignSelf: 'center',
  },
  lineH: {
    position: 'absolute',
    height: 1,
  },
  lineV: {
    position: 'absolute',
    width: 1,
  },
});
