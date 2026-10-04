import { StatusBar } from 'expo-status-bar';
import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  Linking,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { WebView } from 'react-native-webview';
import { API_BASE_URL } from './src/config';

// Reutiliza el sitio SmartVest completo servido por el mismo backend local.
// La URL sale del endpoint API existente para conservar la red ya configurada.
const SMARTVEST_WEB_URL = API_BASE_URL.replace(/\/api\/?$/, '/');
const SMARTVEST_ORIGIN = SMARTVEST_WEB_URL.match(/^https?:\/\/[^/]+/)?.[0] ?? '';

function isAllowedExternalUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return (
      url.protocol === 'tel:' ||
      (url.protocol === 'https:' &&
        (url.hostname === 'maps.google.com' || url.hostname === 'www.google.com'))
    );
  } catch {
    return false;
  }
}

export default function App() {
  const [reloadKey, setReloadKey] = useState(0);
  const [loadError, setLoadError] = useState(false);

  const openExternalUrl = useCallback(async (url: string) => {
    if (!isAllowedExternalUrl(url)) return;
    try {
      await Linking.openURL(url);
    } catch {
      // La app permanece abierta si Android no tiene una aplicación compatible.
    }
  }, []);

  return (
    <View style={styles.root}>
      <StatusBar style="dark" />
      <WebView
        key={reloadKey}
        source={{ uri: SMARTVEST_WEB_URL }}
        originWhitelist={SMARTVEST_ORIGIN ? [SMARTVEST_ORIGIN] : []}
        javaScriptEnabled
        domStorageEnabled
        cacheEnabled={false}
        mixedContentMode="never"
        setSupportMultipleWindows={false}
        onShouldStartLoadWithRequest={(request) => {
          if (!request.isTopFrame) return true;
          if (request.url.startsWith(SMARTVEST_WEB_URL)) return true;
          if (isAllowedExternalUrl(request.url)) {
            void openExternalUrl(request.url);
          }
          return false;
        }}
        onOpenWindow={(event) => {
          void openExternalUrl(event.nativeEvent.targetUrl);
        }}
        onLoadStart={() => setLoadError(false)}
        onError={() => setLoadError(true)}
        onHttpError={(event) => {
          if (event.nativeEvent.statusCode >= 400) setLoadError(true);
        }}
        startInLoadingState
        renderLoading={() => (
          <View style={styles.center}>
            <ActivityIndicator size="large" color="#2563eb" />
            <Text style={styles.message}>Abriendo SmartVest…</Text>
          </View>
        )}
      />
      {loadError && (
        <View style={styles.errorPanel}>
          <Text style={styles.errorTitle}>No se pudo abrir SmartVest</Text>
          <Text style={styles.message}>
            Verifica que el teléfono y el servidor estén conectados a la misma Wi‑Fi.
          </Text>
          <Pressable
            accessibilityRole="button"
            style={styles.retryButton}
            onPress={() => {
              setLoadError(false);
              setReloadKey((current) => current + 1);
            }}
          >
            <Text style={styles.retryText}>Volver a intentar</Text>
          </Pressable>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#f8fafc' },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f8fafc',
    padding: 24,
  },
  message: { marginTop: 12, color: '#475569', textAlign: 'center', fontSize: 15 },
  errorPanel: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f8fafc',
    padding: 28,
  },
  errorTitle: { color: '#0f172a', fontSize: 22, fontWeight: '700', textAlign: 'center' },
  retryButton: {
    marginTop: 22,
    borderRadius: 12,
    backgroundColor: '#1d4ed8',
    paddingHorizontal: 22,
    paddingVertical: 13,
  },
  retryText: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
