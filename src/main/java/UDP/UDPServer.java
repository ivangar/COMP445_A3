package UDP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class UDPServer {
    private static SocketAddress routerAddress;
    private static InetSocketAddress serverAddress;
    private static InetSocketAddress clientAddress;

    private void listenAndServe() throws IOException {

        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.bind(serverAddress);
            System.out.println("Server is listening at " + channel.getLocalAddress());
            ByteBuffer buf = ByteBuffer
                    .allocate(Packet.MAX_LEN)
                    .order(ByteOrder.BIG_ENDIAN);

            for (; ; ) {
                Packet packet = receivePacket(buf, channel);

                if(packet.getType() == PacketType.SYN.getValue()) {
                    handShake(packet ,channel);
                }
                else{
                    String payload = new String(packet.getPayload(), UTF_8);
                    System.out.println("Packet: " + packet);
                    System.out.println("Payload: " + payload);
                    System.out.println("Router: " + routerAddress);

                    // Send the response to the routerAddress not the client.
                    // The peer address of the packet is the address of the client already.
                    // We can use toBuilder to copy properties of the current packet.
                    // This demonstrate how to create a new packet from an existing packet.
//                    Packet resp = createPacket(packet, payload, packet.getSequenceNumber()+1, PacketType.DATA.getValue());
//                    channel.send(resp.toBuffer(), routerAddress);

                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {

                        }
                    };
                    timer.schedule(task, 0, 2000);

                    Packet resp = createPacket(packet, payload, packet.getSequenceNumber()+1, PacketType.DATA.getValue());
                    channel.send(resp.toBuffer(), routerAddress);
                }

            }
        }
    }

    private void handShake(Packet packet, DatagramChannel channel) throws IOException {

        //Handshake step 2
        System.out.println("\n---------Establishing connection with client through 3-way handshake---------\n");
        String payload = new String(packet.getPayload(), UTF_8);
        System.out.println("Client message : " + payload);

        String responseMessage = ("Hi");
        long seq_no = packet.getSequenceNumber() + 1;
        Packet syn_ack_response = createPacket(packet, responseMessage, seq_no, PacketType.SYN_ACK.getValue());
        channel.send(syn_ack_response.toBuffer(), routerAddress);
        System.out.println("Sending SYN_ACK message: Hi\n ACK num: " + seq_no);

    }

    private Packet createPacket(Packet packet, String message, long sequence_number, int type){
        Packet p = packet.toBuilder()
                .setType(type)
                .setSequenceNumber(sequence_number)
                .setPayload(message.getBytes())
                .create();
        return p;
    }

    private Packet receivePacket(ByteBuffer buf, DatagramChannel channel) throws IOException{

        buf.clear();
        routerAddress = channel.receive(buf);

        // Parse a packet from the received raw data.
        buf.flip();
        Packet packet = Packet.fromBuffer(buf);
        buf.flip();

        return packet;
    }

    public static void setRouter(String routerHost, int routerPort){
        routerAddress = new InetSocketAddress(routerHost, routerPort);
    }

    public void setServer(int serverPort){
        serverAddress = new InetSocketAddress(serverPort);
    }

    public static void setClientAddress(int clientPort){
        clientAddress = new InetSocketAddress(clientPort);
    }

    public static void main(String[] args) throws IOException {
        UDPServer server = new UDPServer();
        server.setServer(8007);
        server.listenAndServe();
    }
}