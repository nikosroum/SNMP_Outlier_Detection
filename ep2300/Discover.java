package ep2300;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.*;

public class Discover {

    static String username;
    static String password;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        username = "EP2300_student";
        password = "netmanagement";
        String filename = "test.dat";
        Inet4Address inet4Address = null;
        if (args.length == 1) {
            try {
                inet4Address = (Inet4Address) Inet4Address.getByName(args[0]);
                if (!inet4Address.isReachable(5)) {
                    throw new Exception("Host is unreachable!");
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        } else if (args.length == 5) {

            try {
                inet4Address = (Inet4Address) Inet4Address.getByName(args[0]);
                if (!inet4Address.isReachable(5)) {
                    throw new Exception("Host is unreachable!");
                }
                username = args[2];
                password = args[4];
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
        } else {
            System.out.println("java ep2300 ip_addr");
            System.exit(1);
        }
        long t0 = System.currentTimeMillis();
        SnmpTopDisc discovery = new SnmpTopDisc();
        discovery.SnmpPoll(inet4Address, username, password);
        
        System.out.println("Discovery completed in"+(System.currentTimeMillis()-t0)+"milisecs!\n-------------------------------------------\n");
        discovery.printDiscRouters();
        
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(discovery.getDiscRouters());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.exit(0);

    }
}