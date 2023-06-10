package com.kynl.ledcube.myinterface;

import com.kynl.ledcube.manager.ServerManager;

import com.kynl.ledcube.common.CommonUtils.NetworkServiceState;
import com.kynl.ledcube.common.CommonUtils.ServerState;
import com.kynl.ledcube.common.CommonUtils.ConnectionState;

public interface OnServerStatusChangedListener {
    void onServerStateChanged(ServerState serverState, ConnectionState connectionState);
}
