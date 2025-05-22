FROM node:20-alpine

WORKDIR /app

COPY . .

RUN npm install
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

RUN npm install -g vite

EXPOSE 4173
CMD ["vite", "preview", "--host", "0.0.0.0"]
