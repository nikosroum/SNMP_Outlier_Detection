package ep2300;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;


public class Router implements Serializable{
private static final long serialVersionUID = -5302010108271068350L;
    String hostName;
    public ArrayList<ArrayList<String>> ifTable;
    public ArrayList<ArrayList<String>> ipAdTable;
    private RouterLoadInfo loadinfo;

    public void setLoadinfo(RouterLoadInfo loadinfo) {
        this.loadinfo = loadinfo;
    }
    
    
    public ArrayList<ArrayList<String>> getIpAdTable() {
        return ipAdTable;
    }

    public void setIpAdTable(ArrayList<ArrayList<String>> ipAdTable) {
        this.ipAdTable = ipAdTable;
    }

    public ArrayList<ArrayList<String>> getIpRouteTable() {
        return ipRouteTable;
    }

    public void setIpRouteTable(ArrayList<ArrayList<String>> ipRouteTable) {
        this.ipRouteTable = ipRouteTable;
    }
    public ArrayList<ArrayList<String>> ipRouteTable;
    private ArrayList<String> neighbors; //list of link level neighbors

    public Router() {

        this.ifTable = new ArrayList<ArrayList<String>>();
        this.ipAdTable = new ArrayList<ArrayList<String>>();
        this.ipRouteTable = new ArrayList<ArrayList<String>>();
        this.neighbors = new ArrayList<String>();
        this.loadinfo = new RouterLoadInfo();
    }

    public Router(String hostname, ArrayList<String> neighbours) {
        this.ifTable = new ArrayList<ArrayList<String>>();
        this.ipAdTable = new ArrayList<ArrayList<String>>();
        this.ipRouteTable = new ArrayList<ArrayList<String>>();
        this.hostName = hostname;
        this.neighbors = neighbours;
        
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public ArrayList<ArrayList<String>> getIfTable() {
        return ifTable;
    }

    public void setIfTable(ArrayList<ArrayList<String>> ifTable) {
        this.ifTable = ifTable;
    }

    public ArrayList<String> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(ArrayList<String> neighbors) {
        this.neighbors = neighbors;
    }

    public ArrayList<String> getIpAddr() {
        return ipAdTable.get(1);
    }

    void findNeighbors() {
        ArrayList<String> Myaddresses = getIpAddr();
        Iterator nexthop = ipRouteTable.get(1).iterator();
        while (nexthop.hasNext()) {
            String candidateN = (String) nexthop.next();
            boolean tobeadded = true;
            //check if it is an if Addr
            Iterator ifadd = Myaddresses.iterator();
            while (ifadd.hasNext()) {
                if (((String) ifadd.next()).equals(candidateN)) {
                    tobeadded = false;
                }
            }

            // check if already a neighbour
            Iterator neighbor = neighbors.iterator();
            while (neighbor.hasNext()) {
                if (((String) neighbor.next()).equals(candidateN)) {
                    tobeadded = false;
                    break;
                }
            }

            //Add to neighbours
            if (tobeadded) {
                neighbors.add(candidateN);
            }
        }
    }

    public String getInfo() {
        String result;
        result = "\n\nHostname: " + this.hostName + "\n==========================\nInterface\tIP\tNetmask\n---------------------------\n";

        ArrayList<String> ifDescs = getIfTable().get(1);
        ArrayList<String> ipAddresses = getIpAdTable().get(1);
        ArrayList<String> ipNetmasks = getIpAdTable().get(2);
        int ifcount = 0;

        Iterator IfIterator = getIfTable().get(0).iterator();
        while (IfIterator.hasNext()) {
            String ifIndex = (String) IfIterator.next();

            Iterator IpIterator = getIpAdTable().get(0).iterator();
            int ipcount = 0;
            while (IpIterator.hasNext()) {
                String ipIfIndex = (String) IpIterator.next();
                if (ipIfIndex.equals(ifIndex)) {
                    result = result + ifDescs.get(ipcount) + "\t" + ipAddresses.get(ipcount) + "\t" + ipNetmasks.get(ipcount) + "\n";

                }
                ipcount++;
            }
            ifcount++;
        }


        return result;
    }

    RouterLoadInfo getLoadInfo() {
        
        return this.loadinfo;
    }

   
}
