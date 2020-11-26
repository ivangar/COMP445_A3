package UDP;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class UDPServer {
    private static SocketAddress routerAddress;
    private static InetSocketAddress serverAddress;
    private static InetSocketAddress clientAddress;
    private boolean connection_established = false;
    private Path root = Paths.get("").toAbsolutePath();  //default system current dir

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
                else if(connection_established){
                    String client_get_request = new String(packet.getPayload(), UTF_8);
                    StringBuilder response = new StringBuilder();
                    processGetRequest(client_get_request, response);
                    byte[] responseBytes = response.toString().getBytes(UTF_8);
                    System.out.println("Length of doc returned " + responseBytes.length);

                    if(response.toString().getBytes().length <= (Packet.MAX_LEN-11)) {
                        Packet resp = createPacket(packet, response.toString(), packet.getSequenceNumber()+1, PacketType.DATA.getValue());
                        channel.send(resp.toBuffer(), routerAddress);
                    }

                    else{
                       byte[][] payloads = getPayloads(responseBytes, (Packet.MAX_LEN-11));

                        for(int i = 0; i < payloads.length; i++) {
                            String payload = new String(payloads[i], UTF_8);
                            Packet resp = createPacket(packet, payload, packet.getSequenceNumber()+1, PacketType.DATA.getValue());
                            channel.send(resp.toBuffer(), routerAddress);
                        }

                        Packet fin_packet = createPacket(packet, "", packet.getSequenceNumber()+1, PacketType.FIN.getValue());
                        channel.send(fin_packet.toBuffer(), routerAddress);
                    }

                    /*
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {

                        }
                    };
                    timer.schedule(task, 0, 2000);*/


                }

            }
        }
    }

    public static byte[][] getPayloads(byte[] response, int payload_size) {

        byte[][] payloads = new byte[(int)Math.ceil(response.length / (double)payload_size)][payload_size];

        int offset = 0;

        for(int i = 0; i < payloads.length; i++) {
            payloads[i] = Arrays.copyOfRange(response,offset, offset + payload_size);
            offset += payload_size ;
        }

        return payloads;
    }

    private void processGetRequest(String requestPathLine, StringBuilder response){
        Path normalizePath = Paths.get(requestPathLine).normalize();
        String relativePath = normalizePath.toString();

        Path searchPath = Paths.get(root.toString(), relativePath);

        if (Files.exists(searchPath)){

            //If it is a directory, print all the list of files
            if(Files.isDirectory(searchPath)){
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(searchPath)) {
                    for (Path file: stream) {
                        response.append(file.getFileName() + "\r\n");
                    }
                } catch (IOException | DirectoryIteratorException x) {
                    response.append(x + "\r\n");
                    System.err.println(x);
                }
            }

            //If it is a file, get all contents and send to client
            else if(Files.isRegularFile(searchPath)){
                try {
                    if(Files.isReadable(searchPath)){
                        String data = new String(Files.readAllBytes(searchPath));
                        response.append(data);
                    }
                    else{
                        response.append("This file does not have read permissions\r\n");
                        System.err.println("This file does not have read permissions");
                    }
                } catch (IOException e) {
                    response.append("Error while reading the file contents\r\n");
                    e.printStackTrace();
                }
            }

        }

        else{
            response.append("File or folder not found");
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
        System.out.println("Sending SYN_ACK message: Hi and Seq # :" + seq_no);
        channel.send(syn_ack_response.toBuffer(), routerAddress);
        ByteBuffer buf = ByteBuffer
                .allocate(Packet.MAX_LEN)
                .order(ByteOrder.BIG_ENDIAN);
        Packet ack_packet = receivePacket(buf, channel);
        if(ack_packet.getType() == PacketType.ACK.getValue()) {
            String ack_payload = new String(ack_packet.getPayload(), UTF_8);
            System.out.println("Client ACK response : " + ack_payload);
            connection_established = true;
        }


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