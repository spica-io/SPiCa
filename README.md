# SPiCa Server

**Netty** 프레임워크를 사용하여 구현된 간단한 **In-Memory Key-Value Store**. <br/>
클라이언트는 TCP를 통해 접속하여 `PING`, `SET`, `GET` 명령어를 사용할 수 있습니다.

## Features

*   **PING**: 서버 상태 확인 (`PONG` 응답)
*   **SET**: 키-값 저장 (`SET key value`)
*   **GET**: 키로 값 조회 (`GET key`)
*   **In-Memory Storage**: `ConcurrentHashMap`을 사용한 스레드 안전한 데이터 저장

## Architecture

Netty의 비동기 이벤트 기반 아키텍처를 따릅니다.

### Core Components

1.  **EventLoopGroup**:
    *   **Boss Group**: 클라이언트 연결 수락
    *   **Worker Group**: I/O 처리
2.  **ChannelPipeline**:
    *   `LineBasedFrameDecoder`: 패킷 분리
    *   `StringDecoder`: 바이트를 문자열로 변환
    *   `StringEncoder`: 문자열을 바이트로 변환
    *   `Handler`: 명령어 파싱 및 실행

### Data Flow

1.  **Request**: Client -> Boss Group -> Worker Group -> Pipeline -> Handler
2.  **Processing**: Handler에서 명령어 파싱 (`SET`, `GET`, `PING`) -> `ConcurrentHashMap` 조회/저장
3.  **Response**: Handler -> Pipeline (Encoder) -> Client

## Diagrams

### Server Structure 

```mermaid
graph TD
    %% --- [Nodes: External Actors] ---
    User["Developer / App Main"]:::actor
    Client["Remote TCP Client"]:::client

    %% --- [Subgraph: Server Context] ---
    subgraph APP ["Application Layer (JVM)"]
        direction TB
        
        %% Config Component
        CONFIG[("ServerConfiguration\n(Port, Threads, FrameLength)")]:::config
        
        %% Main Server Class
        subgraph NETTY_SERVER ["NettyServer Class"]
            direction TB
            START((start)):::method
            STOP((stop)):::method
            BLOCK((blockUntilClose)):::method
            
            BOOTSTRAP["ServerBootstrap\n(Factory)"]:::netty
        end
        
        %% Dependency Injection
        CONFIG -.->|Injects settings| NETTY_SERVER
        User -->|1. calls| START
    end

    %% --- [Subgraph: Netty Reactor Layer] ---
    subgraph REACTOR ["Netty Reactor Layer (Event Loops)"]
        direction TB
        
        BOSS["Boss Group\n(NioEventLoopGroup)\n[Acceptor Thread]"]:::boss
        WORKER["Worker Group\n(NioEventLoopGroup)\n[I/O Threads]"]:::worker
        
        START -->|2. init & bind| BOSS
        START -->|3. init| WORKER
        BOSS -->|4. accepts connection\n& registers| WORKER
        STOP -->|shutdownGracefully| BOSS & WORKER
    end

    %% --- [Subgraph: The Pipeline (Per Connection)] ---
    subgraph PIPELINE ["SocketChannel Pipeline (Runtime)"]
        direction TB
        
        SOCKET[("Socket Channel")]:::socket
        
        subgraph HANDLERS ["Handler Chain"]
            direction TB
            
            %% Inbound Flow
            subgraph INBOUND ["Inbound Flow (Read)"]
                H1["1. LineBasedFrameDecoder\n(ByteBuf -> ByteBuf [Framed])"]:::decoder
                H2["2. StringDecoder\n(ByteBuf -> String)"]:::decoder
                H3["4. PingPongHandler\n(Biz Logic)"]:::handler
            end

            %% Outbound Flow
            subgraph OUTBOUND ["Outbound Flow (Write)"]
                H4["3. StringEncoder\n(String -> ByteBuf)"]:::encoder
            end
        end

        %% Flow connections
        Client <==>|TCP/IP| SOCKET
        SOCKET ==>|ByteBuf Stream| H1
        H1 ==>|Frame| H2
        H2 ==>|String| H3
        
        %% [FIXED LINE] 점선 화살표 문법 수정
        H3 -. "ctx.write('Pong')" .-> H4
        H4 -. "Encoded Bytes" .-> SOCKET
        
        WORKER -.->|executes| HANDLERS
    end

    %% --- [Styling Definitions] ---
    classDef actor fill:#1a237e,stroke:#fff,stroke-width:2px,color:#fff
    classDef client fill:#b71c1c,stroke:#fff,stroke-width:2px,color:#fff
    classDef config fill:#fbc02d,stroke:#333,stroke-width:1px,color:#333
    classDef method fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,color:#333,stroke-dasharray: 5 5
    classDef netty fill:#e0f7fa,stroke:#006064,stroke-width:2px,color:#006064
    classDef boss fill:#3f51b5,stroke:#fff,stroke-width:2px,color:#fff
    classDef worker fill:#039be5,stroke:#fff,stroke-width:2px,color:#fff
    classDef socket fill:#4e342e,stroke:#fff,stroke-width:2px,color:#fff
    classDef decoder fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:#2e7d32
    classDef encoder fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#7b1fa2
    classDef handler fill:#212121,stroke:#00e676,stroke-width:3px,color:#fff
    
    linkStyle default stroke:#546e7a,stroke-width:2px,fill:none
```

### Sequence Diagram

```mermaid
sequenceDiagram
    participant C as Client
    participant S as Netty Server
    participant H as Handler
    participant M as DataStore

    C->>S: Connect (TCP Handshake)
    
    Note over C, S: PING Command
    C->>S: "Ping\n"
    S->>H: channelRead("Ping")
    H-->>S: writeAndFlush("Pong\n")
    S-->>C: "Pong\n"
    
    Note over C, S: SET Command
    C->>S: "SET mykey hello\n"
    S->>H: channelRead("SET mykey hello")
    H->>M: put("mykey", "hello")
    H-->>S: writeAndFlush("OK\n")
    S-->>C: "OK\n"
    
    Note over C, S: GET Command
    C->>S: "GET mykey\n"
    S->>H: channelRead("GET mykey")
    H->>M: get("mykey")
    M-->>H: "hello"
    H-->>S: writeAndFlush("hello\n")
    S-->>C: "hello\n"
```

## How to Run

### Prerequisites
- Java 24 or higher
- Gradle

### Run Server
```bash
./gradlew run
```
or
```bash
gradle run
```

### Client Test (netcat)

터미널을 열고 다음 명령어들을 입력하여 테스트할 수 있습니다.

**1. Ping Test**
```bash
echo "Ping" | nc localhost 6379
# 응답: Pong
```

**2. Set Test**
```bash
echo "SET mykey myvalue" | nc localhost 6379
# 응답: OK
```

**3. Get Test**
```bash
echo "GET mykey" | nc localhost 6379
# 응답: myvalue
```
