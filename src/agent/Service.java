package agent;

import responses.FileSearchResponse;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Created by deepal on 3/4/15.
 */
public class Service implements Runnable {

    public int listenPort;
    public String myIp;
    public String username;
    public String bsIP;
    public int bsPort;

    public Service(String ip, int port, String username, String bsIP, int bsPort){
        this.listenPort = port;
        this.myIp = ip;
        this.username = username;
        this.bsIP = bsIP;
        this.bsPort = bsPort;
    }

    private void join() throws IOException{
        DatagramSocket clientSocket = new DatagramSocket();
        Iterator it = Cache.neighbours.entrySet().iterator();
        while(it.hasNext()){

            Map.Entry<String, Integer> neighbour = (Map.Entry<String, Integer>)it.next();
            String nIP = neighbour.getKey();
            InetAddress nIPAddress = InetAddress.getByAddress(nIP.getBytes());
            int nPort = neighbour.getValue();

            byte[] sendData = new byte[1024];
            String command = "JOIN "+myIp+" "+listenPort;
            String sendCommand = String.format("%04d", command.length())+" "+command;
            sendData = sendCommand.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, nPort);
            clientSocket.send(sendPacket);

        }

        clientSocket.close();
    }

    private void leave(){
        //leave distributed system
    }

    private void register() throws IOException{
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByAddress(bsIP.getBytes());
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String command = "REG "+myIp+" "+listenPort+" "+username;
        String sendCommand = String.format("%04d", command.length())+" "+command;
        sendData = sendCommand.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, this.bsPort);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String response = new String(receivePacket.getData());
        clientSocket.close();
        StringTokenizer st = new StringTokenizer(response," ");

        int resLength = Integer.parseInt(st.nextToken());
        String resStatus = st.nextToken();

        if(resLength>0 && resStatus.equals("REGOK")){
            int resCode = Integer.parseInt(st.nextToken());
            boolean registrationSuccessful = false;
            switch (resCode){
                case 9999:
                    System.out.println("Failed! There is some error in the command!");
                    System.exit(0);
                    break;
                case 9998:
                    System.out.println("Failed! You are already registered. Unregister first.");
                    System.exit(0);
                    break;
                case 9997:
                    System.out.println("Failed! Another user registered with same IP/Port. Try a different IP/Port.");
                    System.exit(0);
                    break;
                case 9996:
                    System.out.println("Bootstrap server is full. Try later.");
                    System.exit(0);
                    break;
                case 0:
                    System.out.println("Registration successful!");
                    registrationSuccessful = true;
                    break;
                case 1:
                    String neighbourIP = st.nextToken();
                    int neightbourPort = Integer.parseInt(st.nextToken());
                    Cache.neighbours.put(neighbourIP, new Integer(neightbourPort));
                    registrationSuccessful = true;
                    break;
                case 2:
                    Cache.neighbours.put(st.nextToken(), new Integer(st.nextToken()));
                    Cache.neighbours.put(st.nextToken(), new Integer(st.nextToken()));
                    registrationSuccessful = true;
                    break;
                default:
                    System.out.println("Unknown response from bootstrap server !");
            }
            if(!registrationSuccessful){
                System.exit(0);
            }
        }
        else if(resLength == 0){
            System.out.println("Empty response from bootstrap server. Exiting ..");
            System.exit(0);
        }
        else{
            System.out.println("Didn't get REGOK!");
            System.exit(0);
        }
    }

    private void unregister(){
        //unregister from distributed system
    }

    private FileSearchResponse searchFileLocally(String fileName){
        //search the file in the local directory and return the file path back

        return null;
    }

    private void forwardJoinRequest(String joinRequest) throws IOException{
        DatagramSocket clientSocket = new DatagramSocket();

        ArrayList<String> neighbourIPs = (ArrayList<String>)Cache.neighbours.keySet();

        String randomNeighbourIP = neighbourIPs.get(new Random(neighbourIPs.size()).nextInt());
        int randomNeighbourPort = Cache.neighbours.get(randomNeighbourIP);
        InetAddress nIPAddress = InetAddress.getByAddress(randomNeighbourIP.getBytes());

        byte[] sendData = new byte[1024];

        sendData = joinRequest.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, randomNeighbourPort);
        clientSocket.send(sendPacket);
        clientSocket.close();

    }

    private void processQuery(String query) throws IOException{

        StringTokenizer st = new StringTokenizer(query);

        int queryLength = Integer.parseInt(st.nextToken());

        if(queryLength > 0){
            String command = st.nextToken();
            String sourceIP = st.nextToken();
            int sourcePort = Integer.parseInt(st.nextToken());
            String fileName = st.nextToken();
            int ttl = Integer.parseInt(st.nextToken());

            FileSearchResponse searchResponse = searchFileLocally(fileName);

            if((searchResponse.filePaths == null) && (ttl >1)){
                ttl -= 1;
                DatagramSocket clientSocket = new DatagramSocket();

                ArrayList<String> neighbourIPs = (ArrayList<String>)Cache.neighbours.keySet();

                String randomNeighbourIP = null;
                int randomNeighbourPort = 0;

                if(searchResponse.cachedLocations.length < 2){
                    randomNeighbourIP = neighbourIPs.get(new Random(neighbourIPs.size()).nextInt());
                    randomNeighbourPort = Cache.neighbours.get(randomNeighbourIP);
                }
                else{
                    //select random neighbours from cached locations. it would be effective
                }

                InetAddress nIPAddress = InetAddress.getByAddress(randomNeighbourIP.getBytes());

                byte[] sendData = new byte[1024];
                String cmd = command+" "+sourceIP+" "+sourcePort+" "+fileName+" "+ttl;

                String sendCommand = String.format("%04d", cmd.length())+" "+cmd;
                sendData = sendCommand.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, randomNeighbourPort);
                clientSocket.send(sendPacket);

                clientSocket.close();
            }
            else if(searchResponse.filePaths != null){
                DatagramSocket clientSocket = new DatagramSocket();
                InetAddress sourceIPAddress = InetAddress.getByAddress(sourceIP.getBytes());
                byte[] sendData = new byte[1024];
                int fileCount = searchResponse.filePaths.length;
                String cmd = "SEROK ";

                if(fileCount > 0){

                    cmd += fileCount+" "+myIp+" "+listenPort;

                    for (int i = 0; i < fileCount; i++) {
                        cmd += searchResponse.filePaths[i];
                    }

                    String sendCommand = String.format("%04d", cmd.length())+" "+cmd;
                    sendData = sendCommand.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, sourceIPAddress, sourcePort);
                    clientSocket.send(sendPacket);
                }
            }
        }

    }

    @Override
    public void run() {
        try{
            DatagramSocket serverSocket = new DatagramSocket(listenPort);
            System.out.println("Service agent is listening on port "+listenPort+"...");
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];

            while(true)
            {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String sentence = new String( receivePacket.getData());
                System.out.println("RECEIVED: " + sentence);
                InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                String capitalizedSentence = sentence.toUpperCase();
                sendData = capitalizedSentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
            }

        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
