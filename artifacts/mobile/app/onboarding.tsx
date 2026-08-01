import React, { useState } from 'react';
import {
  Alert,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import * as LocalAuthentication from 'expo-local-authentication';
import * as SecureStore from 'expo-secure-store';
import { useRouter } from 'expo-router';
import { useColors } from '@/hooks/useColors';
import { requestUsageAccessPermission, requestVpnPermission } from '@/hooks/usePermissions';

const steps = ['onboarding', 'permissions', 'terms'] as const;
type Step = (typeof steps)[number];

export default function OnboardingScreen() {
  const colors = useColors();
  const router = useRouter();
  const [name, setName] = useState('');
  const [step, setStep] = useState<Step>('onboarding');
  const [saving, setSaving] = useState(false);

  const nextAfterName = async () => {
    const nextName = name.trim() || 'Operador';
    setName(nextName);
    await SecureStore.setItemAsync('aura-user-name', nextName);
    setStep('permissions');
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

  const handleMissionAccept = async () => {
    setSaving(true);
    try {
      const biometricAvailable = await LocalAuthentication.getEnrolledLevelAsync();
      if (biometricAvailable !== LocalAuthentication.SecurityLevel.NONE) {
        const result = await LocalAuthentication.authenticateAsync({
          promptMessage: 'Confirma tu identidad para entrar a Aura Defense',
          fallbackLabel: 'Usar PIN',
        });
        if (!result.success) {
          Alert.alert('Biometría requerida', 'Debes verificar tu identidad para continuar.');
          return;
        }
      }

      await SecureStore.setItemAsync('aura-onboarding-completed', 'true');
      await SecureStore.setItemAsync('aura-user-name', (name || 'Operador').trim());
      router.replace('/(tabs)');
    } catch (error) {
      Alert.alert('No se pudo completar la misión', 'Hay un problema con la verificación del dispositivo.');
    } finally {
      setSaving(false);
    }
  };

  const renderStepContent = () => {
    if (step === 'onboarding') {
      return (
        <>
          <Text style={[styles.title, { color: colors.foreground }]}>ONBOARDING AURA</Text>
          <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>
            Introduce el nombre del operador para iniciar la misión y activar la protección en tiempo real.
          </Text>
          <TextInput
            value={name}
            onChangeText={setName}
            placeholder="Nombre del operador"
            placeholderTextColor={colors.mutedForeground}
            style={[styles.input, { color: colors.foreground, borderColor: colors.border }]}
            autoCapitalize="words"
          />
          <TouchableOpacity style={[styles.primaryButton, { backgroundColor: colors.primary }]} onPress={nextAfterName}>
            <Text style={styles.primaryButtonText}>Continuar</Text>
          </TouchableOpacity>
        </>
      );
    }

    if (step === 'permissions') {
      return (
        <>
          <Text style={[styles.title, { color: colors.foreground }]}>TOUR DE PERMISOS</Text>
          <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>Activa la configuración requerida para permitir el análisis de paquetes, uso de la aplicación y protección de la VPN.</Text>
          <View style={styles.permissionList}>
            <Text style={[styles.listText, { color: colors.foreground }]}>• Permiso de acceso de uso de la app</Text>
            <Text style={[styles.listText, { color: colors.foreground }]}>• Activación del túnel VPN</Text>
            <Text style={[styles.listText, { color: colors.foreground }]}>• Escaneo de red local y Wi‑Fi</Text>
          </View>
          <View style={styles.buttonStack}>
            <TouchableOpacity style={[styles.secondaryButton, { borderColor: colors.border }]} onPress={handleUsageAccessSetup}>
              <Text style={[styles.secondaryButtonText, { color: colors.foreground }]}>Configurar uso</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.secondaryButton, { borderColor: colors.border }]} onPress={handleVpnSetup}>
              <Text style={[styles.secondaryButtonText, { color: colors.foreground }]}>Preparar VPN</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[styles.primaryButton, { backgroundColor: colors.cyan }]} onPress={() => setStep('terms')}>
              <Text style={styles.primaryButtonText}>Continuar</Text>
            </TouchableOpacity>
          </View>
        </>
      );
    }

    return (
      <>
        <Text style={[styles.title, { color: colors.foreground }]}>TÉRMINOS Y CONDICIONES</Text>
        <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>
          Aura Defense recopila datos de seguridad local para identificar Wi‑Fi insegura, actividad sospechosa y cargas de análisis. El operador debe autorizar este monitoreo para continuar.
        </Text>
        <View style={[styles.termsBox, { borderColor: colors.border, backgroundColor: `${colors.card}AA` }]}> 
          <Text style={[styles.listText, { color: colors.foreground }]}>• Se trata de un escaneo local y privado del entorno.</Text>
          <Text style={[styles.listText, { color: colors.foreground }]}>• La VPN protege el tráfico del dispositivo.</Text>
          <Text style={[styles.listText, { color: colors.foreground }]}>• La autenticación biométrica confirma la identidad del operador.</Text>
        </View>
        <TouchableOpacity style={[styles.primaryButton, { backgroundColor: colors.primary }]} onPress={handleMissionAccept} disabled={saving}>
          <Text style={styles.primaryButtonText}>{saving ? 'Verificando…' : 'Aceptar misión'}</Text>
        </TouchableOpacity>
      </>
    );
  };

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]} contentContainerStyle={styles.content}>
      <View style={styles.card}>
        {renderStepContent()}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { flexGrow: 1, justifyContent: 'center', padding: 24 },
  card: {
    width: '100%',
    maxWidth: 420,
    borderRadius: 20,
    padding: 24,
    gap: 14,
    backgroundColor: '#101722',
    borderWidth: 1,
    borderColor: '#2f3b4d',
    alignSelf: 'center',
  },
  title: { fontSize: 22, fontFamily: 'Inter_700Bold', letterSpacing: 2 },
  subtitle: { fontSize: 13, lineHeight: 20 },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginTop: 8,
  },
  permissionList: { gap: 8 },
  listText: { fontSize: 12, lineHeight: 20 },
  termsBox: { borderWidth: 1, borderRadius: 12, padding: 12, gap: 8 },
  buttonStack: { gap: 10 },
  primaryButton: {
    borderRadius: 12,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryButtonText: { color: '#06131b', fontWeight: '700', letterSpacing: 1 },
  secondaryButton: {
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 10,
    alignItems: 'center',
  },
  secondaryButtonText: { fontWeight: '600' },
});
