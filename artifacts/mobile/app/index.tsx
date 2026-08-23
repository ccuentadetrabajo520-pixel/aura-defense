import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { Redirect } from 'expo-router';

export default function EntryScreen() {
  const [loading, setLoading] = useState(true);
  const [isOnboardingCompleted, setIsOnboardingCompleted] = useState(false);
  const [hasProfile, setHasProfile] = useState(false);

  useEffect(() => {
    let active = true;

    const hydrate = async () => {
      try {
        const [storedName, onboardingStatus] = await Promise.all([
          SecureStore.getItemAsync('aura-user-name'),
          SecureStore.getItemAsync('aura-onboarding-completed'),
        ]);

        if (!active) return;

        const completed = onboardingStatus === 'true';
        setHasProfile(Boolean(storedName && storedName.trim()));
        setIsOnboardingCompleted(completed);
      } catch {
        if (active) {
          setHasProfile(false);
          setIsOnboardingCompleted(false);
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    hydrate();

    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#4dd0e1" />
        <Text style={styles.text}>Cargando configuración inicial…</Text>
      </View>
    );
  }

  if (!isOnboardingCompleted || !hasProfile) {
    return <Redirect href="/onboarding" />;
  }

  return <Redirect href="/(tabs)" />;
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#05070b',
    padding: 24,
  },
  text: {
    marginTop: 12,
    color: '#e5f6ff',
    fontSize: 14,
    fontFamily: 'Inter_500Medium',
  },
});
