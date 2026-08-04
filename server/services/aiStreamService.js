const { GoogleGenAI } = require('@google/genai');

/**
 * Service khusus untuk menangani logika AI Streaming ke Gemini API.
 * Langsung mengirim potongan teks (chunk) secara real-time ke client via WebSocket.
 */
class AiStreamService {
  constructor() {
    const apiKey = process.env.GEMINI_API_KEY || '';
    this.ai = new GoogleGenAI({ apiKey });
  }

  /**
   * Mengirimkan teks hasil ekstraksi file dan prompt user ke Gemini API secara streaming.
   * @param {Object} params
   * @param {string} params.prompt - Pertanyaan/instruksi dari user
   * @param {string} params.extractedText - Teks hasil parser file
   * @param {string} params.fileName - Nama file yang diunggah
   * @param {WebSocket} params.wsClient - Socket client untuk streaming
   * @param {string} params.fileId - ID unik file
   */
  async streamResponseWithContext({ prompt, extractedText, fileName, wsClient, fileId }) {
    try {
      const systemInstruction = `Anda adalah asisten AI cerdas. User telah mengunggah file bernama "${fileName}".
Berikut adalah isi teks dari file tersebut:
--- MULAI DOKUMEN ---
${extractedText}
--- AKHIR DOKUMEN ---
Jawab pertanyaan atau instruksi user berdasarkan isi dokumen di atas secara detail, akurat, dan jelas.`;

      const userMessage = prompt || `Tolong berikan ringkasan dan poin penting dari file ${fileName} ini.`;

      // Kirim event penanda bahwa AI mulai melakukan streaming
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'ai_stream_start',
            fileId: fileId,
            message: 'AI mulai membuat tanggapan streaming...'
          })
        );
      }

      // Memanggil Gemini API dengan fitur generateContentStream
      const responseStream = await this.ai.models.generateContentStream({
        model: 'gemini-2.5-flash',
        contents: [
          { role: 'user', parts: [{ text: `${systemInstruction}\n\nUser Prompt: ${userMessage}` }] }
        ]
      });

      let fullResponse = '';

      for await (const chunk of responseStream) {
        const textChunk = chunk.text;
        if (textChunk) {
          fullResponse += textChunk;
          if (wsClient && wsClient.readyState === 1) {
            wsClient.send(
              JSON.stringify({
                event: 'ai_stream_chunk',
                fileId: fileId,
                chunk: textChunk
              })
            );
          }
        }
      }

      // Kirim event penanda bahwa streaming AI selesai
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'ai_stream_complete',
            fileId: fileId,
            fullText: fullResponse
          })
        );
      }

      return fullResponse;
    } catch (error) {
      console.error('Error pada AiStreamService:', error);
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'ai_stream_error',
            fileId: fileId,
            error: `Gagal menghasilkan respon AI: ${error.message}`
          })
        );
      }
      throw error;
    }
  }
}

module.exports = new AiStreamService();
