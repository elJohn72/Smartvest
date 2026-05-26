export const buildAppUrl = (query: Record<string, string>): string => {
  if (typeof window === 'undefined') {
    return '';
  }

  const origin = window.location.origin;
  const path = window.location.pathname;
  const cleanPath = path.endsWith('/') ? path.slice(0, -1) : path;
  const params = new URLSearchParams(query);

  return `${origin}${cleanPath}?${params.toString()}`;
};
