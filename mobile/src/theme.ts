/** Tokens del producto SmartVest. La web ya usa estos colores en tailwind `smart`. */

export const theme = {
  night: '#0f172a',
  signal: '#1e40af',
  pulse: '#3b82f6',
  paper: '#f8fafc',
  card: '#ffffff',
  inset: '#eef3f8',
  ink: '#0f172a',
  body: '#334155',
  meta: '#64748b',
  quiet: '#94a3b8',
  line: 'rgba(15, 23, 42, 0.08)',
  lineStrong: 'rgba(15, 23, 42, 0.16)',
  clear: '#166534',
  clearSoft: '#dcfce7',
  caution: '#a16207',
  cautionSoft: '#fef3c7',
  warn: '#c2410c',
  warnSoft: '#ffedd5',
  sos: '#b91c1c',
  sosSoft: '#fee2e2',
  ok: '#166534',
  okSoft: '#dcfce7',
  fail: '#991b1b',
  failSoft: '#fee2e2',
} as const;

export const space = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const radius = {
  input: 12,
  card: 20,
  track: 14,
  mark: 12,
} as const;
