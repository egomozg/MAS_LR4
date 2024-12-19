package ru.mpei;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.packet.namednumber.UdpPort;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;

public class RawUdpSocketClient {
    private final PcapHandle sendHandle;
    private final InetAddress srcAddr;
    private final InetAddress dstAddr;
    private final int dstPort;
    private final int srcPort; // по-хорошему рандомный должен быть

    public RawUdpSocketClient(String dstHost, int dstPort) throws Exception {
        this.dstAddr = InetAddress.getByName(dstHost);
        this.dstPort = dstPort;
        this.srcAddr = InetAddress.getLoopbackAddress();
        this.srcPort = 1500; // условно

        PcapNetworkInterface nif = Pcaps.getDevByName("lo");
        if (nif == null) {
            throw new RuntimeException("Не найден loopback интерфейс");
        }

        sendHandle = nif.openLive(1500, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
    }

    public void sendPacket(byte[] data) throws Exception {
        // Формируем UDP пакет вручную

        IpV4Rfc791Tos tos = IpV4Rfc791Tos.newInstance((byte)0);

        UdpPacket.Builder udpBuilder = new UdpPacket.Builder()
                .srcPort(new UdpPort((short)srcPort, "srcPort"))
                .dstPort(new UdpPort((short)dstPort, "dstPort"))
                .srcAddr((Inet4Address)srcAddr)
                .dstAddr((Inet4Address)dstAddr)
                .payloadBuilder(new UnknownPacket.Builder().rawData(data))
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true);

        byte[] rawDataUDP = udpBuilder.build().getRawData();

        IpV4Packet.Builder ipv4Builder = new IpV4Packet.Builder()
                .version(IpVersion.IPV4)
                .tos(tos)
                .ttl((byte)128)
                .protocol(IpNumber.UDP)
                .srcAddr((Inet4Address)srcAddr)
                .dstAddr((Inet4Address)dstAddr)
                .payloadBuilder(udpBuilder)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true);

        byte[] rawDataIP = ipv4Builder.build().getRawData();

        // Получение MAC-адресов
        MacAddress srcMac = MacAddress.getByAddress(new byte[]{(byte)0x12,(byte) 0x34,(byte) 0x56,(byte) 0x78,(byte) 0x9a,(byte) 0xbc});
        MacAddress dstMac = MacAddress.getByAddress(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});

//        MacAddress srcMac = MacAddress.getByAddress(
//                NetworkInterface.getByInetAddress(srcAddr).getHardwareAddress()
//        );
//        MacAddress dstMac = MacAddress.ETHER_BROADCAST_ADDRESS;

        // Создание Ethernet-заголовка
        EthernetPacket ethernetPacket = new EthernetPacket.Builder()
                .srcAddr(srcMac)
                .dstAddr(dstMac)
                .type(EtherType.IPV4)
                .payloadBuilder(ipv4Builder)
                .pad(new byte[1])
                .build();

        System.out.println("UDP-заголовок: " + Arrays.toString(rawDataUDP));
        System.out.println("Полный пакет: " + Arrays.toString(rawDataIP));

        System.out.println("srcPort (short): " + (short) srcPort);
        System.out.println("dstPort (short): " + (short) dstPort);

        try{
            sendHandle.sendPacket(ethernetPacket);
        } catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
        }

    }
}
