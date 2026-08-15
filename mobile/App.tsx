import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { fetchIotState, iotStateUrl, type IotState } from './src/api';
import { API_BASE_URL, DEMO_DEVICE_ID, HOT_RELOAD_LABEL } from './src/config';

export default function App() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [state, setState] = useState<IotState | null>(null);
  const [httpOk, setHttpOk] = useState<boolean | null>(null);

  async function consultarChaleco() {
    setLoading(true);
    setError(null);
    setHttpOk(null);
    try {
      const result = await fetchIotState(DEMO_DEVICE_ID);
      setHttpOk(result.success);
      setState(result.data);
      if (!result.success) {
        setError(result.message || 'La API respondió success=false');
      }
    } catch (err) {
      setHttpOk(false);
      setState(null);
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView contentContainerStyle={styles.screen} accessibilityRole="summary">
      <StatusBar style="light" />
      <Text style={styles.kicker}>{HOT_RELOAD_LABEL}</Text>
      <Text style={styles.title} accessibilityRole="header">
        Telemetría del chaleco
      </Text>
      <Text style={styles.lede}>
        Cliente React Native + TypeScript del proyecto SmartVest. Consulta el
        backend PHP propio (asistencia a personas con discapacidad visual).
      </Text>

      <View style={styles.card}>
        <Text style={styles.label}>URL base (variable de entorno)</Text>
        <Text selectable style={styles.mono}>
          {API_BASE_URL}
        </Text>
        <Text style={styles.label}>Endpoint</Text>
        <Text selectable style={styles.mono}>
          {iotStateUrl(DEMO_DEVICE_ID)}
        </Text>
        <Text style={styles.hint}>
          En emulador Android usa 10.0.2.2. En este Mac no hay AVD (sin Android
          Studio); el destino es un dispositivo físico con Expo Go en la misma
          Wi-Fi.
        </Text>
      </View>

      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Consultar estado del chaleco de demostración"
        onPress={consultarChaleco}
        disabled={loading}
        style={({ pressed }) => [
          styles.button,
          pressed && styles.buttonPressed,
          loading && styles.buttonDisabled,
        ]}
      >
        {loading ? (
          <ActivityIndicator color="#0B1F33" />
        ) : (
          <Text style={styles.buttonText}>Consultar {DEMO_DEVICE_ID}</Text>
        )}
      </Pressable>

      {httpOk !== null ? (
        <Text style={httpOk ? styles.ok : styles.fail}>
          {httpOk ? 'Solicitud HTTP exitosa' : 'Solicitud fallida'}
        </Text>
      ) : null}

      {error ? <Text style={styles.fail}>{error}</Text> : null}

      {state ? (
        <View style={styles.card}>
          <Row label="deviceId" value={state.deviceId} />
          <Row
            label="distancia"
            value={state.distanceCm == null ? 'SIN_DATO' : `${state.distanceCm} cm`}
          />
          <Row label="SOS" value={state.sosActive ? 'ACTIVO' : 'inactivo'} />
          <Row
            label="batería"
            value={state.batteryLevel == null ? 'SIN_DATO' : `${state.batteryLevel}%`}
          />
          <Row
            label="última actualización"
            value={new Date(state.lastUpdate).toLocaleString('es-EC')}
          />
        </View>
      ) : null}
    </ScrollView>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={styles.rowValue}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flexGrow: 1,
    backgroundColor: '#0B1F33',
    paddingHorizontal: 20,
    paddingTop: 64,
    paddingBottom: 40,
    gap: 16,
  },
  kicker: {
    color: '#7DD3FC',
    fontSize: 16,
    fontWeight: '600',
  },
  title: {
    color: '#F8FAFC',
    fontSize: 28,
    fontWeight: '700',
  },
  lede: {
    color: '#CBD5E1',
    fontSize: 16,
    lineHeight: 24,
  },
  card: {
    backgroundColor: '#13283F',
    borderRadius: 12,
    padding: 16,
    gap: 8,
  },
  label: {
    color: '#94A3B8',
    fontSize: 13,
    textTransform: 'uppercase',
  },
  mono: {
    color: '#F8FAFC',
    fontSize: 14,
    fontFamily: 'Menlo',
  },
  hint: {
    color: '#94A3B8',
    fontSize: 14,
    lineHeight: 20,
    marginTop: 4,
  },
  button: {
    backgroundColor: '#38BDF8',
    minHeight: 52,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonPressed: {
    opacity: 0.85,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: '#0B1F33',
    fontSize: 18,
    fontWeight: '700',
  },
  ok: {
    color: '#86EFAC',
    fontSize: 16,
    fontWeight: '600',
  },
  fail: {
    color: '#FCA5A5',
    fontSize: 16,
    lineHeight: 22,
  },
  row: {
    gap: 2,
    marginBottom: 8,
  },
  rowLabel: {
    color: '#94A3B8',
    fontSize: 13,
  },
  rowValue: {
    color: '#F8FAFC',
    fontSize: 18,
  },
});
