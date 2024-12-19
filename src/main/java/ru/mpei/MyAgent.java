package ru.mpei;

public class MyAgent extends jade.core.Agent {
    private AgentDetector detector;
    private AgentDetector publisher1;
    private AgentDetector publisher2;

    @Override
    protected void setup() {
        detector = new AgentDetectorImpl(this);
        publisher1 = new AgentDetectorImpl(this);
        publisher2 = new AgentDetectorImpl(this);
        // Запускаем публикацию
        publisher1.startPublishing(getAID(), 9999);
        publisher2.startPublishing(getAID(), 9999);
        // Запускаем обнаружение
        detector.startDiscovering(9999);

    }
}
