import { GoogleGenAI } from "@google/genai";

// Helper to get the API key safely
const getApiKey = (): string | undefined => {
  return process.env.API_KEY;
};

export const verifyAddressWithGemini = async (address: string): Promise<string | null> => {
  const apiKey = getApiKey();
  if (!apiKey) {
    console.warn("API Key is missing. Skipping AI verification.");
    return null;
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: `Verifica si la siguiente dirección es válida y existe usando Google Maps. 
      Si existe, devuelve la dirección formateada y correcta. 
      Si es ambigua o no se encuentra, sugiere la más probable.
      
      Dirección: "${address}"
      
      Responde SOLAMENTE con la dirección corregida. Si no encuentras nada, responde "No encontrada".`,
      config: {
        tools: [{ googleMaps: {} }],
      },
    });

    // Check grounding chunks for specific map data if needed, but for now we take the text response
    // which should be the corrected address based on the prompt instructions.
    const text = response.text;
    
    if (text && !text.includes("No encontrada")) {
        return text.trim();
    }
    
    return null;

  } catch (error) {
    console.error("Error verifying address with Gemini:", error);
    return null;
  }
};
