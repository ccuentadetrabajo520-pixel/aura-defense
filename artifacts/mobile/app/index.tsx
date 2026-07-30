import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Redirect } from 'expo-router';

export default function EntryScreen() {
  const [loading, setLoading] = useState(true);
  const [hasProfile, setHasProfile] = useState(false);

  useEffect(() => {
    let active = true;
    AsyncStorage.getItem('aura-user-name')
      .then((value) => {
        if (!active) return;
        setHasProfile(Boolean(value && value.trim()));
        setLoading(false);
      })
      .catch(() => {
        if (active) {
          setHasProfile(false);
          setLoading(false);
        }
      });
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

  return hasProfile ? <Redirect href="/(tabs)" /> : <Redirect href="/onboarding" />;
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
