package org.minima.system.network.p2p;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.minima.objects.base.MiniData;
import org.minima.objects.greet.Greeting;
import org.minima.system.Main;
import org.minima.system.brains.BackupManager;
import org.minima.system.input.InputHandler;
import org.minima.system.network.NetworkHandler;
import org.minima.system.network.base.MinimaClient;
import org.minima.system.network.p2p.messages.*;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageProcessor;
import org.minima.utils.messages.TimerMessage;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.minima.system.network.NetworkHandler.NETWORK_CONNECT;
import static org.minima.system.network.NetworkHandler.NETWORK_DISCONNECT;

@Getter
@Slf4j(topic = "P2P")
public class P2PMessageProcessor extends MessageProcessor {

    /**
     * P2P Functions..
     */

    public static final String P2P_INIT = "P2P_INIT";
    public static final String P2P_SHUTDOWN = "P2P_SHUTDOWN";
    public static final String P2P_ON_GREETED = "P2P_ON_GREETED";
    public static final String P2P_ON_CONNECTED = "P2P_ON_CONNECTED";
    public static final String P2P_ON_DISCONNECTED = "P2P_ON_DISCONNECTED";

    public static final String P2P_LOOP = "P2P_LOOP";

    public static final String P2P_CONNECT = "P2P_CONNECT";
    public static final String P2P_DISCONNECT = "P2P_DISCONNECT";

    public static final String P2P_RENDEZVOUS = "P2P_RENDEZVOUS";
    public static final String P2P_WALK_LINKS = "P2P_WALK_LINKS";
    public static final String P2P_SWAP_LINK = "P2P_SWAP_LINK";
    public static final String P2P_DO_SWAP = "P2P_DO_SWAP";
    public static final String P2P_MAP_NETWORK = "P2P_MAP_NETWORK";
    public static final String P2P_MAP_NETWORK_RESPONSE = "P2P_MAP_NETWORK_RESPONSE";
    public static final String P2P_PRINT_NETWORK_MAP = "P2P_PRINT_NETWORK_MAP";
    public static final String P2P_PRINT_NETWORK_MAP_RESPONSE = "P2P_PRINT_NETWORK_MAP_RESPONSE";





    /*
     * Network Messages
     * RENDEZVOUS
     * WALK_LINKS
     * SWAP_LINK
     * MAP_NETWORK
     */

    private static final int CLEANUP_LOOP_DELAY = 60_000;

    private final P2PState state;
    private final int minimaPort;
    //The data store
    private InetAddress hostIP;

    Message printNetworkMapRPCReq;

    public P2PMessageProcessor() {
        super("P2P Message Processor");

        try {
            this.hostIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            log.error("Could not identify the local ip address: " + hostIP);
        }
        state = new P2PState(5, null);
        state.setAddress(new InetSocketAddress(getHostIP(), getMinimaPort()));
        this.minimaPort = 9001;

    }

    public P2PMessageProcessor(String hostIP, int minimaPort) {
        super("P2P Message Processor");

        try {
            this.hostIP = InetAddress.getByName(hostIP);
        } catch (UnknownHostException e) {
            log.error("Could not identify the local ip address: " + hostIP);
        }
        this.minimaPort = minimaPort;


        //Get the BackupManager
        BackupManager backup = Main.getMainHandler().getBackupManager();
        File p2pDataFile = backup.getBackUpFile("p2pdata.json");
        state = new P2PState(5, p2pDataFile);
        state.setAddress(new InetSocketAddress(getHostIP(), getMinimaPort()));
        //Start the Ball rolling..
//        this.setLOG(true);
        PostTimerMessage(new TimerMessage(10_000, P2P_LOOP));

    }

    public void stop() {
        PostMessage(P2P_SHUTDOWN);
    }

    /**
     * You can use this to get your HOST/IP etc
     *
     * @return
     */
    protected NetworkHandler getNetworkHandler() {
        return Main.getMainHandler().getNetworkHandler();
    }

    /**
     * All the current connections
     *
     * @return
     */
    protected ArrayList<MinimaClient> getCurrentMinimaClients() {
        return getNetworkHandler().getNetClients();
    }

    protected void sendMessage(MinimaClient zClient, Message zMessage) {
        zClient.PostMessage(zMessage);
    }


    /**
     * Routes messages to the correct processing function
     *
     * @param zMessage The Full Message
     * @throws Exception
     */
    @Override
    protected void processMessage(Message zMessage) throws Exception {
        if (!zMessage.isMessageType(P2P_WALK_LINKS)) {
            log.debug("[+] P2PMessageProcessor processing: " + zMessage.getMessageType());
        }
        try {
            switch (zMessage.getMessageType()) {
                case P2P_SHUTDOWN:
                    processShutdownMsg(zMessage);
                    break;
                case P2P_ON_GREETED:
                    processOnGreetedMsg(zMessage);
                    break;
                case P2P_RENDEZVOUS:
                    processOnRendezvousMsg(zMessage);
                    break;
                case P2P_ON_CONNECTED:
                    processOnConnectedMsg(zMessage);
                    break;
                case P2P_ON_DISCONNECTED:
                    processOnDisconnectedMsg(zMessage);
                    break;
                case P2P_CONNECT:
                    processConnectMsg(zMessage);
                    break;
                case P2P_DISCONNECT:
                    processDisconnectMsg(zMessage);
                    break;
                case P2P_LOOP:
                    processLoopMsg(zMessage);
                    break;
                case P2P_WALK_LINKS:
                    processWalkLinksMsg(zMessage);
                    break;
                case P2P_SWAP_LINK:
                    processSwapLinkMsg(zMessage);
                    break;
                case P2P_DO_SWAP:
                    processDoSwapMsg(zMessage);
                    break;
                case P2P_MAP_NETWORK:
                    processMapNetworkMsg(zMessage);
                    break;
                case P2P_MAP_NETWORK_RESPONSE:
                    processMapNetworkResponseMsg(zMessage);
                    break;
                case P2P_PRINT_NETWORK_MAP:
                    processPrintNetworkMapRequestMsg(zMessage);
                    break;
                case P2P_PRINT_NETWORK_MAP_RESPONSE:
                    processPrintNetworkMapResponseMsg(zMessage);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {

            StringBuilder builder = new StringBuilder();
            StackTraceElement[] trace = e.getStackTrace();
            for (StackTraceElement traceElement : trace)
                builder.append("\tat " + traceElement + "\n");

            log.error("[!] Exception in P2P Message Processor: " + e + "\n" + builder);
        }
    }


    private void processShutdownMsg(Message zMessage) {
        log.debug("[+] P2PMessageProcessor processing P2P_SHUTDOWN message");
        // Make sure the node list is saved
        P2PFunctions.SaveNodeList(this.state);
        //And stop this Message Processor stack
        stopMessageProcessor();
    }

    private void processOnGreetedMsg(Message zMessage) {
        log.debug("[+] P2PMessageProcessor processing P2P_ON_CONNECTED message");
        Greeting greeting = (Greeting) zMessage.getObject("greeting");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        if (greeting.getDetails().containsKey("isClient")) {
            boolean isClient = (boolean) greeting.getDetails().get("isClient");
            Long minimaPort = (Long) greeting.getDetails().get("minimaPort");
            Long numClients = (Long) greeting.getDetails().get("numClients");

            InetSocketAddress address = new InetSocketAddress(client.getHost(), minimaPort.intValue());
            client.setMinimaAddress(address);

            // Update State
            if (client.isIncoming()) {
                if (isClient) {
                    state.addClientLink(address);
                } else {
                    state.addInLink(address);
                }
                state.addToInLinkClientUidToMinimaAddress(client.getUID(), address);
            } else {
                state.addOutLink(address);
            }

            if (!isClient) {

                if (state.getRequestSwapOnConnect().contains(client.getMinimaAddress())) {
                    P2PMsgSwapLink swapLink = new P2PMsgSwapLink();
                    swapLink.setSwapTarget(state.getAddress());
                    client.PostMessage(new Message(MinimaClient.NETMESSAGE_P2P_SWAP_LINK).addObject("data", swapLink));
                    state.removeRequestSwapOnConnect(client.getMinimaAddress());
                }

                log.info("Adding node to RandomNodeSet");
                if (!state.getRandomNodeSet().contains(address)) {
                    state.addRandomNodeSet(address);
                }
            }

            if (state.isRendezvousComplete() && state.getClientLinks().size() < state.getNumLinks() * 4) {
                int numClientSlotsAvailable = (state.getNumLinks() * 4) - state.getClientLinks().size();
                int numSwaps = Math.min(numClients.intValue() / 2, numClientSlotsAvailable);
                for (int i = 0; i < numSwaps; i++) {
                    P2PMsgSwapLink swapLink = new P2PMsgSwapLink();
                    swapLink.setSwapTarget(state.getAddress());
                    swapLink.setSwapClientReq(true);
                    client.PostMessage(new Message(MinimaClient.NETMESSAGE_P2P_SWAP_LINK).addObject("data", swapLink));
                }
            }
        } else {
            log.warn("Client " + client.getHost() + " port " + client.getPort() + " is not a p2p node");
        }
    }

    private void processOnRendezvousMsg(Message zMessage) {
        if (state.isRendezvousComplete()) {
            return;
        }

        P2PMsgRendezvous rendezvous = (P2PMsgRendezvous) zMessage.getObject("rendezvous");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        rendezvous.getAddresses().forEach(state::addRandomNodeSet);
        state.setRendezvousComplete(true);

        TimerMessage shutdownMsg = new TimerMessage(1_000, P2P_DISCONNECT);
        shutdownMsg.addObject("client", client);
        shutdownMsg.addInteger("attempt", 0);
        shutdownMsg.addString("reason", "Disconnecting After Rendezvous ");
        PostTimerMessage(shutdownMsg);
    }


    private void processOnConnectedMsg(Message zMessage) {
        // Do nothing as we don't have enough info to process - wait until we get the greeting message
        log.debug("[+] P2PMessageProcessor processing P2P_ON_CONNECTED message");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
    }


    private void processOnDisconnectedMsg(Message zMessage) {
        log.debug(this.state.genPrintableState());
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        String uid = zMessage.getString("uid");
        log.debug("[!] P2P_ON_DISCONNECT Disconnected from isInLink?" + client.isIncoming() + " IP: " + client.getMinimaAddress());
        if (!client.isIncoming() && state.getOutLinks().size() <= state.getNumLinks()) {
            // Replace Outlink
            P2PMsgWalkLinks walkLinks = new P2PMsgWalkLinks(true, true);
            InetSocketAddress nextHop = P2PFunctions.SelectRandomAddress(state.getInLinks());
            MinimaClient minimaClient = P2PFunctions.getClientForInetAddress(nextHop, getCurrentMinimaClients(), true);
            if (minimaClient != null) {
                log.debug("[+] P2P_ON_DISCONNECT P2P_WALK Starting inLink walk to replace lost outLink neighbour");
                Message walkMsg = new Message(MinimaClient.NETCLIENT_P2P_WALK_LINKS);
                walkMsg.addObject("data", walkLinks);
                minimaClient.PostMessage(walkMsg);
                ExpiringMessage expiringMessage = new ExpiringMessage(new Message(P2PMessageProcessor.P2P_WALK_LINKS).addObject("data", walkLinks));
                // We need messages more hops away to expire faster
                expiringMessage.setTimestamp(System.currentTimeMillis() + 2_000L);
                state.getExpiringMessageMap().put(walkLinks.getSecret(), expiringMessage);
            } else {
                log.warn("[-] P2P_ON_DISCONNECT MinimaClient for " + nextHop + " does not exist in clients list for inlink");
            }
        }
        if (client.isIncoming() && state.getInLinks().contains(client.getMinimaAddress()) && state.getInLinks().size() <= state.getNumLinks()) {
            // Replace Inlink
            P2PMsgWalkLinks walkLinks = new P2PMsgWalkLinks(false, false);
            InetSocketAddress nextHop = P2PFunctions.SelectRandomAddress(state.getOutLinks());
            if (nextHop == null) {
                nextHop = P2PFunctions.SelectRandomAddress(state.getInLinks());
            }
            MinimaClient minimaClient = P2PFunctions.getClientForInetAddress(nextHop, getCurrentMinimaClients(), false);
            if (minimaClient != null) {
                log.debug("[+] P2P_ON_DISCONNECT P2P_WALK Starting outLink walk to replace lost inLink neighbour");
                Message walkMsg = new Message(MinimaClient.NETCLIENT_P2P_WALK_LINKS);
                walkMsg.addObject("data", walkLinks);
                minimaClient.PostMessage(walkMsg);
                ExpiringMessage expiringMessage = new ExpiringMessage(new Message(P2PMessageProcessor.P2P_WALK_LINKS).addObject("data", walkLinks));
                // We need messages more hops away to expire faster
                expiringMessage.setTimestamp(System.currentTimeMillis() + 2_000L);
                state.getExpiringMessageMap().put(walkLinks.getSecret(), expiringMessage);
            } else {
                log.warn("[-] P2P_ON_DISCONNECT MinimaClient for " + nextHop + " does not exist in clients list for outlink");
            }
        }
        state.removeDisconnectingClient(client.getUID());
        this.state.removeLink(uid, client.getMinimaAddress());
        log.debug(this.state.genPrintableState());

    }

    private void processConnectMsg(Message zMessage) {
//        log.debug("[+] P2PMessageProcessor processing P2P_CONNECT message");

        InetSocketAddress address = (InetSocketAddress) zMessage.getObject("address");
        String reason = zMessage.getString("reason");
        if (!state.getAddress().equals(address)) {

            Message msg = new Message(NETWORK_CONNECT);
            msg.addObject("address", address);
            log.debug("[+] P2P_CONNECT to: " + address + " reason: " + reason);
            Main.getMainHandler().getNetworkHandler().PostMessage(msg);
        } else {
            log.debug("[!] Attempting to connect to self");
            state.removeRandomNodeSet(address);
        }
    }

    private void processDisconnectMsg(Message zMessage) {
        MinimaClient client = (MinimaClient) zMessage.getObject("client");
        String reason = zMessage.getString("reason");
        log.debug("[!] P2P_DISCONNECT Disconnecting from isInLink? " + client.isIncoming() + " IP: " + client.getMinimaAddress() + " for: " + reason);
        int attempt = zMessage.getInteger("attempt");
        if (this.state.getOutLinks().contains(client.getMinimaAddress()) ||
                this.state.getInLinks().contains(client.getMinimaAddress()) ||
                this.state.getClientLinks().contains(client.getMinimaAddress())) {
            getNetworkHandler().PostMessage(new Message(NETWORK_DISCONNECT).addString("uid", client.getUID()));
        } else {
            if (attempt < 3) {
                TimerMessage shutdownMsg = new TimerMessage(1_000, P2P_DISCONNECT);
                shutdownMsg.addObject("client", client);
                shutdownMsg.addInteger("attempt", attempt + 1);
                shutdownMsg.addString("reason", reason + " attempt: " + attempt);
                PostTimerMessage(shutdownMsg);
            }
        }


    }

    private void processSwapLinkMsg(Message zMessage) {
        P2PMsgSwapLink swapLink = (P2PMsgSwapLink) zMessage.getObject("data");
        if (swapLink.isConditionalSwapReq() && state.getInLinks().size() < state.getNumLinks()) {
            log.debug("[-] P2P_DO_SWAP not sent for conditional swap - not enough inLinks: " + state.getInLinks().size());
            return;
        }
        if (swapLink.isSwapClientReq()) {
            InetSocketAddress addressToDoSwap = P2PFunctions.SelectRandomAddress(state.getClientLinks());

            ArrayList<MinimaClient> clients = getCurrentMinimaClients().stream().filter(x -> !state.getDisconnectingClients().contains(x.getUID())).collect(Collectors.toCollection(ArrayList::new));

            log.debug("[!] P2P_SWAP_LINK Num Clients post filter: " + clients.size() + " num disconnecting clients: " + state.getDisconnectingClients().size() + " num clients: " + getCurrentMinimaClients().size());
            MinimaClient minimaClient = P2PFunctions.getClientForInetAddress(addressToDoSwap, clients, true);
            if (minimaClient != null) {
                log.debug("[+] P2P_DO_SWAP CLIENT Sending do swap message to client");
                state.addDisconnectingClient(minimaClient.getUID());
                Message doSwap = new Message(MinimaClient.NETMESSAGE_P2P_DO_SWAP);
                P2PMsgDoSwap msgDoSwap = new P2PMsgDoSwap();
                msgDoSwap.setSecret(swapLink.getSecret());
                msgDoSwap.setSwapTarget(swapLink.getSwapTarget());
                doSwap.addObject("data", msgDoSwap);
                minimaClient.PostMessage(doSwap);
            } else {
                log.debug("[-] P2P_DO_SWAP not sent swap - no clients available");

            }
        } else {
            ArrayList<InetSocketAddress> filteredInLinks = (ArrayList<InetSocketAddress>) state.getInLinks().stream()
                    .filter(x -> !x.equals(swapLink.getSwapTarget()))
                    .collect(Collectors.toList());
            if (!filteredInLinks.isEmpty()) {
                InetSocketAddress addressToDoSwap = P2PFunctions.SelectRandomAddress(filteredInLinks);

                ArrayList<MinimaClient> clients = getCurrentMinimaClients().stream().filter(x -> !state.getDisconnectingClients().contains(x.getUID())).collect(Collectors.toCollection(ArrayList::new));

                log.debug("[!] P2P_SWAP_LINK Num Clients post filter: " + clients.size() + " num disconnecting clients: " + state.getDisconnectingClients().size() + " num clients: " + getCurrentMinimaClients().size());
                MinimaClient minimaClient = P2PFunctions.getClientForInetAddress(addressToDoSwap, clients, true);
                if (minimaClient != null) {
                    log.debug("[+] P2P_DO_SWAP Sending do swap message");
                    state.addDisconnectingClient(minimaClient.getUID());
                    Message doSwap = new Message(MinimaClient.NETMESSAGE_P2P_DO_SWAP);
                    P2PMsgDoSwap msgDoSwap = new P2PMsgDoSwap();
                    msgDoSwap.setSecret(swapLink.getSecret());
                    msgDoSwap.setSwapTarget(swapLink.getSwapTarget());
                    doSwap.addObject("data", msgDoSwap);
                    minimaClient.PostMessage(doSwap);
                } else {
                    log.debug("[-] P2P_DO_SWAP not sent swap - no inLinks that are not the swap target");

                }
            }
//            else {
//                TimerMessage timerMessage = new TimerMessage(1_000, zMessage.getMessageType());
//                timerMessage.addObject("data", swapLink);
//                PostTimerMessage(timerMessage);
//            }
        }

    }

    private void processDoSwapMsg(Message zMessage) {
        P2PMsgDoSwap doSwap = (P2PMsgDoSwap) zMessage.getObject("data");
        MinimaClient client = (MinimaClient) zMessage.getObject("client");

        log.debug("[+] P2P_DO_SWAP Swapping from: " + client.getMinimaAddress() + " to: " + doSwap.getSwapTarget());

        // Connect to new client
        PostMessage(new Message(P2PMessageProcessor.P2P_CONNECT).addObject("address", doSwap.getSwapTarget()).addString("reason", " for a DO_SWAP Request"));

        // Disconnect from old client
        Message shutdownMsg = new Message(P2P_DISCONNECT);
        shutdownMsg.addObject("client", client);
        shutdownMsg.addInteger("attempt", 0);
        shutdownMsg.addString("reason", "Disconnecting after Do Swap to: " + doSwap.getSwapTarget());
        PostMessage(shutdownMsg);

    }

    private void sendSwapLinkMsgIfOutWalk(P2PMsgWalkLinks msgWalkLinks) {
        if (!msgWalkLinks.isWalkInLinks()) {
            P2PMsgSwapLink swapLink = new P2PMsgSwapLink();
            swapLink.setSecret(msgWalkLinks.getSecret());
            swapLink.setSwapTarget(msgWalkLinks.getPathTaken().get(0));
            swapLink.setConditionalSwapReq(true);
            PostMessage(new Message(P2P_SWAP_LINK).addObject("data", swapLink));
        }
    }

    private void processWalkLinksMsg(Message zMessage) {
        P2PMsgWalkLinks msgWalkLinks = (P2PMsgWalkLinks) zMessage.getObject("walkLinksMsg");
        InetSocketAddress nextHop;
        if (msgWalkLinks.isReturning()) {
            nextHop = msgWalkLinks.getPreviousNode(state.getAddress());
        } else {
            // Outbound
            msgWalkLinks.addHopToPath(state.getAddress());

            if (msgWalkLinks.getNumHopsToGo() == 0) {
                msgWalkLinks.setReturning(true);
                nextHop = msgWalkLinks.getPreviousNode(state.getAddress());
                sendSwapLinkMsgIfOutWalk(msgWalkLinks);
                if (!msgWalkLinks.isWalkInLinks()) {
                    nextHop = null;
                }

            } else {
                if (msgWalkLinks.isWalkInLinks()) {
                    ArrayList<InetSocketAddress> filteredInLinks = (ArrayList<InetSocketAddress>) state.getInLinks().stream()
                            .filter(x -> msgWalkLinks.getPathTaken().stream().noneMatch(x::equals))
                            .collect(Collectors.toList());
                    if (filteredInLinks.isEmpty()) {
                        log.debug("[-] P2P_WALK_LINKS no inLinks for the next hop. Origin address: " + msgWalkLinks.getPathTaken().get(0));
                    }
                    nextHop = P2PFunctions.SelectRandomAddress(filteredInLinks);
                } else {
                    ArrayList<InetSocketAddress> filteredOutLinks = (ArrayList<InetSocketAddress>) state.getOutLinks().stream()
                            .filter(x -> msgWalkLinks.getPathTaken().stream().noneMatch(x::equals))
                            .collect(Collectors.toList());
                    if (filteredOutLinks.isEmpty()) {
                        log.debug("[-] P2P_WALK_LINKS no outLinks for the next hop. Origin address: " + msgWalkLinks.getPathTaken().get(0));
                    }
                    nextHop = P2PFunctions.SelectRandomAddress(filteredOutLinks);
                }

                if (nextHop == null) {
                    msgWalkLinks.setReturning(true);
                    if (msgWalkLinks.isWalkInLinks()) {
                        nextHop = msgWalkLinks.getPreviousNode(state.getAddress());
                    } else {
                        sendSwapLinkMsgIfOutWalk(msgWalkLinks);
                    }
                }
            }

        }

        if (nextHop != null) {
            MinimaClient minimaClient = P2PFunctions.getClientForInetAddressEitherDirection(nextHop, getCurrentMinimaClients());
            if (minimaClient != null) {
                log.debug("[+] P2P_WALK_LINKS Sending message to: " + nextHop + " isReturning: " + msgWalkLinks.isReturning());
                Message walkMsg = new Message(MinimaClient.NETCLIENT_P2P_WALK_LINKS);
                walkMsg.addObject("data", msgWalkLinks);
                sendMessage(minimaClient, walkMsg);
            } else {
                log.warn("[-] P2P_WALK_LINKS MinimaClient for " + nextHop + " does not exist");
            }
        }

        if (state.getAddress().equals(msgWalkLinks.getPathTaken().get(0))) {
            processReturnedWalkMsg(msgWalkLinks);
        }
    }

    public void processReturnedWalkMsg(P2PMsgWalkLinks msg) {
        log.debug("[+] P2P_WALK_LINKS returned to the start after hops: " + msg.getPathTaken().size() + " end node address: " + msg.getPathTaken().get(msg.getPathTaken().size() - 1));

        if (msg.isWalkInLinks()) {
            InetSocketAddress finalAddress = msg.getPathTaken().get(msg.getPathTaken().size() - 1);
            if (!finalAddress.equals(this.state.getAddress())) {
                if (!state.getRandomNodeSet().contains(finalAddress)) {
                    state.addRandomNodeSet(finalAddress);
                }
                if (this.state.getOutLinks().size() < this.state.getNumLinks()) {
                    if (msg.isJoiningWalk()) {
                        state.addRequestSwapOnConnect(finalAddress);
                    }
                    PostMessage(new Message(P2P_CONNECT).addObject("address", finalAddress).addString("reason", "For completed connection walk"));
                } else {
                    log.debug("[!] P2P WalkLinks Result: Not Connecting already have max numLinks");
                }
            } else {
                log.debug("[!] P2P WalkLinks Result: Not Connecting as returned own address");
            }
        } else {
            log.debug("[-] P2P_WALK_LINKS outlink walk returned to start - This shouldn't happen");
        }
    }

    private void processLoopMsg(Message zMessage) {
        if (state.getOutLinks().size() == state.getNumLinks()) {
            state.setSetupComplete(true);
        }
        if (!state.isSetupComplete()) {
            ArrayList<Message> msgs = P2PFunctions.Join(state, getCurrentMinimaClients());
            msgs.forEach(this::PostMessage);
        }
        ArrayList<ExpiringMessage> expiringMessages = this.state.dropExpiredMessages();
        if (!expiringMessages.isEmpty()) {
            log.debug("[!] P2P Network messages timed out");
            for (ExpiringMessage expMsg: expiringMessages){
                if (expMsg.getMsg().isMessageType(P2P_MAP_NETWORK)){
                    Message msg = expMsg.getMsg();
                    P2PMsgMapNetwork networkMap = (P2PMsgMapNetwork) msg.getObject("data");
                    if (networkMap == null){
                        log.error("[-] P2P_LOOP expiring message network map is null!? " + msg);
                    }
                    Message newMsg = new Message(P2P_MAP_NETWORK_RESPONSE);
                    newMsg.addObject("data", networkMap).addString("from_ip", state.getAddress().toString());
                    ExpiringMessage expiringMessage = new ExpiringMessage(newMsg);
                    // We need messages more hops away to expire faster
                    expiringMessage.setTimestamp(System.currentTimeMillis() + ((6 - networkMap.getPathTaken().size()) * 500L));
                    state.getExpiringMessageMap().put(networkMap.getMsgUID(), expiringMessage);
                    PostMessage(newMsg);
                } else {
                    log.debug("[!] " + expMsg.msg.getMessageType() + " expired without a response");
                }
            }
        }

        PostTimerMessage(new TimerMessage(1_000, P2P_LOOP));
    }

    private void processMapNetworkMsg(Message zMessage) {
        P2PMsgMapNetwork networkMap = (P2PMsgMapNetwork) zMessage.getObject("data");
        String fromAddress = zMessage.getString("from_ip");
        log.debug("[+] P2P_MAP_NETWORK from address: " + fromAddress);
        // Add this node to the path
        networkMap.getPathTaken().add(state.getAddress());
        if (networkMap.getPathTaken().size() < 6) {
            // Send messages out another degree
            ArrayList<InetSocketAddress> filteredInLinks = (ArrayList<InetSocketAddress>) state.getInLinks().stream()
                    .filter(x -> networkMap.getPathTaken().stream().noneMatch(x::equals)).distinct()
                    .collect(Collectors.toList());
            log.debug("[!] P2P_MAP_NETWORK path: " + networkMap.getPathTaken() + " filtered InLinks: " + filteredInLinks);

            if (!filteredInLinks.isEmpty()) {
                for (InetSocketAddress nextHop : filteredInLinks) {
                    MinimaClient minimaClient = P2PFunctions.getClientForInetAddress(nextHop, getCurrentMinimaClients(), true);
                    if (minimaClient != null) {
                        // Update the Msg UID
                        networkMap.setMsgUID(MiniData.getRandomData(8));
                        Message msg = new Message(MinimaClient.NETMESSAGE_P2P_MAP_NETWORK);
                        msg.addObject("data", networkMap);

                        log.debug("[!] P2P_MAP_NETWORK forwarding message to: " + nextHop + " Messages UID: " + networkMap.getMsgUID());
                        sendMessage(minimaClient, msg);

                        Message expiringMsg = new Message(P2P_MAP_NETWORK);
                        P2PMsgMapNetwork expNetMap = new P2PMsgMapNetwork(networkMap);
                        expiringMsg.addObject("data", expNetMap);

                        state.getRequestUIDtoMsgUID().put(networkMap.getRequestUID(), networkMap.getMsgUID());
                        ExpiringMessage expiringMessage = new ExpiringMessage(expiringMsg);
                        // We need messages more hops away to expire faster
                        expiringMessage.setTimestamp(System.currentTimeMillis() + ((6 - networkMap.getPathTaken().size()) * 500L));
                        state.getExpiringMessageMap().put(networkMap.getMsgUID(), expiringMessage);
                    } else {
                        log.warn("[-] P2P_MAP_NETWORK client not found for: " + nextHop);
                    }
                }
            } else {
                Message expiringMsg = new Message(P2P_MAP_NETWORK);
                P2PMsgMapNetwork expNetMap = new P2PMsgMapNetwork(networkMap);
                expiringMsg.addObject("data", expNetMap);
                state.getRequestUIDtoMsgUID().put(networkMap.getRequestUID(), networkMap.getMsgUID());
                ExpiringMessage expiringMessage = new ExpiringMessage(expiringMsg);
                // We need messages more hops away to expire faster
                expiringMessage.setTimestamp(System.currentTimeMillis() + ((6 - networkMap.getPathTaken().size()) * 500L));
                state.getExpiringMessageMap().put(networkMap.getMsgUID(), expiringMessage);

                // Start returning messages
                Message reponseMsg = new Message(P2P_MAP_NETWORK_RESPONSE).addObject("data", networkMap).addString("from_ip", state.getAddress().toString());
                PostMessage(reponseMsg);
            }
        } else {
            Message expiringMsg = new Message(P2P_MAP_NETWORK);
            P2PMsgMapNetwork expNetMap = new P2PMsgMapNetwork(networkMap);
            expiringMsg.addObject("data", expNetMap);
            state.getRequestUIDtoMsgUID().put(networkMap.getRequestUID(), networkMap.getMsgUID());
            ExpiringMessage expiringMessage = new ExpiringMessage(expiringMsg);
            // We need messages more hops away to expire faster
            expiringMessage.setTimestamp(System.currentTimeMillis() + ((6 - networkMap.getPathTaken().size()) * 500L));
            state.getExpiringMessageMap().put(networkMap.getMsgUID(), expiringMessage);

            // Start returning messages
            Message reponseMsg = new Message(P2P_MAP_NETWORK_RESPONSE).addObject("data", networkMap).addString("from_ip", state.getAddress().toString());
            PostMessage(reponseMsg);
        }

    }


    private void sendMapNetworkResponseMsg(P2PMsgMapNetwork networkMap) {
        InetSocketAddress nextHop = networkMap.getPreviousNode(state.getAddress());
        if (nextHop != null) {
            MinimaClient minimaClient = P2PFunctions.getClientForInetAddressEitherDirection(nextHop, getCurrentMinimaClients());
            if (minimaClient != null) {
                Message retMsg = new Message(MinimaClient.NETMESSAGE_P2P_MAP_NETWORK_RESPONSE).addObject("data", networkMap);
                log.warn("[-] P2P_MAP_NETWORK_RESPONSE sent to next hop with message uid: " + networkMap.getMsgUID());
                sendMessage(minimaClient, retMsg);
            } else {
                log.warn("[-] P2P_MAP_NETWORK_RESPONSE client not found for: " + nextHop);
            }
        }
    }

    private void processMapNetworkResponseMsg(Message zMessage) {
        P2PMsgMapNetwork networkMap = (P2PMsgMapNetwork) zMessage.getObject("data");
        String fromAddress = zMessage.getString("from_ip");

        if (networkMap == null){
            log.error("[-] P2P_MAP_NETWORK_RESPONSE  network map is null!? " + zMessage);
        }
        log.debug("[!] P2P_MAP_NETWORK_RESPONSE from: " + fromAddress +
                " Request UID " + networkMap.getRequestUID() +
                " Messages UID " + networkMap.getMsgUID() +
                " number of links: " + networkMap.getNodes().size() +
                " path: " + networkMap.getPathTaken());

        ExpiringMessage returnedMsg = state.getExpiringMessageMap().remove(networkMap.getMsgUID());
        if (returnedMsg != null) { //Should always be not null
            boolean subRequestsComplete = true;
            long delay = System.currentTimeMillis() - returnedMsg.getTimestamp();

            log.debug("[+] P2P_MAP_NETWORK_RESPONSE returned in: " + delay + "ms");

            ArrayList<P2PMsgMapNetwork> mapResponses = state.getNetworkMapRequests().get(networkMap.getRequestUID());

            if (mapResponses == null) {
                log.debug("[+] P2P_MAP_NETWORK_RESPONSE mapResponses is null");

                mapResponses = new ArrayList<>();
                mapResponses.add(networkMap);
                state.getNetworkMapRequests().put(networkMap.getRequestUID(), mapResponses);
            } else {
                log.debug("[+] P2P_MAP_NETWORK_RESPONSE mapResponses has links: " + mapResponses.size());
                mapResponses.add(networkMap);
            }

            for (ExpiringMessage msg : state.getExpiringMessageMap().values()) {
                if (msg.getMsg().isMessageType(P2P_MAP_NETWORK)) {
                    P2PMsgMapNetwork reqMap = (P2PMsgMapNetwork) zMessage.getObject("data");
                    log.debug("[!] P2P_MAP_NETWORK_RESPONSE reqUID: " + networkMap.getRequestUID() + " expiring message Req UID: " + reqMap.getRequestUID());
                    if (reqMap.getRequestUID().isEqual(networkMap.getRequestUID())) {
                        log.debug("[!] P2P_MAP_NETWORK_RESPONSE subRequestsComplete == false");
                        subRequestsComplete = false;
                    }
                }
            }

            if (subRequestsComplete) {
                ArrayList<P2PMsgMapNetwork> retMapResponses = state.getNetworkMapRequests().remove(networkMap.getRequestUID());
                if (retMapResponses != null) {
                    log.debug("[+] P2P_MAP_NETWORK_RESPONSE complete number of responses: " + retMapResponses.size());

                    P2PMsgMapNetwork retMap = new P2PMsgMapNetwork(retMapResponses, networkMap.getPathTaken(), networkMap.getRequestUID());
                    retMap.setMsgUID(state.getRequestUIDtoMsgUID().get(retMap.getRequestUID()));
                    retMap.addThisNodeFromState(state);
                    if (state.getAddress().equals(retMap.getPathTaken().get(0))) {
                        log.debug("[+] P2P_MAP_NETWORK_RESPONSE Back to start Sending to print. Num responses: " + retMap.getNodes().size());
                        PostMessage(new Message(P2P_PRINT_NETWORK_MAP_RESPONSE).addObject("data", retMap));
                    } else {
                        log.debug("[+] P2P_MAP_NETWORK_RESPONSE sending to previous node");
                        sendMapNetworkResponseMsg(retMap);
                    }
                } else {
                    log.debug("[-] P2P_MAP_NETWORK_RESPONSE no responses for: " + networkMap.getRequestUID());
                }
            }

        } else {
            log.error("[-] P2P_MAP_NETWORK_RESPONSE no expiring message for: " + networkMap.getMsgUID());
        }


    }

    private void processPrintNetworkMapRequestMsg(Message zMessage) {
        printNetworkMapRPCReq = zMessage;

        P2PMsgMapNetwork mapNetwork = new P2PMsgMapNetwork();
        log.error("[-] P2P_PRINT_NETWORK_MAP Request UID " + mapNetwork.getRequestUID());


        PostMessage(new Message(P2P_MAP_NETWORK).addObject("data", mapNetwork).addString("from_ip", state.getAddress().toString()));

    }


    private void processPrintNetworkMapResponseMsg(Message zMessage) {
        if (printNetworkMapRPCReq != null) {
            JSONObject networkMapJSON = InputHandler.getResponseJSON(printNetworkMapRPCReq);
            P2PMsgMapNetwork networkMap = (P2PMsgMapNetwork) zMessage.getObject("data");
            networkMapJSON.put("network_map", networkMap.toJSONArray());
            networkMapJSON.put("total_nodes", networkMap.getNodes().size());

            //All good
            InputHandler.endResponse(printNetworkMapRPCReq, true, "");
        } else {
            log.warn("[-] Failed to make network map");
        }
    }
}
