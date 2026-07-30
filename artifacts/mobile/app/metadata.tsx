import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as ImageManipulator from 'expo-image-manipulator';
import { useColors } from '@/hooks/useColors';

export default function MetadataScreen() {
  const colors = useColors();
  const [status, setStatus] = useState('Selecciona una imagen local para limpiar EXIF.');

  const cleanImage = async () => {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      Alert.alert('Permiso requerido', 'Necesitas conceder acceso a la galería para limpiar metadatos.');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ImagePicker.MediaTypeOptions.Images, quality: 1 });
    if (result.canceled) return;

    const asset = result.assets[0];
    setStatus('Limpiando metadatos EXIF…');
    try {
      const manipulated = await ImageManipulator.manipulateAsync(asset.uri, [], { compress: 0.9, format: ImageManipulator.SaveFormat.JPEG, base64: false });
      setStatus(`Imagen procesada correctamente: ${manipulated.uri.split('/').pop()}`);
    } catch (error) {
      setStatus('No se pudo limpiar la imagen.');
    }
  };

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]} contentContainerStyle={styles.content}>
      <Text style={[styles.title, { color: colors.foreground }]}>LIMPIEZA DE METADATOS EXIF</Text>
      <Text style={[styles.subtitle, { color: colors.mutedForeground }]}>Procesa imágenes locales y elimina metadatos sensibles sin simulaciones.</Text>
      <TouchableOpacity style={[styles.button, { backgroundColor: colors.primary }]} onPress={cleanImage}>
        <Text style={styles.buttonText}>Seleccionar imagen local</Text>
      </TouchableOpacity>
      <View style={[styles.panel, { borderColor: colors.border, backgroundColor: colors.card }]}> 
        <Text style={[styles.panelTitle, { color: colors.foreground }]}>ESTADO</Text>
        <Text style={[styles.statusText, { color: colors.mutedForeground }]}>{status}</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 20, gap: 16 },
  title: { fontSize: 22, fontFamily: 'Inter_700Bold', letterSpacing: 2 },
  subtitle: { fontSize: 12, lineHeight: 18 },
  button: { paddingVertical: 12, borderRadius: 10, alignItems: 'center' },
  buttonText: { color: '#fff', fontFamily: 'Inter_700Bold', letterSpacing: 1.2 },
  panel: { borderWidth: 1, borderRadius: 14, padding: 14, gap: 8 },
  panelTitle: { fontSize: 13, fontFamily: 'Inter_700Bold', letterSpacing: 1.4 },
  statusText: { fontSize: 12, lineHeight: 18 },
});
