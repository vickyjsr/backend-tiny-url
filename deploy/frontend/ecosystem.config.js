module.exports = {
  apps: [{
    name: 'tinyurl-frontend',
    script: 'server.js',
    cwd: '/opt/tinyurl/frontend/app',
    instances: 1,
    exec_mode: 'fork',
    env: {
      NODE_ENV: 'production',
      PORT: 3000
    },
    error_file: '/opt/tinyurl/frontend/logs/pm2-error.log',
    out_file: '/opt/tinyurl/frontend/logs/pm2-out.log',
    log_file: '/opt/tinyurl/frontend/logs/pm2-combined.log',
    time: true,
    autorestart: true,
    max_restarts: 10,
    min_uptime: '10s',
    watch: false,
    max_memory_restart: '500M'
  }]
};
