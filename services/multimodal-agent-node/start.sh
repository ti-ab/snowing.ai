#!/bin/sh

if [ "$NODE_ENV" = "production" ]; then
  echo "🚀 Starting in production mode..."
  exec node dist/agent.js start
else
  echo "🛠 Starting in development mode..."
  exec node dist/agent.js dev
fi
