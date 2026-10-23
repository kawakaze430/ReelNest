FROM node:22-alpine AS deps
WORKDIR /app
# Once package-lock.json is generated and reviewed, this uses npm ci.
# Bootstrap path supports the inherited repository which has no lockfile yet.
COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci --no-audit --no-fund; \
    else npm install --no-audit --no-fund; fi

FROM deps AS dev
COPY . .
EXPOSE 3000
CMD ["npm", "run", "dev"]

FROM deps AS builder
COPY . .
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_AUTHORITY
ARG NEXT_PUBLIC_OIDC_CLIENT_ID=reelnest-web
ARG NEXT_PUBLIC_APP_ORIGIN
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL} \
    NEXT_PUBLIC_AUTHORITY=${NEXT_PUBLIC_AUTHORITY} \
    NEXT_PUBLIC_OIDC_CLIENT_ID=${NEXT_PUBLIC_OIDC_CLIENT_ID} \
    NEXT_PUBLIC_APP_ORIGIN=${NEXT_PUBLIC_APP_ORIGIN}
# Protect published JS from accidentally embedding localhost, missing auth, or http:// URLs.
RUN node scripts/validate-public-env.mjs
RUN npm run build

FROM node:22-alpine AS production
ENV NODE_ENV=production PORT=3000 HOSTNAME=0.0.0.0
WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /app/.next/standalone ./
COPY --from=builder --chown=app:app /app/.next/static ./.next/static
COPY --from=builder --chown=app:app /app/public ./public
USER app
EXPOSE 3000
CMD ["node", "server.js"]
