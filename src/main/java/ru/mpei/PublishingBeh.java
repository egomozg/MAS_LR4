package ru.mpei;

import com.fasterxml.jackson.core.JsonProcessingException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.pcap4j.core.PcapNetworkInterface;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class PublishingBeh extends TickerBehaviour {
    private final AID agentAID;
    private final int port;
    private RawUdpSocketClient rawClient;

    public PublishingBeh(Agent a, AID agentAID, int port) {
        super(a, 2000); // Раз в 2 секунды отправляем информацию
        this.agentAID = agentAID;
        this.port = port;
        try {
            // Инициализируем rawClient
            this.rawClient = new RawUdpSocketClient("127.0.0.1", port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onTick() {
        if (rawClient == null) return;

        // Формируем JSON с информацией об агенте
        AgentMessage msg = new AgentMessage();
        msg.setAgentName(agentAID.getName());
        msg.setGuid(true);


        ObjectMapper mapper = new ObjectMapper();
        byte[] data = null;
        try {
            data = mapper.writeValueAsBytes(msg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

//        System.out.println("Отправка данных: " + Arrays.toString(data));

        try {
            // Отправляем пакет
            rawClient.sendPacket(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        try {
//            DatagramSocket socket = new DatagramSocket(1500);
//            InetAddress address = InetAddress.getByName("127.0.0.1");
//            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
//            socket.send(packet);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }
}
