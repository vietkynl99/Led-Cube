package com.kynl.ledcube.myinterface;

import com.kynl.ledcube.manager.ServerManager;

public interface OnServerStatusChangedListener {
    void onServerStateChanged(ServerManager.ServerState serverState, ServerManager.ConnectionState connectionState);
}
