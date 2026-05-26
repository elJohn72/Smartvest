const getApiBasePath = (): string => {
  if (typeof window === 'undefined') {
    return '/api';
  }

  const { pathname } = window.location;

  if (pathname.includes('/dist/')) {
    return `${pathname.split('/dist/')[0]}/api`;
  }

  const [firstSegment] = pathname.split('/').filter(Boolean);
  return firstSegment ? `/${firstSegment}/api` : '/api';
};

export const verifyAddressWithGemini = async (address: string): Promise<string | null> => {
  try {
    const response = await fetch(`${getApiBasePath()}/address.php`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ address }),
    });

    const payload = await response.json();

    if (!response.ok || !payload.success || !payload.address) {
      return null;
    }

    return String(payload.address).trim();
  } catch (error) {
    console.error('Error verifying address:', error);
    return null;
  }
};
