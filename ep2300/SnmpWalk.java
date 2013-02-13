package ep2300;

import java.lang.*;
import java.util.*;
import java.net.*;
import com.adventnet.snmp.snmp2.*;
import com.adventnet.snmp.snmp2.usm.*;

public class SnmpWalk {

    private SnmpOID OID;
    private SnmpSession snmpSession;
    private String username;

    public SnmpWalk(String userName, SnmpSession session, String oid) {
        this.username = userName;
        this.OID = new SnmpOID(oid);
        this.snmpSession = session;

    }

    public static ArrayList<String> execute(String username, SnmpSession session, String root_oid) {



        ArrayList<String> result = new ArrayList<String>();

        // Build GETNEXT request PDU
        SnmpPDU pdu = new SnmpPDU();
        pdu.setCommand(SnmpAPI.GETNEXT_REQ_MSG);

        // need to save the root OID to walk sub-tree
        SnmpOID oid = new SnmpOID(root_oid); //root_OID
        int rootoid[] = (int[]) oid.toValue();
        if (rootoid == null) //if don't have a valid OID for first, exit
        {
            System.err.println("Invalid OID argument! ");
            System.exit(1);
        } else {
            pdu.addNull(oid);
        }
        pdu.setUserName(username.getBytes());

        // loop for each PDU in the walk
        while (true) // until received OID isn't in sub-tree
        {
            try {
                // Send PDU and receive response PDU
                pdu = session.syncSend(pdu);
            } catch (SnmpException e) {
                System.err.println("Sending PDU" + e.getMessage());
                System.exit(1);
            }

            if (pdu == null) {
                // timeout
                System.out.println("Request timed out to!");
                System.exit(1);
            }


            // stop if outside sub-tree
            if (!isInSubTree(rootoid, pdu)) {
                //System.out.println("Not in sub tree.");
                break;
            }

            Enumeration e = pdu.getVariableBindings().elements();

            while (e.hasMoreElements()) {
                int error = 0;
                SnmpVarBind varbind = (SnmpVarBind) e.nextElement();
                // check for error
                if ((error = varbind.getErrindex()) != 0) {
                    System.out.println("Error Indication in response: "
                            + SnmpException.exceptionString((byte) error));
                    System.exit(1);
                }
                // print response pdu variable-bindings
                result.add(varbind.getVariable().toString());
            }


            // set GETNEXT_REQ_MSG to do walk
            // Don't forget to set request id to 0 otherwise next request will fail
            pdu.setReqid(0);

            SnmpOID first_oid = pdu.getObjectID(0);
            pdu = new SnmpPDU();
            pdu.setCommand(SnmpAPI.GETNEXT_REQ_MSG);
            pdu.setUserName(username.getBytes());
            pdu.addNull(first_oid);
        } // end of while true
        return result;
    }

    /** check if first varbind oid has rootoid as an ancestor in MIB tree */
    static boolean isInSubTree(int[] rootoid, SnmpPDU pdu) {
        SnmpOID objID = (SnmpOID) pdu.getObjectID(0);
        if (objID == null) {
            return false;
        }

        int oid[] = (int[]) objID.toValue();
        if (oid == null) {
            return false;
        }
        if (oid.length < rootoid.length) {
            return false;
        }

        for (int i = 0; i < rootoid.length; i++) {
            if (oid[i] != rootoid[i]) {
                return false;
            }
        }
        return true;
    }
}
