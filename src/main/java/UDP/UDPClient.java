package UDP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class UDPClient {
    private static SocketAddress routerAddress;
    private static InetSocketAddress serverAddress;
    private static InetSocketAddress clientAddress;
    private boolean connection_established = false;
    private long sequence_number;

    private void runClient() throws IOException {
        try(DatagramChannel channel = DatagramChannel.open()){
            channel.bind(clientAddress);

            if(!connection_established)
                handShake(channel);

            if(connection_established){
                //This section is to test GET, to test POST comment this section and uncomment Post section
                //Get contents of /docs

                String msg = "/docs/textfile.txt";
                sequence_number++;
                Packet p = createPacket(msg, PacketType.DATA.getValue());
                channel.send(p.toBuffer(), routerAddress);
                Packet resp = receivePacket(channel);
                StringBuilder response = new StringBuilder();

                while(resp.getType() != PacketType.FIN.getValue()){
                    String server_response = new String(resp.getPayload(), UTF_8);
                    //System.out.println("\n\nPacket payload is :\n" + server_response);

                    response.append(server_response);
                    ack_packet(resp.getSequenceNumber(), channel);
                    resp = receivePacket(channel);
                }

                System.out.println("\n---------Server response for Get file contents-----------\n" + response);


                //This section is to test POST, to test GET comment this section and uncomment GET section
/**
                //String msg = "Post new content to the file";
                String msg = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum interdum, risus at dictum sodales, odio elit consequat tortor, sit amet dignissim ligula eros sed elit. Phasellus id felis a lorem placerat lobortis. Vivamus ultrices quam vitae ipsum semper laoreet. Nam suscipit magna quis tellus accumsan, a dapibus dui vehicula. Cras laoreet fringilla sem id pellentesque. Donec eget tellus scelerisque, convallis massa nec, dignissim ex. Pellentesque venenatis, lacus sed pretium elementum, odio libero pretium massa, eget aliquet felis tortor vitae leo. Duis et venenatis nisl, eget fringilla turpis. Fusce sagittis massa at malesuada consequat.\n" +
                        "\n" +
                        "Duis urna lorem, vehicula ac tellus eu, mattis consectetur massa. Nulla facilisi. Sed et fringilla eros. Phasellus quis lacinia eros. Praesent pharetra eu turpis sed tincidunt. Curabitur semper malesuada purus sed egestas. Cras euismod lobortis dui, eget tincidunt dui dignissim in. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Pellentesque ullamcorper sed elit nec dapibus. Cras condimentum urna vitae fermentum finibus. Sed elit leo, euismod id posuere vel, ultrices nec mi. Sed felis risus, dignissim in dolor ac, ultrices consequat mauris. Nam orci libero, rhoncus ut odio a, efficitur tincidunt enim. Aenean vehicula, nulla id placerat rhoncus, arcu dolor congue mi, nec rutrum nisi urna vitae risus. Etiam cursus vulputate dolor sit amet lobortis. Vivamus a dictum mi, eu tincidunt tortor.\n" +
                        "\n" +
                        "Cras ut dolor eu mi interdum tempor et at enim. In posuere orci at ultrices sagittis. Nullam id diam mattis, viverra elit a, pellentesque nisi. Fusce sollicitudin eget erat mattis egestas. Suspendisse bibendum non elit ac condimentum. Nam blandit placerat ante vitae fermentum. Cras sit amet finibus dui, eget hendrerit purus. Nunc rhoncus mattis elit at ultrices. Sed turpis dui, mattis non ligula sit amet, dapibus convallis quam. Maecenas eget erat quis diam sagittis lacinia. Nullam non ligula eu lacus efficitur dignissim. Ut vel dignissim ante. Donec non mi finibus, sodales felis dapibus, suscipit velit. Phasellus et magna auctor, venenatis neque at, rutrum massa." +
                        "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Maecenas nec pellentesque elit, sit amet venenatis lectus. Sed a sollicitudin diam, ut scelerisque velit. Etiam egestas tincidunt turpis laoreet venenatis. Duis convallis tempor lectus et feugiat. Vestibulum in mauris viverra, ullamcorper sapien id, sollicitudin augue. Aenean finibus egestas magna, tempus imperdiet lectus laoreet ac. Mauris et rhoncus massa. Phasellus ullamcorper ligula sit amet ligula ornare rutrum. Donec egestas risus neque, at pulvinar justo ornare at. Vivamus convallis rhoncus eros, sit amet placerat nibh sagittis eget.\n" +
                        "\n" +
                        "Morbi molestie a sapien sed sollicitudin. Sed malesuada sit amet neque eget tincidunt. Morbi semper odio urna, ut faucibus velit pulvinar ut. Sed nulla magna, tempor quis egestas quis, laoreet non metus. Aenean ut mi mollis, dictum magna vitae, aliquam dui. Nunc in urna scelerisque, tincidunt turpis quis, sollicitudin nunc. Suspendisse non turpis ac ex imperdiet elementum et sit amet magna. Suspendisse aliquam dapibus orci, sed porttitor orci pretium vitae. Donec sit amet condimentum velit.";
                sequence_number++;

                byte[] responseBytes = msg.getBytes(UTF_8);
                System.out.println("Length of Post message " + responseBytes.length);

                if(msg.getBytes().length <= (Packet.MAX_LEN-11)) {
                    Packet resp = createPacket(msg, PacketType.DATA.getValue());
                    System.out.println("\nSending Packet with Seq # :" + resp.getSequenceNumber());
                    channel.send(resp.toBuffer(), routerAddress);
                    Packet new_packet = receivePacket(channel);
                    if(new_packet.getType() == PacketType.ACK.getValue())
                        System.out.println(" \nSeq # of received packet : " + new_packet.getSequenceNumber());

                    sequence_number++;
                    Packet fin_packet = createPacket("", PacketType.FIN.getValue());
                    channel.send(fin_packet.toBuffer(), routerAddress);
                }

                else{
                    List<byte[]> payloads = getPayloads(responseBytes, (Packet.MAX_LEN-11));

                    for (byte[] payload : payloads) {
                        String payload_data = new String(payload, UTF_8);
                        Packet resp = createPacket(payload_data, PacketType.DATA.getValue());
                        System.out.println("\nSending Packet with Seq # :" + resp.getSequenceNumber());
                        channel.send(resp.toBuffer(), routerAddress);

                        Packet new_packet = receivePacket(channel);
                        if(new_packet.getType() == PacketType.ACK.getValue())
                            System.out.println(" \nSeq # of received packet : " + new_packet.getSequenceNumber());

                        sequence_number++;
                    }

                    Packet fin_packet = createPacket("", PacketType.FIN.getValue());
                    channel.send(fin_packet.toBuffer(), routerAddress);
                }
 */
            }
        }
    }

    public static void setRouter(String routerHost, int routerPort){
        routerAddress = new InetSocketAddress(routerHost, routerPort);
    }

    public static void setServer(String serverHost, int serverPort){
        serverAddress = new InetSocketAddress(serverHost, serverPort);
    }

    public static void setClientAddress(int clientPort){
        clientAddress = new InetSocketAddress(clientPort);
    }

    private Packet createPacket(String message, int type){
        Packet p = new Packet.Builder()
                .setType(type)
                .setSequenceNumber(sequence_number)
                .setPortNumber(serverAddress.getPort())
                .setPeerAddress(serverAddress.getAddress())
                .setPayload(message.getBytes())
                .create();
        return p;
    }

    private Packet receivePacket(DatagramChannel channel) throws IOException{

        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN).order(ByteOrder.BIG_ENDIAN);
        buf.clear();
        channel.receive(buf);

        // Parse a packet from the received raw data.
        buf.flip();
        Packet packet = Packet.fromBuffer(buf);
        buf.flip();

        sequence_number++;
        return packet;
    }

    public static List<byte[]> getPayloads(byte[] response, int payload_size) {

        List<byte[]> payloads = new ArrayList<byte[]>();
        int offset = 0;

        while (offset < response.length) {
            int end = Math.min(response.length, offset + payload_size);
            payloads.add(Arrays.copyOfRange(response, offset, end));
            offset += payload_size;
        }

        return payloads;
    }

    private void handShake(DatagramChannel channel) throws IOException {

        System.out.println("\n---------Establishing connection with server through 3-way handshake---------\n");

        //Handshake step 1

        String helloMessage = "Hi S";
        sequence_number = 1L;

        Packet packet1 = createPacket(helloMessage, PacketType.SYN.getValue());

        System.out.println("Sending SYN message with sequence number " + sequence_number);
        channel.send(packet1.toBuffer(), routerAddress);

        Packet server_packet = receivePacket(channel);
        //Handshake step 3
        if(server_packet.getType() == PacketType.SYN_ACK.getValue()) {
            String payload = new String(server_packet.getPayload(), UTF_8);
            System.out.println("Server SYN_ACK response : " + payload);
            connection_established = true;
            sequence_number = server_packet.getSequenceNumber() + 1;
            ack_packet(server_packet.getSequenceNumber(), channel);
        }
    }

    private void ack_packet(long seq_no, DatagramChannel channel) throws IOException {
        System.out.println(" \nSeq # of received packet : " + seq_no);
        Packet ack_packet = createPacket(String.valueOf(seq_no), PacketType.ACK.getValue());
        channel.send(ack_packet.toBuffer(), routerAddress);
    }

    public static void main(String[] args) throws IOException {

        setRouter("localhost", 3000);
        setServer("localhost", 8007);
        setClientAddress(41830);
        new UDPClient().runClient();
    }
}

