<img width="200" height="200" alt="image" src="https://github.com/user-attachments/assets/b83d1e02-269b-4e15-8054-80e606d3516b" />

# SPiCa Server

SPiCa는 **Netty** 프레임워크를 사용하여 구현된 고성능 **In-Memory Key-Value Store Server**입니다. <br/>
클라이언트는 TCP를 통해 접속하여 `PING`, `SET`, `GET`, `DEL`, `MGET` 명령어를 사용할 수 있습니다.

## Features

*   **PING**: 서버 상태 확인 (`Pong` 응답)
*   **SET**: 키-값 저장 (`SET key value`)
*   **SET MATCH**: 조건부 저장 (`SET key newValue MATCH oldValue`)
*   **GET**: 키로 값 조회 (`GET key`)
*   **DEL**: 키 삭제 (`DEL key`)
*   **MGET**: 여러 키 동시 조회 (`MGET key1 key2 ...`)
*   **In-Memory Storage**: `ConcurrentHashMap`을 사용한 Thread-safe한 데이터 저장
*   **Memory Optimization**: `byte[]` 저장소와 ByteBuf 풀링으로 메모리 효율 극대화

## Performance

### Benchmark Results

| Command | Throughput | Latency (p50) | Latency (p99) |
|---------|------------|---------------|---------------|
| PING | 58,651 ops/sec | 0.063 ms | 0.144 ms |
| SET | 51,881 ops/sec | 0.073 ms | 0.154 ms |
| GET | 46,458 ops/sec | 0.074 ms | 0.211 ms |

*테스트 환경: 4 threads × 10,000 ops, JIT Warm-up 3회, TCP_NODELAY 적용*

### Optimization Techniques

*   **Memory Locality**: `String` 대신 `byte[]` 저장으로 61% 메모리 절감
*   **ByteBuf Pooling**: 상수 응답 재사용으로 Zero-Copy 전송
*   **Lazy Split**: 필요한 명령어에서만 문자열 분리 수행

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
* **Business Logic Layer:**
    * `CommandHandler`: 명령어를 파싱하고 적절한 핸들러로 라우팅합니다.
    * `SetHandler`, `GetHandler`, `DeleteHandler`, `MultiGetHandler`: 각 명령어별 비즈니스 로직을 수행합니다.
* **Response Layer:**
    * `Responses`: 상수 ByteBuf를 재사용하여 Zero-Copy 응답을 제공합니다.

### Virtual Threads (Planned)
Java 24의 Virtual Threads를 활용한 하이브리드 아키텍처를 계획 중입니다.

* **Fast Path**: PING, GET, SET, DEL 등 빠른 명령어는 Netty Event Loop에서 직접 처리
* **Slow Path**: SAVE, EVAL 등 블로킹 작업은 Virtual Thread에서 처리하여 다른 요청에 영향 없음

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
                H2["2. CommandHandler\n(Parse & Route)"]:::handler
            end

            %% Handlers
            subgraph LOGIC ["Business Logic"]
                H3["SetHandler\nGetHandler\nDeleteHandler\nMultiGetHandler"]:::handler
            end
        end

        %% Flow connections
        Client <==>|TCP/IP| SOCKET
        SOCKET ==>|ByteBuf Stream| H1
        H1 ==>|Frame| H2
        H2 ==>|Dispatch| H3
        
        H3 -. "Responses.send()" .-> SOCKET
        
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
    participant H as CommandHandler
    participant M as DataStore (byte[])

    C->>S: Connect (TCP Handshake)
    
    Note over C, S: PING Command
    C->>S: "PING\n"
    S->>H: channelRead0(ByteBuf)
    H-->>S: Responses.pong(ctx)
    S-->>C: "Pong\n"
    
    Note over C, S: SET Command
    C->>S: "SET mykey hello\n"
    S->>H: channelRead0(ByteBuf)
    H->>M: putIfAbsent("mykey", bytes)
    H-->>S: Responses.ok(ctx)
    S-->>C: "OK\n"
    
    Note over C, S: GET Command
    C->>S: "GET mykey\n"
    S->>H: channelRead0(ByteBuf)
    H->>M: get("mykey")
    M-->>H: byte[]
    H-->>S: Responses.send(ctx, "value: hello\n")
    S-->>C: "value: hello\n"
    
    Note over C, S: DEL Command
    C->>S: "DEL mykey\n"
    S->>H: channelRead0(ByteBuf)
    H->>M: remove("mykey")
    H-->>S: Responses.ok(ctx)
    S-->>C: "OK\n"
```

## Data Flow Lifecycle

데이터의 흐름은 양방향(Duplex) 파이프라인을 통해 다음과 같은 상태 전이(State Transition)를 거칩니다.

1.  **Ingress (Connection & Read):**
    * Client Connection $\rightarrow$ Boss Group (Accept) $\rightarrow$ Worker Group (Registration).
    * Socket Read $\rightarrow$ **[Framing]** (Byte Stream assembly) $\rightarrow$ CommandHandler.
2.  **Processing (Execution):**
    * Decoded Command $\rightarrow$ Handler Routing $\rightarrow$ Business Logic Execution (e.g., `ConcurrentHashMap` Access).
3.  **Egress (Write & Flush):**
    * Execution Result $\rightarrow$ `Responses.send()` $\rightarrow$ ByteBuf (Pooled/Constant) $\rightarrow$ Socket Buffer Flush $\rightarrow$ Client.

## Project Structure

```
src/main/java/com/spica/
├── Application.java              # Entry Point
├── server/
│   ├── Server.java               # Server Interface
│   ├── ServerConfiguration.java  # Configuration (port, threads)
│   └── NettyServer.java          # Netty Bootstrap & Pipeline
└── handler/
    ├── Responses.java            # ByteBuf 상수 & 풀링 유틸리티
    ├── CommandHandler.java       # 명령어 파싱 & 라우팅
    ├── SetHandler.java           # SET 명령어 처리
    ├── GetHandler.java           # GET 명령어 처리
    ├── DeleteHandler.java        # DEL 명령어 처리
    ├── MultiGetHandler.java      # MGET 명령어 처리
    ├── PingPongHandler.java      # PING 명령어 처리
    └── SleepHandler.java         # SLEEP 명령어 처리 (테스트용)
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
echo "PING" | nc localhost 6379
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
# 응답: value: myvalue
```

**4. Delete Test**
```bash
echo "DEL mykey" | nc localhost 6379
# 응답: OK
```

**5. Multi-Get Test**
```bash
echo "MGET key1 key2 key3" | nc localhost 6379
# 응답: 각 키별 결과
```

---

updatedAt: 2025.12.14
