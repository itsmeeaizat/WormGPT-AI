require('dotenv').config();
const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const cors = require('cors');

const uploadService = require('./services/uploadService');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Folder uploads sementara
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Konfigurasi Multer untuk menangani Multipart Upload
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    cb(null, `${uniqueSuffix}-${file.originalname}`);
  }
});

const upload = multer({
  storage: storage,
  limits: { fileSize: 25 * 1024 * 1024 } // Maksimal 25MB
});

// Map untuk menyimpan koneksi WebSocket berdasarkan clientId
const activeWsClients = new Map();

// Penanganan Koneksi WebSocket Real-time
wss.on('connection', (ws, req) => {
  const urlParams = new URLSearchParams(req.url.replace(/^.*\?/, ''));
  const clientId = urlParams.get('clientId') || `client_${Date.now()}`;

  activeWsClients.set(clientId, ws);
  console.log(`[WebSocket] Client terhubung: ${clientId}`);

  ws.send(
    JSON.stringify({
      event: 'connected',
      clientId: clientId,
      message: 'Koneksi WebSocket ke server AI Chat berhasil terhubung.'
    })
  );

  ws.on('close', () => {
    activeWsClients.delete(clientId);
    console.log(`[WebSocket] Client terputus: ${clientId}`);
  });

  ws.on('error', (err) => {
    console.error(`[WebSocket] Error client ${clientId}:`, err);
  });
});

/**
 * Endpoint POST /api/upload
 * Menerima file upload, langsung merespon HTTP 202 Accepted secara instan
 * tanpa menunggu parsing & AI selesai, lalu memasukkan file ke dalam background queue.
 */
app.post('/api/upload', upload.single('file'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: 'Tidak ada file yang diunggah.' });
    }

    const clientId = req.body.clientId || 'default_client';
    const userPrompt = req.body.prompt || '';
    const fileId = `file_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
    const wsClient = activeWsClients.get(clientId);

    // Memasukkan tugas ke dalam Background Queue (Asinkron)
    uploadService.enqueueJob({
      fileId: fileId,
      filePath: req.file.path,
      originalName: req.file.originalname,
      mimeType: req.file.mimetype,
      userPrompt: userPrompt,
      wsClient: wsClient
    });

    // Respon HTTP instan (Optimistic UI support)
    return res.status(202).json({
      success: true,
      message: 'File berhasil diterima dan sedang diproses di latar belakang.',
      data: {
        fileId: fileId,
        fileName: req.file.originalname,
        fileSize: req.file.size,
        status: 'PROCESSING'
      }
    });
  } catch (error) {
    console.error('[HTTP Upload Error]:', error);
    return res.status(500).json({ success: false, message: 'Gagal mengunggah file.', error: error.message });
  }
});

// Start Server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`=================================================`);
  console.log(` Server AI Chat File Upload berjalan di port ${PORT}`);
  console.log(` HTTP Endpoint : http://localhost:${PORT}/api/upload`);
  console.log(` WebSocket     : ws://localhost:${PORT}?clientId=YOUR_CLIENT_ID`);
  console.log(`=================================================`);
});
