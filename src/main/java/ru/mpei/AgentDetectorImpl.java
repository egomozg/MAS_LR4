package ru.mpei;

import jade.core.AID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jade.core.Agent;

public class AgentDetectorImpl implements AgentDetector {
    private final Agent agent;
    private final Map<String, AgentInfo> knownAgents = new ConcurrentHashMap<>();

    private int publishPort;
    private int discoverPort;
    private AID myAID;

    public AgentDetectorImpl(Agent agent) {
        this.agent = agent;
    }

    @Override
    public void startPublishing(AID aid, int port) {
        this.myAID = aid;
        this.publishPort = port;
        agent.addBehaviour(new PublishingBeh(agent, aid, port));
    }

    @Override
    public void startDiscovering(int port) {
        this.discoverPort = port;
        // Добавляем поведение для приёма пакетов
        agent.addBehaviour(new DiscoveringBeh(agent, port, knownAgents));
        // TODO
        // Можно добавить периодическое поведение для очистки устаревших агентов
//        agent.addBehaviour(new AgentsCleanupBehaviour(agent, knownAgents, 5000));
    }

    @Override
    public List<AID> getActiveAgents() {
        return knownAgents.values().stream()
                .filter(AgentInfo::isActive)
                .map(AgentInfo::getAID)
                .toList();
    }
}
