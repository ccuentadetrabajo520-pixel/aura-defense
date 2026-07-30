import { useColorScheme } from 'react-native';
import colors from '@/constants/colors';

/**
 * Returns the design tokens for the current color scheme.
 * Always returns the dark cyberpunk palette because app.json forces
 * "userInterfaceStyle": "dark". Both light and dark keys share the
 * same tokens so the result is identical either way.
 */
export function useColors() {
  const scheme = useColorScheme();
  // Direct key access — no unsafe cast, TS infers the correct palette type
  const palette = scheme === 'dark' ? colors.dark : colors.light;
  return { ...palette, radius: colors.radius };
}
