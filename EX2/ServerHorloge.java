import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerHorloge {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(12345);
        System.out.println("Serveur démarré sur le port 12345...");

        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);

            long tPrime1 = System.currentTimeMillis();

            String messageRecu = new String(packet.getData(), 0, packet.getLength());
            long t1 = Long.parseLong(messageRecu);

            // Thread.sleep(10);

            long tPrime2 = System.currentTimeMillis();

            String reponse = t1 + ";" + tPrime1 + ";" + tPrime2;
            byte[] dataToSend = reponse.getBytes();

            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();

            DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, clientAddress, clientPort);

            socket.send(sendPacket);
            System.out.println("Sync effectuée avec " + clientAddress);
        }
    }
}