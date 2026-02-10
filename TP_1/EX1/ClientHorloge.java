import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class ClientHorloge {
    public static void main(String args[]) throws Exception {
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String signal = "PING";
        sendData = signal.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 12345);
        clientSocket.send(sendPacket);
        System.out.println("Signal envoyé au serveur, attente de la réponse");

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("REÇU DU SERVEUR : " + serverResponse);

        clientSocket.close();
    }
}