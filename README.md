<img width="200" height="200" alt="image" src="https://github.com/user-attachments/assets/b83d1e02-269b-4e15-8054-80e606d3516b" />

# SPiCa Server

SPiCa는 **Netty** 프레임워크를 사용하여 구현된 간단한 **In-Memory Key-Value Store Server**입니다. <br/>
클라이언트는 TCP를 통해 접속하여 `PING`, `SET`, `GET`.. 명령어를 사용할 수 있습니다.

## Features

*   **PING**: 서버 상태 확인 (`PONG` 응답)
*   **SET**: 키-값 저장 (`SET key value`)
*   **GET**: 키로 값 조회 (`GET key`)
*   **In-Memory Storage**: `ConcurrentHashMap`을 사용한 Thread-safe한 데이터 저장

## Architecture (Component Specifications)

### Thread Model (Event Loop Groups)
시스템의 스레드 모델은 역할에 따라 물리적으로 격리된 두 개의 `EventLoopGroup`으로 구성됩니다.

* **Acceptor Group (Boss):**
    * 단일 스레드(혹은 설정된 소수)로 구성되며 `OP_ACCEPT` 이벤트만을 전담합니다.
    * TCP 3-way Handshake를 완료한 `SocketChannel`을 생성하고, 즉시 Worker Group의 Selector에 등록(Register)하여 병목을 제거합니다.
* **I/O Processor Group (Worker):**
    * CPU 코어 수에 비례하여 할당된 스레드 풀입니다.
    * 할당된 Channel의 모든 `OP_READ`, `OP_WRITE` 이벤트를 **Single-Threaded Event Loop** 내에서 순차 처리하여, 불필요한 동기화(Lock) 비용을 제거합니다(Lock-free Architecture).

### Channel Pipeline (Responsibility Chain)
데이터 처리 로직은 **Interceptor Filter Pattern**의 변형인 `ChannelPipeline`을 통해 계층화되어 있습니다. 각 핸들러는 단일 책임 원칙(SRP)에 따라 엄격히 분리됩니다.

* **Protocol Adaptation Layer (Inbound):**
    * `LineBasedFrameDecoder`: TCP의 스트림(Stream) 특성으로 인한 Packet Fragmentation/Coalescing 문제를 해결하기 위해, Delimiter(\n) 기반으로 바이트 스트림을 온전한 프레임으로 재조립합니다.
    * `StringDecoder`: 조립된 바이너리 프레임(`ByteBuf`)을 애플리케이션 레벨 객체(`String`)로 변환(Decoding)합니다.
* **Business Logic Layer:**
    * `PingPongHandler` / `CommandHandler`: 디코딩된 메시지를 수신하여 비즈니스 로직(Command Parsing, State Management)을 수행합니다. 필요 시 `EventExecutorGroup`을 통해 블로킹 작업을 별도 스레드로 격리(Offloading)합니다.
* **Transport Adaptation Layer (Outbound):**
    * `StringEncoder`: 응답 객체를 네트워크 전송을 위한 직렬화된 바이트 스트림으로 변환합니다.

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

## Data Flow Lifecycle

데이터의 흐름은 양방향(Duplex) 파이프라인을 통해 다음과 같은 상태 전이(State Transition)를 거칩니다.

1.  **Ingress (Connection & Read):**
    * Client Connection $\rightarrow$ Boss Group (Accept) $\rightarrow$ Worker Group (Registration).
    * Socket Read $\rightarrow$ **[Framing]** (Byte Stream assembly) $\rightarrow$ **[Decoding]** (Object instantiation).
2.  **Processing (Execution):**
    * Decoded Command $\rightarrow$ Handler Routing $\rightarrow$ Business Logic Execution (e.g., `ConcurrentHashMap` Access).
3.  **Egress (Write & Flush):**
    * Execution Result $\rightarrow$ `ChannelHandlerContext.write()` $\rightarrow$ **[Encoding]** (Serialization) $\rightarrow$ Socket Buffer Flush $\rightarrow$ Client.

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

---

updatedAt: 2025.12.2
