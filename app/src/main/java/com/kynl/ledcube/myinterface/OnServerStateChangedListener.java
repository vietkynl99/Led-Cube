package com.kynl.ledcube.myinterface;

import com.kynl.ledcube.common.CommonUtils.ServerState;

public interface OnServerStateChangedListener {
    void onServerStateChanged(ServerState serverState);
}
