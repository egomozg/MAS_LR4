package ru.mpei;

import jade.core.AID;

public class AgentInfo {
    private final AID aid;
    private long lastUpdateTime;
    private final boolean isGuid;

    public AgentInfo(AID aid, boolean isGuid) {
        this.aid = aid;
        this.isGuid = isGuid;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public AID getAID() {
        return aid;
    }

    public boolean isGuid() {
        return isGuid;
    }

    public boolean isActive() {
        // Агент считается активным, если обновлялся недавно (например, не старше 10 сек)
        return System.currentTimeMillis() - lastUpdateTime < 10000;
    }

    public void updateTimestamp() {
        this.lastUpdateTime = System.currentTimeMillis();
    }
}
