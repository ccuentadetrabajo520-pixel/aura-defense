import React, { useState } from 'react';
import { Button, Platform, StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from 'expo-router';
import { useColors } from '@/hooks/useColors';

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
