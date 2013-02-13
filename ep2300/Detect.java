
package ep2300;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Detect {

    String username;
    String password;
    static ArrayList<Router> routers;

    public static void main(String... args) throws IOException, ClassNotFoundException {
        String filename = "test.dat";
        String time_interval = null;
        String samples = null;
        int observation_samples = 0;
        boolean onlySD = false;

        if (args.length >= 3) {
            samples = args[2];
            time_interval = args[1];
            observation_samples = Integer.parseInt(args[0]);
            if (args.length>3 && args[3].equals("-SD")) {
                onlySD = true;
            }
        } else {
            System.out.println("java ep2300 observation_set time_interval samples [-SD]");
            System.exit(1);
        }

        try {
            FileInputStream fis = new FileInputStream(filename);

            ObjectInputStream ois = new ObjectInputStream(fis);
            Object obj = ois.readObject();
            routers = new ArrayList<Router>();
            routers = (ArrayList) obj;


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        //3rd parameter is for using only SD - true only for SD false for 3 methods
        RouterPoller rpoller = new RouterPoller(samples, time_interval, routers.size(), onlySD, observation_samples);
        //Start the polling method
        try {
            rpoller.pollRouterLoad(routers);
        } catch (InterruptedException ex) {
            Logger.getLogger(Detect.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
