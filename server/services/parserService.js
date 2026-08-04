const fs = require('fs');
const pdfParse = require('pdf-parse');
const mammoth = require('mammoth');

/**
 * Service khusus untuk melakukan parsing dan ekstraksi teks dari berbagai format file.
 * Mendukung format: PDF, DOCX, TXT, MD, CSV, JSON.
 */
class ParserService {
  /**
   * Mengekstrak teks dari file berdasarkan mimetype dan path file.
   * @param {string} filePath - Path file di server
   * @param {string} mimeType - Tipe MIME file
   * @param {string} originalName - Nama asli file
   * @returns {Promise<string>} Teks hasil ekstraksi
   */
  async extractTextFromFile(filePath, mimeType, originalName) {
    if (!fs.existsSync(filePath)) {
      throw new Error(`File tidak ditemukan di path: ${filePath}`);
    }

    const fileExtension = originalName.split('.').pop().toLowerCase();

    try {
      if (mimeType === 'application/pdf' || fileExtension === 'pdf') {
        return await this.parsePdf(filePath);
      } else if (
        mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
        fileExtension === 'docx'
      ) {
        return await this.parseDocx(filePath);
      } else if (
        mimeType.startsWith('text/') ||
        ['txt', 'md', 'csv', 'json', 'js', 'html', 'css', 'kt', 'java'].includes(fileExtension)
      ) {
        return await this.parsePlainText(filePath);
      } else {
        throw new Error(`Format file .${fileExtension} (${mimeType}) tidak didukung untuk ekstraksi teks.`);
      }
    } catch (error) {
      throw new Error(`Gagal mengekstrak teks dari file ${originalName}: ${error.message}`);
    }
  }

  /**
   * Parsing file PDF menggunakan pdf-parse
   */
  async parsePdf(filePath) {
    const dataBuffer = fs.readFileSync(filePath);
    const pdfData = await pdfParse(dataBuffer);
    return pdfData.text.trim();
  }

  /**
   * Parsing file Word DOCX menggunakan mammoth
   */
  async parseDocx(filePath) {
    const result = await mammoth.extractRawText({ path: filePath });
    return result.value.trim();
  }

  /**
   * Reading plain text files (TXT, MD, Code files, etc.)
   */
  async parsePlainText(filePath) {
    const content = fs.readFileSync(filePath, 'utf-8');
    return content.trim();
  }
}

module.exports = new ParserService();
