import { StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';
import { radius, theme } from './theme';

/** Mismos trazos del membrete web: chaleco + señal. */
export function VestMark({ size = 36 }: { size?: number }) {
  return (
    <View style={[styles.mark, { width: size, height: size, borderRadius: radius.mark }]}>
      <Svg viewBox="0 0 100 100" width={size - 10} height={size - 10}>
        <Path
          d="M25 25 L25 15 Q50 45 75 15 L75 25 Q85 50 85 60 L85 85 Q50 92 15 85 L15 60 Q15 50 25 25 Z"
          fill="none"
          stroke={theme.night}
          strokeWidth={8}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <Path d="M35 50 Q50 38 65 50" fill="none" stroke={theme.signal} strokeWidth={7} strokeLinecap="round" />
        <Path d="M42 62 Q50 55 58 62" fill="none" stroke={theme.signal} strokeWidth={7} strokeLinecap="round" />
        <Circle cx={50} cy={74} r={5} fill={theme.signal} />
      </Svg>
    </View>
  );
}

/** Umbrales reales del firmware: 40, 100 y 200 cm. */
export function proximityZone(cm: number): { label: string; color: string; soft: string; fill: number } {
  if (cm <= 40) {
    return { label: 'Peligro · hasta 40 cm', color: theme.sos, soft: theme.sosSoft, fill: 1 };
  }
  if (cm <= 100) {
    return { label: 'Alerta · hasta 100 cm', color: theme.warn, soft: theme.warnSoft, fill: 0.72 };
  }
  if (cm <= 200) {
    return { label: 'Precaución · hasta 200 cm', color: theme.caution, soft: theme.cautionSoft, fill: 0.4 };
  }
  return { label: 'Despejado · más de 200 cm', color: theme.clear, soft: theme.clearSoft, fill: 0.12 };
}

export function ProximityBand({
  distanceCm,
  sosActive,
}: {
  distanceCm: number | null;
  sosActive: boolean;
}) {
  if (sosActive) {
    return (
      <View style={[styles.band, { backgroundColor: theme.sosSoft }]}>
        <Text style={[styles.bandLabel, { color: theme.sos }]}>SOS activo</Text>
        <View style={styles.track}>
          <View style={[styles.fill, { width: '100%', backgroundColor: theme.sos }]} />
        </View>
      </View>
    );
  }

  if (distanceCm == null) {
    return (
      <View style={styles.band}>
        <Text style={styles.bandMuted}>Distancia sin dato del chaleco</Text>
      </View>
    );
  }

  const zone = proximityZone(distanceCm);
  return (
    <View style={[styles.band, { backgroundColor: zone.soft }]}>
      <Text style={[styles.hero, { color: zone.color }]}>{Math.round(distanceCm)}</Text>
      <Text style={[styles.unit, { color: zone.color }]}>cm al obstáculo</Text>
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${Math.round(zone.fill * 100)}%`, backgroundColor: zone.color }]} />
      </View>
      <Text style={[styles.bandLabel, { color: zone.color }]}>{zone.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  mark: {
    backgroundColor: '#ffffff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  band: {
    borderRadius: radius.card,
    padding: 16,
    gap: 4,
    backgroundColor: theme.inset,
  },
  hero: {
    fontSize: 56,
    fontWeight: '700',
    letterSpacing: -1,
    fontVariant: ['tabular-nums'],
  },
  unit: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
  },
  track: {
    height: 8,
    borderRadius: 4,
    backgroundColor: 'rgba(15, 23, 42, 0.08)',
    overflow: 'hidden',
  },
  fill: {
    height: 8,
    borderRadius: 4,
  },
  bandLabel: {
    marginTop: 8,
    fontSize: 14,
    fontWeight: '600',
  },
  bandMuted: {
    color: theme.meta,
    fontSize: 15,
  },
});
