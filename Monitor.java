import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class Monitor {

    // ip -> disponibilidade
    private static final Map<String,Double> disponibilidade = new HashMap<>();
    // ip -> contador de eventos durante o intervalo
    private static final Map<String,Double> contador = new HashMap<>();
    // ip -> maximo dos eventos
    private static final Map<String,Double> max = new HashMap<>();
    // lock para leitura/escrita do contador
    private static final Object lock = new Object();

    public static final int PORT = 8881;
    public static final String DEFAULT_MULTICAST_IP = "224.0.0.2";
    public static final int DEFAULT_INTERVAL_MS = 1000;

    // Interface de rede usada para enviar e receber o multicast.
    // Sender e Receiver PRECISAM usar a mesma, senao o receive() bloqueia para sempre.
    // "en0" (mac wifi/ethernet), "eth0"/"wlan0" (linux).
    public static final String INTERFACE = "eth0";

    public static volatile boolean runThreads = true;

    public static void main(String[] args) {
        String multicastIP = DEFAULT_MULTICAST_IP;
        int interval = DEFAULT_INTERVAL_MS;
        boolean dynamicInterval = false;
        if (args.length == 2) {
            multicastIP = args[0];
            interval = Integer.parseInt(args[1]);
        } else if (args.length == 1) {
            multicastIP = args[0];
        } else if (args.length == 0) {
            dynamicInterval = true;
        } else {
            throw new RuntimeException("""
                                       Parâmetros incorretos. Esperado: ./monitor <multicast-ip> <interval>
                                       ./monitor <multicast-ip>
                                       ./monitor
                                       """);
        }
        Thread sender = new SenderThread(multicastIP, interval, dynamicInterval);
        Thread receiver = new ReceiverThread(multicastIP);
        Thread monitor = new MonitorThread(interval);
        sender.start();
        receiver.start();
        monitor.start();
        try {
            sender.join();
            receiver.join();
            monitor.join();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static class SenderThread extends Thread {

        String ipAddress;
        int interval;
        boolean dynamic;

        public SenderThread(String ipAddress, int interval, boolean dynamic) {
            this.ipAddress = ipAddress;
            this.interval = interval;
            this.dynamic = dynamic;
        }

        @Override
        public void run() {
            MulticastSocket socket;
            DatagramPacket outPacket;
            byte[] outBuf;

            try {
                socket = new MulticastSocket();
                socket.setOption(StandardSocketOptions.IP_MULTICAST_IF,
                                 NetworkInterface.getByName(INTERFACE));
                long counter = 0;
                String msg;

                while (runThreads) {
                    msg = "This is multicast! " + counter;
                    counter++;
                    outBuf = msg.getBytes();

                    InetAddress address = InetAddress.getByName(ipAddress);
                    outPacket = new DatagramPacket(outBuf, outBuf.length, address, PORT);

                    socket.send(outPacket);

                    handleInterval(interval, dynamic);
                }
            } catch (Exception ioe) {
                System.out.println("Sender error: " + ioe);
                runThreads = false;
            }
        }

        private void handleInterval(int interval, boolean dynamic) throws InterruptedException {
            // se intervalo dinamico, simula variação de disponibilidade
            // pausa um tempo aleatorio de 1ms ate o intervalo configurado
            int computedInterval = interval;
            if (dynamic) {
                computedInterval = ThreadLocalRandom.current().nextInt(1, interval + 1);
            }
            Thread.sleep(computedInterval);
        }

    }

    static class MonitorThread extends Thread {

        int interval;

        public MonitorThread(int interval) {
            this.interval = interval;
        }

        @Override
        public void run() {
            try {
                while (runThreads) {
                    // pausa primeiro para a primeira janela ser completa
                    Thread.sleep(interval);
                    synchronized(lock) {
                        for (Map.Entry<String,Double> keyValue : contador.entrySet()) {
                            String ip = keyValue.getKey();
                            double cont = keyValue.getValue();
                            double maxAtual = Math.max(max.getOrDefault(ip, 0.0), cont);
                            maxAtual = Math.max(maxAtual, 1.0);
                            max.put(ip, maxAtual);
                            disponibilidade.put(ip, cont/maxAtual);
                            System.out.println("IP=" + ip + ", " +
                                String.format("%.2f", disponibilidade.get(ip)*100) + "% " + 
                                "(max="+maxAtual+", atual="+cont+")");
                        }
                        contador.clear();
                    }
                }
            } catch (InterruptedException ie) {
                System.out.println("Monitor error: " + ie);
                runThreads = false;
            }
        }

    }

    static class ReceiverThread extends Thread {

        String ipAddress;

        public ReceiverThread(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        @Override
        public void run() {
            MulticastSocket socket;
            DatagramPacket inPacket;
            byte[] inBuf = new byte[256];
            try {
                socket = new MulticastSocket(PORT);
                InetAddress address = InetAddress.getByName(ipAddress);

                socket.joinGroup(new InetSocketAddress(address, PORT),
                                 NetworkInterface.getByName(INTERFACE));

                while (runThreads) {
                    inPacket = new DatagramPacket(inBuf, inBuf.length);
                    socket.receive(inPacket);
                    String msg = new String(inBuf, 0, inPacket.getLength());
                    String ip = inPacket.getAddress().getHostAddress();
                    synchronized(lock) {
                        double contadorAtual = contador.get(ip) == null ? 0 : contador.get(ip);
                        contador.put(ip, contadorAtual+1);
                    }

                }
            } catch (IOException ioe) {
                System.out.println("Receiver error: " + ioe);
                runThreads = false;
            }
        }
    }
}