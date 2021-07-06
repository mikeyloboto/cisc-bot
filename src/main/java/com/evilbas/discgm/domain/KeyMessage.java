package com.evilbas.discgm.domain;

public class KeyMessage {
    private Integer playerId;
    private String messageId;
    private KeyMessageType messageType;

    public KeyMessage(Integer playerId, String messageId, KeyMessageType messageType) {
        this.playerId = playerId;
        this.messageId = messageId;
        this.messageType = messageType;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public KeyMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(KeyMessageType messageType) {
        this.messageType = messageType;
    }
}
