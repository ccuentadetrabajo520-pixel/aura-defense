import React from 'react';
import { Platform, StyleSheet, View } from 'react-native';
import { useColors } from '@/hooks/useColors';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';
import { useSecurity } from '@/contexts/SecurityContext';

export default function TabLayout() {
  const colors = useColors();
  const { threatCount } = useSecurity();

  const isWeb = Platform.OS === 'web';

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.mutedForeground,
        tabBarStyle: {
          backgroundColor: '#0A0F16',
          borderTopWidth: 1,
          borderTopColor: `${colors.cyan}25`,
          height: isWeb ? 84 : 62,
          paddingBottom: isWeb ? 34 : 8,
          paddingTop: 6,
          elevation: 0,
          shadowOpacity: 0,
        },
        tabBarLabelStyle: {
          fontSize: 9,
          fontFamily: 'Inter_600SemiBold',
          letterSpacing: 1.2,
          marginTop: 2,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'SHIELD',
          tabBarIcon: ({ color, focused }) => (
            <MaterialCommunityIcons
              name={focused ? 'shield-check' : 'shield-outline'}
              size={22}
              color={color}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="scanner"
        options={{
          title: 'SCANNER',
          tabBarIcon: ({ color, focused }) => (
            <MaterialCommunityIcons
              name={focused ? 'radar' : 'radar'}
              size={22}
              color={color}
            />
          ),
          tabBarBadge: threatCount > 0 ? threatCount : undefined,
          tabBarBadgeStyle: {
            backgroundColor: colors.threat,
            fontSize: 9,
            fontFamily: 'Inter_700Bold',
          },
        }}
      />
      <Tabs.Screen
        name="telemetry"
        options={{
          title: 'TELEMETRY',
          tabBarIcon: ({ color, focused }) => (
            <MaterialCommunityIcons
              name={focused ? 'chart-line' : 'chart-line-variant'}
              size={22}
              color={color}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="purge"
        options={{
          title: 'PURGE',
          tabBarIcon: ({ color, focused }) => (
            <MaterialCommunityIcons
              name={focused ? 'delete-sweep' : 'delete-sweep-outline'}
              size={22}
              color={color}
            />
          ),
          tabBarBadge: threatCount > 0 ? threatCount : undefined,
          tabBarBadgeStyle: {
            backgroundColor: colors.threat,
            fontSize: 9,
            fontFamily: 'Inter_700Bold',
          },
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({});
