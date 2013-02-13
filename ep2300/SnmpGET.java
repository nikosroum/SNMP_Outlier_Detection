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
import java.util.Vector;

public class SnmpGET {

    private SnmpOID OID;
    private SnmpSession snmpSession;
    private Vector<SnmpVarBind> value;
    private String username;

    public SnmpGET(String userName, SnmpSession session, String oid) {
        this.username = userName;
        this.OID = new SnmpOID(oid);
        this.snmpSession = session;
        this.value = new Vector<SnmpVarBind>();
    }

    public Vector<SnmpVarBind> execute() {

        //Build the request PDU
        SnmpPDU snmpPDU = new SnmpPDU();
        snmpPDU.setCommand(SnmpAPI.GET_REQ_MSG);
        snmpPDU.setUserName(this.username.getBytes());
        snmpPDU.addNull(OID);

        try {
            snmpPDU = snmpSession.syncSend(snmpPDU);
        } catch (SnmpException e) {
            System.err.println("Error while Sending PDU: " + e);
        }
        if (snmpPDU == null) {
            System.out.println("Request timed out");
        } else {
            if (snmpPDU.getErrstat() == 0) {
                value.add(((SnmpVarBind) snmpPDU.getVariableBinding(0)));
            } else {
                System.out.println(snmpPDU.getError());
            }
        }
        return value;
    }
}
