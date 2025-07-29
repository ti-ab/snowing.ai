FROM node:20.17.0

RUN npm install -g pnpm

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers package en premier pour tirer parti du cache Docker
COPY package.json pnpm-lock.yaml* ./

# Installer les dépendances
RUN pnpm install

# Copier le reste des fichiers (après les deps pour utiliser le cache efficacement)
COPY . .

# Construire le projet
RUN pnpm build

CMD ["sh", "-c", "node dist/agent.js $( [ \"$NODE_ENV\" = \"production\" ] && echo start || echo dev )"]
