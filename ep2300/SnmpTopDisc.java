package ep2300;

import com.adventnet.snmp.snmp2.UDPProtocolOptions;
import com.adventnet.snmp.snmp2.usm.USMUserEntry;
import com.adventnet.snmp.snmp2.usm.USMUtils;

import com.adventnet.snmp.snmp2.SnmpException;
import com.adventnet.snmp.snmp2.SnmpSession;
import com.adventnet.snmp.snmp2.SnmpVar;
import com.adventnet.snmp.snmp2.SnmpVarBind;
import com.adventnet.snmp.snmp2.SnmpAPI;
import com.adventnet.snmp.snmp2.SnmpOID;
import com.adventnet.snmp.snmp2.SnmpPDU;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SnmpTopDisc implements Serializable {

    public String Host_OID = ".1.3.6.1.2.1.1.5.0";
    public String ifDesc_OID = ".1.3.6.1.2.1.2.2.1.2";
    public String ifIndex_OID = ".1.3.6.1.2.1.2.2.1.1";
    public String ipAdEntfIndex_OID = ".1.3.6.1.2.1.4.20.1.2";
    public String ipAdEntAddr_OID = ".1.3.6.1.2.1.4.20.1.1";
    public String ipAdEntNetMask_OID = ".1.3.6.1.2.1.4.20.1.3";
    public String ipRouteIfIndex_OID = ".1.3.6.1.2.1.4.21.1.2";
    public String ipRouteDest_OID = ".1.3.6.1.2.1.4.21.1.1";
    public String ipRouteNextHop_OID = ".1.3.6.1.2.1.4.21.1.7";
    public String ipRouteType_OID = ".1.3.6.1.2.1.4.21.1.8";
    public String ipRouteProto_OID = ".1.3.6.1.2.1.4.21.1.9";
    private ArrayList<String> visitedIP; //set of visited IPs
    private ArrayList<Router> DiscRouters; //set of Discovered Routers
    private SnmpSession snmpSession;
    private SnmpAPI api;
    private UDPProtocolOptions opt;

    public ArrayList<Router> getDiscRouters() {
        return DiscRouters;
    }

    public SnmpTopDisc() {
        this.visitedIP = new ArrayList<String>();
        this.DiscRouters = new ArrayList<Router>();
    }

    public void SnmpPoll(Inet4Address RouterIP, String username, String password) {
        System.out.println("Checking router with IP : " + RouterIP);
        //Create SnmpApi to make pdu requests and receive answers
        api = new SnmpAPI();
        //Create snmp session
        snmpSession = new SnmpSession(api);
        opt = new UDPProtocolOptions(RouterIP);
        snmpSession.setProtocolOptions(opt);
        snmpSession.setVersion(3);
        snmpSession.setRetries(5);
        try {
            snmpSession.open();
        } catch (SnmpException e) {
            System.err.println("Unable to open session " + e.getMessage());
            System.exit(1);
        }

        try {
            USMUtils.init_v3_parameters(username, null, USMUserEntry.MD5_AUTH, password, null, opt, snmpSession, false, USMUserEntry.NO_PRIV);

        } catch (SnmpException e) {
            System.err.println("Unable to configure v3 USM Parameters : " + e);

        }
        //Get the hostname of the router
        SnmpGET getHostName = new SnmpGET(username, this.snmpSession, Host_OID);

        String hostName = getHostName.execute().firstElement().getVariable().toString();
        System.out.println("Host name is = " + hostName);

        //Create a new router object 
        Router newrouter = new Router();
        newrouter.setHostName(hostName);

        //ifIndex
        SnmpWalk getIfIndex = new SnmpWalk(username, this.snmpSession, ifIndex_OID);
        ArrayList<String> ifIndex = getIfIndex.execute(username, this.snmpSession, ifIndex_OID);
        if (ifIndex != null) {
            newrouter.ifTable.add(ifIndex);
        } else {
            System.out.println("Null re...");
        }

        //ifDescs
        SnmpWalk getIfDescs = new SnmpWalk(username, this.snmpSession, ifDesc_OID);
        ArrayList<String> ifDescs = getIfDescs.execute(username, this.snmpSession, ifDesc_OID);
        if (ifDescs != null) {
            newrouter.ifTable.add(ifDescs);
        } else {
            System.out.println("Null re...");
        }



        //Fill ipAdTable for new router
        //ipIfIndex
        SnmpWalk getIpIfIndex = new SnmpWalk(username, this.snmpSession, ipAdEntfIndex_OID);
        ArrayList<String> IpIfIndex = getIpIfIndex.execute(username, this.snmpSession, ipAdEntfIndex_OID);
        if (IpIfIndex != null) {
            newrouter.ipAdTable.add(IpIfIndex);
        } else {
            System.out.println("Null re...");
        }

        //ipAddr
        SnmpWalk getIpAddr = new SnmpWalk(username, this.snmpSession, ipAdEntAddr_OID);
        ArrayList<String> IpAddr = getIpAddr.execute(username, this.snmpSession, ipAdEntAddr_OID);
        if (IpAddr != null) {
            newrouter.ipAdTable.add(IpAddr);
        } else {
            System.out.println("Null re...");
        }

        //ipAddr
        SnmpWalk getIpNetMask = new SnmpWalk(username, this.snmpSession, ipAdEntNetMask_OID);
        ArrayList<String> IpNetMask = getIpNetMask.execute(username, this.snmpSession, ipAdEntNetMask_OID);
        if (IpNetMask != null) {
            newrouter.ipAdTable.add(IpNetMask);
        } else {
            System.out.println("Null re...");
        }

        //Fill ipRouteTable
        SnmpWalk getIpRIndex = new SnmpWalk(username, this.snmpSession, ipRouteIfIndex_OID);
        //ipRouteIfndex
        ArrayList<String> IpRIndex = getIpRIndex.execute(username, this.snmpSession, ipRouteIfIndex_OID);
        if (IpRIndex != null) {
            newrouter.ipRouteTable.add(IpRIndex);
        } else {
            System.out.println("Null re...");
        }

        //IpRouteNextHop
        SnmpWalk getIpRNextHop = new SnmpWalk(username, this.snmpSession, ipRouteNextHop_OID);
        ArrayList<String> IpRNextHop = getIpRNextHop.execute(username, this.snmpSession, ipRouteNextHop_OID);
        if (IpRNextHop != null) {
            newrouter.ipRouteTable.add(IpRNextHop);
        } else {
            System.out.println("Null re...");
        }

        newrouter.findNeighbors();

        //at last before the recursive call
        DiscRouters.add(newrouter);
        visitedIP.addAll(newrouter.getIpAddr());

        Iterator Rneighbor = (newrouter.getNeighbors()).iterator();
        while (Rneighbor.hasNext()) {
            String neighborIP = (String) Rneighbor.next();

            if (!visitedIP.contains(neighborIP)) {
                System.out.println("Visit now : " + neighborIP);
                Inet4Address inet4Address = null;
                try {
                    inet4Address = (Inet4Address) Inet4Address.getByName(neighborIP);
                } catch (UnknownHostException ex) {
                    Logger.getLogger(SnmpTopDisc.class.getName()).log(Level.SEVERE, null, ex);
                }
                SnmpPoll(inet4Address, username, password);

            }

        }




        snmpSession.close();
        // stop api thread
        api.close();
        //Return the RouterCollection of the discovered routers
        return;

    }

    String searchHostname(String ipaddress) {

        Iterator DiscIterator = DiscRouters.iterator();
        while (DiscIterator.hasNext()) {
            Router temp = (Router) DiscIterator.next();
            if (temp.getIpAddr().contains(ipaddress)) {
                return temp.getHostName();
            }
        }


        return null;
    }

    void printDiscRouters() {
        Iterator routerIt = DiscRouters.iterator();

        while (routerIt.hasNext()) {
            Router router = (Router) routerIt.next();
            System.out.println(router.getInfo());
            System.out.println("Neighbours:");
            System.out.println("---------------------------");
            Iterator neighborIpIt = router.getNeighbors().iterator();
            while (neighborIpIt.hasNext()) {
                String NeighbourIP = (String) neighborIpIt.next();
                System.out.println(searchHostname(NeighbourIP) + ":" + NeighbourIP);
            }
        }

    }
}
