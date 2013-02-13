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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

//used to poll 
public class SnmpLoadPoll implements Runnable { //Runnable

    private SnmpSession snmpSession;
    private SnmpAPI api;
    private UDPProtocolOptions opt;
    private SnmpWalk getInOctets;
    private Router router;
    private String username;
    private String password;
    private ArrayList<String> inOctets;
    private double timePeriod; //rename
    private final String IfInOctets_OID;
    private long delay;
    private ArrayList<Router> routers;
    private long start;
    private long time_difference;

    public long getDelay() {
        return delay;
    }

    public SnmpLoadPoll(ArrayList<Router> routers, Router r, String username, String pass, double timePeriod) {

        this.router = r;
        this.username = username;
        this.password = pass;
        this.IfInOctets_OID = ".1.3.6.1.2.1.2.2.1.10";
        this.routers = routers;
        this.delay = 0;
        //Create snmp session
        api = new SnmpAPI();

        snmpSession = new SnmpSession(api);
        opt = new UDPProtocolOptions(r.getIpAddr().get(0));
        snmpSession.setProtocolOptions(opt);
        snmpSession.setVersion(3);
        snmpSession.setTimeout(2000);
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

        this.getInOctets = new SnmpWalk(username, snmpSession, this.IfInOctets_OID);
        this.inOctets = new ArrayList<String>();
        this.timePeriod = timePeriod;
    }

    public Router getRouter() {
        return this.router;
    }

    public static long SumOctets(ArrayList<String> octets) {
        Iterator octIterator = octets.iterator();
        long sum = 0;
        while (octIterator.hasNext()) {
            String tempOctValue = (String) octIterator.next();
            try {
                long value = Long.parseLong(tempOctValue);
                sum += value;
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException");
            }
        }
        return sum;

    }

    /**
     * Polls the router for inOctets an all its interfaces, calculates the load 
     * and adds it to the router object
     */
    public void run() {

        //Get inOctets from all interfaces of the router

        start = System.currentTimeMillis();
        inOctets = getInOctets.execute(this.username, this.snmpSession, this.IfInOctets_OID);
        time_difference = (start - router.getLoadInfo().getLast_timestamp()) / 1000;

        router.getLoadInfo().setLast_timestamp(start);

        long sum = SumOctets(inOctets);
        //calculate the load

        double calculatedLoad = (sum - router.getLoadInfo().getLastSumOctetsRetrieved()) / time_difference;
        if (router.getLoadInfo().getLastSumOctetsRetrieved() != 0) { //Not calculate the first load 
            (router.getLoadInfo()).getLoadOctets().add(calculatedLoad);

        }


        delay = start; 
        router.getLoadInfo().setLastSumOctetsRetrieved(sum);

        snmpSession.close();
        // stop api thread
        api.close();

    }
}
