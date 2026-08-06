const fs = require('fs');
const parserService = require('./parserService');
const aiStreamService = require('./aiStreamService');

/**
 * Service untuk mengelola antrean (queue) pemrosesan file secara asinkron.
 * Mencegah blocking pada HTTP response upload endpoint.
 */
class UploadService {
  constructor() {
    this.jobQueue = [];
    this.isProcessing = false;
  }

  /**
   * Menambahkan tugas pemrosesan file ke dalam antrean (Queue).
   * @param {Object} jobData
   * @param {string} jobData.fileId - ID unik file
   * @param {string} jobData.filePath - Path file sementara di disk
   * @param {string} jobData.originalName - Nama asli file
   * @param {string} jobData.mimeType - MIME type file
   * @param {string} jobData.userPrompt - Prompt opsional dari user
   * @param {WebSocket} jobData.wsClient - Socket WebSocket milik user
   */
  enqueueJob(jobData) {
    this.jobQueue.push(jobData);

    // Beritahu client via WebSocket bahwa file telah masuk antrean
    if (jobData.wsClient && jobData.wsClient.readyState === 1) {
      jobData.wsClient.send(
        JSON.stringify({
          event: 'file_queued',
          fileId: jobData.fileId,
          status: 'QUEUED',
          message: 'File berhasil diunggah dan masuk dalam antrean pemrosesan.'
        })
      );
    }

    // Jalankan worker pemroses antrean jika belum berjalan
    this.processNextJob();
  }

  /**
   * Worker asinkron untuk memproses antrean satu per satu.
   */
  async processNextJob() {
    if (this.isProcessing || this.jobQueue.length === 0) {
      return;
    }

    this.isProcessing = true;
    const currentJob = this.jobQueue.shift();
    const { fileId, filePath, originalName, mimeType, userPrompt, wsClient } = currentJob;

    try {
      // 1. Update status WebSocket: Sedang Mengekstrak / Parsing
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'file_processing',
            fileId: fileId,
            status: 'PARSING',
            message: 'Memproses dan mengekstrak teks dari file...'
          })
        );
      }

      // 2. Ekstraksi Teks Asinkron melalui ParserService
      const extractedText = await parserService.extractTextFromFile(filePath, mimeType, originalName);

      // 3. Update status WebSocket: Parsing Selesai
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'file_parsed',
            fileId: fileId,
            status: 'PARSED_SUCCESS',
            message: 'Teks file berhasil diekstrak. Mengirim ke model AI...',
            textPreview: extractedText.substring(0, 200) + (extractedText.length > 200 ? '...' : '')
          })
        );
      }

      // 4. Langsung teruskan potongan teks ke AI Stream Service
      await aiStreamService.streamResponseWithContext({
        prompt: userPrompt,
        extractedText: extractedText,
        fileName: originalName,
        wsClient: wsClient,
        fileId: fileId
      });

      // 5. Update status akhir
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'file_completed',
            fileId: fileId,
            status: 'COMPLETED',
            message: 'Seluruh alur pemrosesan file dan respon AI selesai.'
          })
        );
      }
    } catch (error) {
      console.error(`Gagal memproses job file ${fileId}:`, error);
      if (wsClient && wsClient.readyState === 1) {
        wsClient.send(
          JSON.stringify({
            event: 'file_error',
            fileId: fileId,
            status: 'ERROR',
            message: `Gagal memproses file: ${error.message}`
          })
        );
      }
    } finally {
      // Hapus file sementara jika ada
      if (fs.existsSync(filePath)) {
        try {
          fs.unlinkSync(filePath);
        } catch (_) {}
      }

      this.isProcessing = false;
      // Lanjutkan ke job berikutnya di queue jika masih ada
      setImmediate(() => this.processNextJob());
    }
  }
}

module.exports = new UploadService();
