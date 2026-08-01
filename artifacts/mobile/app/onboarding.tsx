import React, { useState } from 'react';
import { Alert, Button, Platform, StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from 'expo-router';
import { useColors } from '@/hooks/useColors';
import { requestUsageAccessPermission, requestVpnPermission } from '@/hooks/usePermissions';

export default function OnboardingScreen() {
  const colors = useColors();
  const router = useRouter();
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);

  const saveProfile = async () => {
    const nextName = name.trim() || 'Operador';
    setSaving(true);
    try {
      await AsyncStorage.setItem('aura-user-name', nextName);
      router.replace('/(tabs)');
    } finally {
      setSaving(false);
    }
  };

  const handleUsageAccessSetup = async () => {
    if (Platform.OS !== 'android') {
      Alert.alert('Android only', 'Usage access is Android-only.');
      return;
    }
    const granted = await requestUsageAccessPermission();
    Alert.alert(
      granted ? 'Usage access ready' : 'Manual step required',
      'Open Settings > Apps > Special app access > Usage access and allow Aura Defense.',
    );
  };

  const handleVpnSetup = async () => {
    if (Platform.OS !== 'android') {
      Alert.alert('Android only', 'VPN setup is Android-only.');
      return;
    }
    const prepared = await requestVpnPermission();
    Alert.alert(prepared ? 'VPN ready' : 'VPN permission required', 'Allow Aura Defense to configure the VPN profile from Android.');
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}> 
      <View style={styles.card}>
        <Text style={[styles.title, { color: colors.foreground }]}>ONBOARDING AURA</Text>
        <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>Introduce el nombre del operador para persistirlo localmente y acceder al panel de defensa.</Text>
        <TextInput
          value={name}
          onChangeText={setName}
          placeholder="Nombre del operador"
          placeholderTextColor={colors.mutedForeground}
          style={[styles.input, { color: colors.foreground, borderColor: colors.border }]}
          autoCapitalize="words"
        />
        <Button title={saving ? 'Guardando…' : 'Continuar'} onPress={saveProfile} disabled={saving} />
        <Button title="Configure usage access" onPress={handleUsageAccessSetup} />
        <Button title="Prepare VPN" onPress={handleVpnSetup} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  card: {
    width: '100%',
    maxWidth: 420,
    borderRadius: 20,
    padding: 24,
    gap: 12,
    backgroundColor: '#101722',
    borderWidth: 1,
    borderColor: '#2f3b4d',
  },
  title: {
    fontSize: 22,
    fontFamily: 'Inter_700Bold',
    letterSpacing: 2,
  },
  subtitle: {
    fontSize: 13,
    lineHeight: 20,
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginTop: 8,
  },
});
