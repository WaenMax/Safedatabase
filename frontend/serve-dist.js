import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, 'dist');
const port = Number(process.env.FRONTEND_PORT || 5173);

const contentTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
};

function send(res, status, body, type = 'text/plain; charset=utf-8') {
  res.writeHead(status, { 'Content-Type': type });
  res.end(body);
}

const server = http.createServer((req, res) => {
  const requestPath = decodeURIComponent((req.url || '/').split('?')[0]);
  const cleanPath = requestPath === '/' ? '/index.html' : requestPath;
  const filePath = path.normalize(path.join(root, cleanPath));

  if (!filePath.startsWith(root)) {
    send(res, 403, 'Forbidden');
    return;
  }

  fs.readFile(filePath, (error, data) => {
    if (!error) {
      send(res, 200, data, contentTypes[path.extname(filePath)] || 'application/octet-stream');
      return;
    }

    fs.readFile(path.join(root, 'index.html'), (fallbackError, fallback) => {
      if (fallbackError) {
        send(res, 404, 'dist/index.html not found. Run npm run build first.');
        return;
      }
      send(res, 200, fallback, contentTypes['.html']);
    });
  });
});

server.listen(port, '127.0.0.1', () => {
  console.log(`Frontend dist server listening on http://127.0.0.1:${port}`);
});
