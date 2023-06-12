package com.kynl.ledcube.myinterface;

import com.kynl.ledcube.common.CommonUtils.ServerState;

public interface OnServerResponseListener {
    void onServerResponse(ServerState serverState, String message);
}
