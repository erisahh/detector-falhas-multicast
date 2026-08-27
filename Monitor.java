import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Monitor {

    // ip -> disponibilidade
    private static Map<String,Double> disponibilidade = new HashMap<>();
    // ip -> contador de eventos durante o intervalo
    private static Map<String,Double> contador = new HashMap<>();
    // ip -> maximo dos eventos
    private static Map<String,Double> max = new HashMap<>();

    public static final int PORT = 8881;
    public static void main(String[] args) {
        Thread sender = new Monitor.SenderThread();
        Thread receiver = new ReceiverThread();
        sender.start();
        receiver.start();
        try {
            sender.join();
            receiver.join();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static class SenderThread extends Thread {
        public void run() {
            DatagramSocket socket = null;
            DatagramPacket outPacket = null;
            byte[] outBuf;


            try {
                socket = new DatagramSocket();
                long counter = 0;
                String msg;

                while (true) {
                    msg = "This is multicast! " + counter;
                    counter++;
                    outBuf = msg.getBytes();

                    //Send to multicast IP address and port
                    //InetAddress address = InetAddress.getByName("224.0.0.1");
                    InetAddress address = InetAddress.getByName("224.0.0.2");
                    outPacket = new DatagramPacket(outBuf, outBuf.length, address, PORT);

                    socket.send(outPacket);
                    for (Map.Entry<String,Double> keyValue : contador.entrySet()) {
                        String ip = keyValue.getKey();
                        double cont = keyValue.getValue();
                        double maxAtual = cont > max.getOrDefault(ip, 0.0) ? cont : max.get(ip);
                        maxAtual = Math.max(maxAtual, 1.0);
                        max.put(ip, maxAtual);
                        disponibilidade.put(ip, cont/maxAtual);
                        System.out.println("Disponibilidade: IP=" + ip + ", %=" + disponibilidade.get(ip));
                    }

                    System.out.println("Server sends : " + msg);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                    }
                }
            } catch (IOException ioe) {
                System.out.println(ioe);
            }
        }
    }

    static class ReceiverThread extends Thread {
        public void run() {
            MulticastSocket socket = null;
            DatagramPacket inPacket = null;
            byte[] inBuf = new byte[256];
            try {
                //Prepare to join multicast group
                socket = new MulticastSocket(PORT);
                //InetAddress address = InetAddress.getByName("224.0.0.1");
                InetAddress address = InetAddress.getByName("224.0.0.2");

                socket.joinGroup(address);

                while (true) {
                    inPacket = new DatagramPacket(inBuf, inBuf.length);
                    socket.receive(inPacket);
                    String msg = new String(inBuf, 0, inPacket.getLength());
                    System.out.println("From " + inPacket.getAddress() + " Msg : " + msg);
                    String ip = inPacket.getAddress().getHostAddress();
                    double contadorAtual = contador.get(ip) == null ? 0 : contador.get(ip);
                    System.out.println(ip + " " + contadorAtual);
                    contador.put(ip, contadorAtual+1);

                }
            } catch (IOException ioe) {
                System.out.println(ioe);
            }
        }
    }
}
