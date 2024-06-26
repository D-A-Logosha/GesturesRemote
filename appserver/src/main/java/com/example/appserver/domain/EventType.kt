package com.example.appserver.domain

enum class EventType {
    ServerStarting,
    ServerStarted,
    ServerStopping,
    ServerStopped,
    ManagerStarted,
    ManagerStopped,
    ClientConnected,
    ClientDisconnected,
    MessageReceived,
    MessageSent,
    Error
}
