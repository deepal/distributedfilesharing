package agent;

import responses.FileSearchResponse;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;


public class Service implements Runnable {

    public Semaphore printLock = new Semaphore(1);

    private void join() throws IOException {
        DatagramSocket clientSocket = new DatagramSocket();
        Iterator it = Cache.neighbours.entrySet().iterator();
        while (it.hasNext()) {

            Map.Entry<String, Integer> neighbour = (Map.Entry<String, Integer>) it.next();
            String nIP = neighbour.getKey();
            InetAddress nIPAddress = InetAddress.getByName(nIP);
            int nPort = neighbour.getValue();

            byte[] sendData = new byte[1024];
            String command = "JOIN " + Cache.NODE_IP + " " + Cache.NODE_PORT + " " + Cache.HOP_COUNT;
            String sendCommand = String.format("%04d", command.length() + 5) + " " + command;
            sendData = sendCommand.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, nPort);
            clientSocket.send(sendPacket);

        }

        clientSocket.close();
    }

    private void register() throws IOException {
        String command = "REG " + Cache.NODE_IP + " " + Cache.NODE_PORT + " " + Cache.NODE_USER;
        String sendCommand = String.format("%04d", command.length() + 5) + " " + command;

        Socket clientSocket = new Socket(Cache.BSIP, Cache.BSPORT);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(sendCommand);
        String response = inFromServer.readLine();
        clientSocket.close();

        StringTokenizer st = new StringTokenizer(response, " ");

        int resLength = Integer.parseInt(st.nextToken());
        String resStatus = st.nextToken();

        if (resLength > 0 && resStatus.equals("REGOK")) {
            int resCode = Integer.parseInt(st.nextToken());
            boolean registrationSuccessful = false;
            switch (resCode) {
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
                    registrationSuccessful = true;
                    break;
                case 1:
                    String neighbourIP = st.nextToken();
                    int neightbourPort = Integer.parseInt(st.nextToken());
                    Cache.neighbours.put(neighbourIP, new Integer(neightbourPort));
                    registrationSuccessful = true;
                    break;
                default:
                    if (resCode > 1) {
                        if (resCode > 1) {
                            Cache.neighbours.put(st.nextToken(), new Integer(st.nextToken()));
                            Cache.neighbours.put(st.nextToken(), new Integer(st.nextToken()));
                            registrationSuccessful = true;
                        }
                    } else {
                        System.out.println("Unknown response from bootstrap server !");
                    }
            }
            if (!registrationSuccessful) {
                System.exit(0);
            }
        } else if (resLength == 0) {
            System.out.println("Empty response from bootstrap server. Exiting ..");
            System.exit(0);
        } else {
            System.out.println("Didn't get REGOK!");
            System.exit(0);
        }
    }

    private FileSearchResponse searchFileLocally(String fileName) {

        String[] searchKeywords = fileName.split(" ");
        HashSet<String> searchKeySet = new HashSet<String>();
        for (int i = 0; i < searchKeywords.length; i++) {
            searchKeySet.add(searchKeywords[i]);
        }

        ArrayList<String> fp = new ArrayList<String>();

        Iterator it = Cache.myFiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, HashSet<String>> entry = (Map.Entry<String, HashSet<String>>) it.next();
            HashSet<String> keywordSet = entry.getValue();

            if (keywordSet.containsAll(searchKeySet)) { //if the key set of search term equals the key set of the file, set found
                fp.add(entry.getKey());
            }
        }

        FileSearchResponse fResp = new FileSearchResponse();

        if (fp.size() > 0) {
            fResp.filePaths = fp;
        } else {
            fResp.filePaths = null;
        }

        return fResp;
    }

    private void forwardJoinRequest(String joinRequest) throws IOException {
        //TODO: need to add TTL and caching new comers

        String[] tokens = joinRequest.split(" ");
        int length = Integer.parseInt(tokens[0]);
        if (length > 0) {

            ArrayList<String> neighbourIPs = (ArrayList<String>) Cache.neighbours.keySet();
            String randomNeighbourIP = neighbourIPs.get(new Random(neighbourIPs.size()).nextInt());
            int randomNeighbourPort = Cache.neighbours.get(randomNeighbourIP);
            InetAddress nIPAddress = InetAddress.getByName(randomNeighbourIP);

            String cmd = tokens[1];
            String IPAddr = tokens[2];
            String port = tokens[3];
            int ttl = Integer.parseInt(tokens[4]);

            ttl--;

            if (!Cache.neighbours.containsKey(IPAddr)) {
                Cache.neighbours.put(IPAddr, new Integer(port));
            }

            String command = cmd + " " + IPAddr + " " + port + " " + ttl;
            String sendCommand = String.format("%04d", command.length() + 5) + " " + command;

            DatagramSocket clientSocket = new DatagramSocket();
            byte[] sendData = new byte[1024];
            sendData = sendCommand.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, randomNeighbourPort);
            clientSocket.send(sendPacket);
            clientSocket.close();
        }


    }

    private void processQuery(String[] tokens) throws IOException {

        int queryLength = Integer.parseInt(tokens[0]);

        if (queryLength > 0) {
            String command = tokens[1];
            String sourceIP = tokens[2];
            int sourcePort = Integer.parseInt(tokens[3]);
            String fileName = tokens[4];
            int ttl = Integer.parseInt(tokens[5]);
            String hash = tokens[6];

            FileSearchResponse searchResponse = searchFileLocally(fileName);

            if ((searchResponse.filePaths == null) && (ttl > 1)) {
                ttl -= 1;
                DatagramSocket clientSocket = new DatagramSocket();

                ArrayList<String> neighbourIPs = (ArrayList<String>) (Cache.neighbours.keySet());

                String randomNeighbourIP = null;
                int randomNeighbourPort = 0;

                if (searchResponse.cachedLocations.size() < 2) {
                    randomNeighbourIP = neighbourIPs.get(new Random(neighbourIPs.size()).nextInt());
                    randomNeighbourPort = Cache.neighbours.get(randomNeighbourIP);
                } else {
                    //select random neighbours from cached locations. it would be effective
                }

                InetAddress nIPAddress = InetAddress.getByName(randomNeighbourIP);

                byte[] sendData = new byte[1024];
                String cmd = command + " " + sourceIP + " " + sourcePort + " " + fileName + " " + ttl + " " + hash;

                String sendCommand = String.format("%04d", cmd.length() + 5) + " " + cmd;
                sendData = sendCommand.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, randomNeighbourPort);
                clientSocket.send(sendPacket);

                clientSocket.close();

            } else if (searchResponse.filePaths != null) {
                if (sourceIP.equals(Cache.NODE_IP)) {
                    System.out.println("------------------------------------------");
                    System.out.println("Search results from my files: ");
                    for (int i = 0; i < searchResponse.filePaths.size(); i++) {
                        System.out.println((i + 1) + ". " + searchResponse.filePaths.get(i));
                    }
                    System.out.println("------------------------------------------");
                } else {
                    DatagramSocket clientSocket = new DatagramSocket();
                    InetAddress sourceIPAddress = InetAddress.getByName(sourceIP);
                    byte[] sendData = new byte[1024];
                    int fileCount = searchResponse.filePaths.size();
                    String cmd = "SEROK ";

                    if (fileCount > 0) {

                        cmd += fileCount + " " + Cache.NODE_IP + " " + Cache.NODE_PORT + " ";

                        for (int i = 0; i < fileCount; i++) {
                            cmd += searchResponse.filePaths.get(i);
                        }

                        cmd += " " + hash;

                        String sendCommand = String.format("%04d", cmd.length() + 5) + " " + cmd;
                        sendData = sendCommand.getBytes();
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, sourceIPAddress, sourcePort);
                        clientSocket.send(sendPacket);
                    }
                }
            }
        }
    }

    public synchronized void printSearchOutput(String[] tokens) {
        int respLength = Integer.parseInt(tokens[0]);
        if (respLength > 0) {
            //String respMsg = tokens[1];
            int fileCount = Integer.parseInt(tokens[2]);
            String senderIP = tokens[3];
            int senderPort = Integer.parseInt(tokens[4]);

            int tokenIndex = 4;

            String recvHash = tokens[tokenIndex + fileCount + 1]; //get the hash from the last token

            String searchFileName = Cache.queryCache.get(recvHash);

            if (searchFileName != null) {
                System.out.println("Searched files for query: \"" + searchFileName + "\"");
                System.out.println("From " + senderIP + " at port " + senderPort);
                if (fileCount > 0) {
                    for (int i = 0; i < fileCount; i++) {
                        System.out.println((i + 1) + ". " + tokens[tokenIndex + i]);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(Cache.NODE_PORT);
            System.out.println("Service agent is listening on port " + Cache.NODE_PORT + "...");
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];

            try {
                register();
                join();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String request = new String(receivePacket.getData());
                String[] tokens = request.split(" ");
                int requestLength = Integer.parseInt(tokens[0]);

                request = request.substring(0, requestLength);
                tokens = request.split(" ");

                if (requestLength > 0) {

                    String command = tokens[1];
                    String[] filePaths;
                    if (command.equals("SER")) {
                        processQuery(tokens);   //search the file locally and if not found, forward it
                    } else if (command.equals("SEROK")) {
                        printSearchOutput(tokens); // print the search results
                    } else if (command.equals("JOIN")) {
                        forwardJoinRequest(request);    //forward the join request to a random neighbour
                    }
                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
