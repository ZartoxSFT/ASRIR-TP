import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientHorloge {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        byte[] buffer = new byte[1024];

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

        long t1 = System.currentTimeMillis();
        byte[] dataToSend = String.valueOf(t1).getBytes();

        DatagramPacket sendPacket = new DatagramPacket(dataToSend, dataToSend.length, serverAddress, 12345);
        socket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivePacket);
        long t2 = System.currentTimeMillis();

        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        String[] temps = response.split(";");

        long tPrime1 = Long.parseLong(temps[1]);
        long tPrime2 = Long.parseLong(temps[2]);

        System.out.println("T1 : " + sdf.format(new Date(t1)));
        System.out.println("T'1 : " + sdf.format(new Date(tPrime1)));
        System.out.println("T'2 : " + sdf.format(new Date(tPrime2)));
        System.out.println("T2 : " + sdf.format(new Date(t2)));
        System.out.println("-------------------------------------");

        double delta = (t2 - t1) - (tPrime2 - tPrime1);
        double theta = ((tPrime1 + tPrime2) / 2.0) - ((t1 + t2) / 2.0);

        System.out.printf("δ : %.2f ms%n", delta);
        System.out.printf("θ : %.2f ms%n", theta);

        System.out.println("-------------------------------------");

        long heureCorrigeeLong = (long) (System.currentTimeMillis() + theta);
        String heureCorrigeeStr = sdf.format(new Date(heureCorrigeeLong));

        System.out.println("Corrected clock: " + heureCorrigeeStr);

        socket.close();
    }
}