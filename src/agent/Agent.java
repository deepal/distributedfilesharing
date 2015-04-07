package agent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Agent implements Runnable, Observer {

    private void showMenu() throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Actions - ");
        System.out.println("1. Issue a query");
        System.out.println("2. Add a file");
        System.out.println("3. List my files");
        System.out.println("4. List neighbours");
        System.out.println("6. Leave network\n");
        System.out.println("Select action: ");
        int option = sc.nextInt();

        switch (option) {
            case 1:
                System.out.println("File name to search : ");
                String fileName = sc.next();
                searchFile(fileName);
                break;
            case 2:
                System.out.println("File name to add : ");
                String addFileName = new Scanner(System.in).useDelimiter("\\n").next();
                addFile(addFileName);
                break;
            case 3:
                listMyFiles();
                break;
            case 4:
                listNeighbours();
                break;
            case 5:
                leave();
                break;
            default:
                System.out.println("Invalid input!");
        }
    }

    private void addFile(String fileName) {
        String[] keywords = fileName.split(" ");
        fileName = fileName.replace(" ", "_");  //replace spaces with underscore
        HashSet<String> keyset = new HashSet<String>();
        for (int i = 0; i < keywords.length; i++) {
            keyset.add(keywords[i]);
        }
        Cache.myFiles.put(fileName, keyset);
    }

    private void listMyFiles() {
        Iterator it = Cache.myFiles.entrySet().iterator();
        if (it.hasNext()) {
            System.out.println("\n-------------------My Files-------------------");
            while (it.hasNext()) {
                Map.Entry<String, HashSet<String>> fileEntry = (Map.Entry<String, HashSet<String>>) it.next();
                System.out.println(fileEntry.getKey());
            }
            System.out.println("------------------------------------------------\n");
        } else {
            System.out.println("No files !");
        }
        (new Scanner(System.in)).nextLine();
    }

    private void listNeighbours() {
        Iterator it = Cache.neighbours.entrySet().iterator();
        if (it.hasNext()) {
            System.out.println("\n-------------------Neighbours-------------------");
            while (it.hasNext()) {
                Map.Entry<String, Integer> neighbourEntry = (Map.Entry<String, Integer>) it.next();
                System.out.println(neighbourEntry.getKey());
            }
            System.out.println("------------------------------------------------\n");
        } else {
            System.out.println("No connections !");
        }
        (new Scanner(System.in)).nextLine();
    }

    private void unregister() {

    }

    public void leave() {
        unregister();
    }

    private String getNodeHash() throws Exception {
        String date = "" + ((new Date()).getTime());
        String hashMe = date + Cache.NODE_IP + Cache.NODE_PORT + Cache.NODE_USER;
        byte[] bytesOfMessage = hashMe.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] thedigest = md.digest(bytesOfMessage);

        StringBuffer hashBuf = new StringBuffer();

        for (int i = 0; i < thedigest.length; i++) {
            hashBuf.append(thedigest[i]);
        }
        return hashBuf.toString();
    }

    public void searchFile(String fileName) {
        try {
            fileName = fileName.replace(" ", "_");
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress nIPAddress = InetAddress.getByName(Cache.NODE_IP);
            String myHash = getNodeHash();
            String command = "SER " + Cache.NODE_IP + " " + Cache.NODE_PORT + " " + fileName + " " + Cache.HOP_COUNT + " " + myHash;
            String sendCommand = String.format("%04d", command.length() + 5) + " " + command;
            byte[] sendData = new byte[1024];
            sendData = sendCommand.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, nIPAddress, Cache.NODE_PORT);
            clientSocket.send(sendPacket);
            System.out.println("Waiting for search results for file \"" + fileName + "\"...");
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            //Cache.semAgent.acquire();
            showMenu();
            //Cache.semService.release();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        try {
            showMenu();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
