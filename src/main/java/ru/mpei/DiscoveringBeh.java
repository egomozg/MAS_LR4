package ru.mpei;

import jade.core.Agent;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.Behaviour;
import jade.core.AID;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;

public class DiscoveringBeh extends Behaviour {
    private volatile boolean finished = false;
    private final int port;
    private final Map<String, AgentInfo> knownAgents;
    private PcapHandle handle;

    public DiscoveringBeh(Agent agent, int port, Map<String, AgentInfo> knownAgents) {
        super(agent);
        this.port = port;
        this.knownAgents = knownAgents;
    }

    @Override
    public void action(){

    }

    @Override
    public void onStart() {
        try {
            // Открываем устройство для прослушивания (в данном случае loopback)
            // lo это чисто для линукса loopback interface, на windows не покатит
            PcapNetworkInterface nif = Pcaps.getDevByName("lo");
            if (nif == null) {
                System.err.println("Не найден loopback интерфейс");
                finished = true;
                return;
            }

            handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
            // Фильтр по порту: udp dst or src port {port}
            // юниксовый фильтр
            handle.setFilter("udp port " + port, BpfProgram.BpfCompileMode.OPTIMIZE);

            // Запускаем поток для чтения пакетов
            Thread listenThread = new Thread(() -> {
                try {
                    while (!finished) {
                        Packet packet = handle.getNextPacketEx();
                        if (packet != null) {
                            processPacket(packet);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listenThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            finished = true;
        }
    }

    private void processPacket(Packet packet) {
        // Извлекаем UDP payload
        UdpPacket udpPacket = packet.get(UdpPacket.class);
        if (udpPacket == null) return;

        byte[] payload = udpPacket.getPayload().getRawData();
        String jsonStr = new String(payload, StandardCharsets.UTF_8);

        try {
            ObjectMapper mapper = new ObjectMapper();
            AgentMessage msg = mapper.readValue(jsonStr, AgentMessage.class);

            String agentName = msg.getAgentName();
            boolean isGuid = msg.isGuid();

            AID aid = new AID(agentName, AID.ISGUID);
            knownAgents.compute(agentName, (k,v) -> {
                if (v == null) {
                    return new AgentInfo(aid, isGuid);
                } else {
                    v.updateTimestamp();
                    return v;
                }
            });
        } catch (Exception e) {
            // Некорректный пакет
        }
    }

    @Override
    public boolean done() {
        return finished;
    }

    @Override
    public int onEnd() {
        if (handle != null && handle.isOpen()) {
            handle.close();
        }
        return super.onEnd();
    }
}
