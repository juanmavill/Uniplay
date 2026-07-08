workspace "UniPlay" "Deployment architecture for the UniPlay real-time collaborative game platform." {

  model {
    user = person "Player" "Uses UniPlay from a web browser to create rooms, join games, draw, guess and use voice chat."

    softwareSystem = softwareSystem "UniPlay" "Real-time collaborative game platform for university communities." {
      frontend = container "Frontend" "React SPA served by Nginx. Provides lobby, game canvas, chat and voice controls." "React, Vite, Nginx"
      apiGateway = container "API Gateway" "Public backend entrypoint. Handles CORS, routing and rate limiting." "Spring Boot"
      roomService = container "Room Service" "Creates rooms and manages players in each room." "Spring Boot"
      gameService = container "Game Service" "Manages rounds, words, answers, scoring and game state." "Spring Boot"
      realtimeService = container "Realtime Service" "Provides STOMP/SockJS WebSocket channels for drawing, rounds and voice events." "Spring Boot, STOMP, SockJS"
      metricsService = container "Metrics Service" "Consumes domain events and exposes KPIs." "Spring Boot"
      voiceService = container "Voice Service" "Issues LiveKit voice tokens and publishes voice state events." "Spring Boot"
      redis = container "Redis" "Shared infrastructure for persistence and Pub/Sub event delivery." "Redis 7.4"
      livekit = container "LiveKit" "Voice media server for room audio." "LiveKit Server"
    }

    user -> frontend "Uses" "HTTPS/HTTP"
    user -> livekit "Sends and receives voice media" "WebRTC"

    frontend -> apiGateway "Calls REST APIs and opens WebSocket endpoint" "HTTP, SockJS/STOMP"
    frontend -> livekit "Connects to voice room with token" "WebRTC"

    apiGateway -> roomService "Routes room requests" "HTTP"
    apiGateway -> gameService "Routes game requests" "HTTP"
    apiGateway -> realtimeService "Routes WebSocket handshake and STOMP traffic" "HTTP/WebSocket"
    apiGateway -> metricsService "Routes KPI requests" "HTTP"
    apiGateway -> voiceService "Routes voice token and mute requests" "HTTP"

    roomService -> redis "Stores room/player state" "Redis"
    gameService -> redis "Stores game sessions and publishes round events" "Redis, Pub/Sub"
    realtimeService -> redis "Subscribes to round and voice events" "Redis Pub/Sub"
    metricsService -> redis "Consumes domain events and stores KPI counters" "Redis, Pub/Sub"
    voiceService -> redis "Publishes voice state events" "Redis Pub/Sub"
    voiceService -> livekit "Issues room access tokens for" "LiveKit JWT"

    deploymentEnvironment "Local Docker" {
      deploymentNode "Developer workstation" "Local machine running Docker Desktop" "Windows, Docker" {
        deploymentNode "Docker network: uniplay" "Compose network for UniPlay services." "Docker Compose" {
          frontendInstance = containerInstance frontend {
            tags "Frontend"
            properties {
              "published port" "5173 -> 80"
              "container" "uniplay-frontend"
            }
          }

          gatewayInstance = containerInstance apiGateway {
            tags "Backend"
            properties {
              "published port" "8080 -> 8080"
              "container" "uniplay-api-gateway"
            }
          }

          roomInstance = containerInstance roomService {
            tags "Backend"
            properties {
              "published port" "8081 -> 8081"
              "container" "uniplay-room-service"
            }
          }

          gameInstance = containerInstance gameService {
            tags "Backend"
            properties {
              "published port" "8082 -> 8082"
              "container" "uniplay-game-service"
            }
          }

          realtimeInstance = containerInstance realtimeService {
            tags "Backend"
            properties {
              "published port" "8083 -> 8083"
              "container" "uniplay-realtime-service"
            }
          }

          metricsInstance = containerInstance metricsService {
            tags "Backend"
            properties {
              "published port" "8084 -> 8084"
              "container" "uniplay-metrics-service"
            }
          }

          voiceInstance = containerInstance voiceService {
            tags "Backend"
            properties {
              "published port" "8085 -> 8085"
              "container" "uniplay-voice-service"
            }
          }

          redisInstance = containerInstance redis {
            tags "Data"
            properties {
              "published port" "6379 -> 6379"
              "container" "uniplay-redis"
              "volume" "redis-data:/data"
            }
          }

          livekitInstance = containerInstance livekit {
            tags "Media"
            properties {
              "published ports" "7880-7881 TCP, 7882 UDP"
              "container" "uniplay-livekit"
            }
          }
        }
      }
    }
  }

  views {
    deployment softwareSystem "Local Docker" "LocalDockerDeployment" "Local Docker deployment for UniPlay." {
      include *
      autoLayout lr
    }

    styles {
      element "Person" {
        shape person
        background #0f8b8d
        color #ffffff
      }

      element "Frontend" {
        background #172026
        color #ffffff
      }

      element "Backend" {
        background #2f6f9f
        color #ffffff
      }

      element "Data" {
        shape cylinder
        background #c7472c
        color #ffffff
      }

      element "Media" {
        background #7b4f9d
        color #ffffff
      }
    }
  }
}
