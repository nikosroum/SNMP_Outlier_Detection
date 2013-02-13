
package ep2300;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RouterPoller {
    private HashMap<String, String> Outliers;
    private ScheduledExecutorService scheduler;
    private int samplesNumber;     // The number of samples to get from routers
    private int timePeriod;        // The period between 2 consequent polls
    private double Sd;
    private int instance;
    private int observingSamples;
    ArrayList<Double> DS;
    ///////////////////////////////////////////////
    private double Mean;
    private boolean methodSD;
    private String SD_filename = "1SD_file.txt";
    private String MAD_filename = "1MAD_file.txt";
    private String Tukey_filename = "1Tukey_file.txt";
    BufferedWriter outSD;
    BufferedWriter outMAD;
    BufferedWriter outTU;

    ///////////////////////////////////////////////
    /**
     * Constructor for RouterPoller 
     */
    public RouterPoller(String samples, String time_interval, int size, boolean method, int observation) {
        try {

            scheduler = Executors.newScheduledThreadPool(size + 1);
            this.samplesNumber = Integer.parseInt(samples);
            this.timePeriod = Integer.parseInt(time_interval);
            this.Outliers = new HashMap<String, String>();
            this.observingSamples = observation;
            this.Sd = 0.0;
            this.Mean = 0.0;
            this.DS = new ArrayList<Double>();
            this.methodSD = method;
            outSD = new BufferedWriter(new FileWriter(SD_filename));
            outMAD = new BufferedWriter(new FileWriter(MAD_filename));
            outTU = new BufferedWriter(new FileWriter(Tukey_filename));
        } catch (Exception e) {
            System.err.println("RouterLoad initialization failed.");
            System.exit(1);
        }
    }

    /**
     * Polling the routers
     * 
     * @param discRouters
     *            - The collection of the discovered Routers
     */
    public void pollRouterLoad(final ArrayList<Router> discRouters) throws InterruptedException, IOException {

        // Start a load polling thread for every discovered router

        // Create a scheduled thread pool executor for the threads we will need
        SnmpLoadPoll poller;

        SnmpLoadPoll[] pollers;
        //Init RouterLoadInfo Discovery phase hadn't done this.
        for (int i = 0; i < discRouters.size(); i++) {
            Router r = discRouters.get(i);
            r.setLoadinfo(new RouterLoadInfo());
        }
        //Keep a timestamp at the beggining to calculate the total time of the observation period
        long T0 = System.currentTimeMillis();
        System.out.println("Entering Observation period...");
        //Start the Observetion
        int samples = 0;
        int init_size = observingSamples;
        while (DS.size() < observingSamples) {
            //Need at least 3 instances
            if (samples > 2) {
                firstcalcSd(discRouters);
            }
            final ScheduledExecutorService scheduler;
            System.out.println("Observation Sample: " + samples);
            long t0 = System.currentTimeMillis();
            //Create a thread pool to handle each router's thread
            scheduler = Executors.newScheduledThreadPool(discRouters.size());
            poller = null;
            pollers = new SnmpLoadPoll[discRouters.size()];

            for (int i = 0; i < discRouters.size(); i++) {
                Router r = discRouters.get(i);
                //Create thread for this router
                poller = new SnmpLoadPoll(discRouters, r, "EP2300_student", "netmanagement", (double) timePeriod);
                pollers[i] = (SnmpLoadPoll) poller;
                //Assign thread to scheduler
                final ScheduledFuture FuturePoller;
                FuturePoller = scheduler.schedule(poller, (long) 0, TimeUnit.SECONDS);
            }

            //Wait until all finished
            long t1 = System.currentTimeMillis();

            boolean sleep = true;
            while (sleep) {
                //check if all routers have filled their Octets Array
                boolean all = true;
                for (int i = 1; i < discRouters.size(); i++) {
                    if (discRouters.get(i - 1).getLoadInfo().getLoadOctets().size() != discRouters.get(i).getLoadInfo().getLoadOctets().size()) {
                        all = false;
                    }
                }
                if (all) {
                    sleep = false;
                }

            }

            scheduler.shutdown();

            long end = System.currentTimeMillis();

            scheduler.awaitTermination(timePeriod + (long) ((end - t1) / 1000), TimeUnit.SECONDS);

            System.out.println("End of observation sample " + samples + "\n");
            samples++;
            
            if (DS.size() >= 2) {

                firstcheck(discRouters);
            }
            //If sample was completed before the time period then wait till time period.
            long now = System.currentTimeMillis();

            if ((now - t0) < Long.valueOf(timePeriod * 1000)) {
                long diff = Long.valueOf(timePeriod * 1000) - (now - t0);
                System.out.println("Wait for " + diff);
                Thread.sleep(diff);
            }

            //check for outlier

            System.out.println("Total time: " + (System.currentTimeMillis() - t0) + " milisecs");
            System.out.println("================\n");
        }

        //end of training phase
        System.out.println("Total Observation time: " + (System.currentTimeMillis() - T0));
        System.out.println("========\nEnd of Observation Set\n========");

        //Live traffic monitoring
        //keep timestamp to calculate the total time of live monitoring
        long start_time = System.currentTimeMillis();

        samples = observingSamples;
        while (samples < (observingSamples+samplesNumber)) {

            //Calculate SD and Mean at the beggining of each sample

            Sd = calcSD(discRouters);

            long t0 = System.currentTimeMillis();
            System.out.println("Sample: " + samples);
            //Create a thread poll to handle polling threads of the routers
            final ScheduledExecutorService scheduler;
            scheduler = Executors.newScheduledThreadPool(discRouters.size());

            poller = null;
            pollers = new SnmpLoadPoll[discRouters.size()];
            for (int i = 0; i < discRouters.size(); i++) {

                Router r = discRouters.get(i);
                //create SnmpLoadPoll thread for this router
                poller = new SnmpLoadPoll(discRouters, r, "EP2300_student", "netmanagement", (double) timePeriod);
                pollers[i] = (SnmpLoadPoll) poller;
                final ScheduledFuture FuturePoller;
                //Assign thread to scheduler
                FuturePoller = scheduler.schedule(poller, (long) 0, TimeUnit.SECONDS);
            }


            long t1 = System.currentTimeMillis();

            boolean sleep = true;
            while (sleep) {
                boolean all = true;
                for (int i = 1; i < discRouters.size(); i++) {

                    if (discRouters.get(i - 1).getLoadInfo().getLoadOctets().size() != discRouters.get(i).getLoadInfo().getLoadOctets().size()) {
                        all = false;
                    }

                }
                if (all) {
                    sleep = false;
                }

            }

            scheduler.shutdown();

            long end = System.currentTimeMillis();

            instance = samples ;
            searchforOutliers(discRouters);
            System.out.println("End of sample " + samples);
            samples++;
            //Check if sample is completed before timeperiod.If so, then wait.
            long now = System.currentTimeMillis();
            if ((now - t0) < Long.valueOf(timePeriod * 1000)) {
                long diff = Long.valueOf(timePeriod * 1000) - (now - t0);
                System.out.println("Wait for " + diff);
                Thread.sleep(diff);
            }
            System.out.println("Total time: " + (System.currentTimeMillis() - t0) + " milisecs\t");
            System.out.println("================");

        }

        long end_time = System.currentTimeMillis();
        System.out.println("Total time for live traffic : " + (end_time - start_time));

        //Write all global states to a file.
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter("1DS_file.txt"));

            for (int i = 0; i < DS.size(); i++) {
                String text = Double.toString(DS.get(i));
                text = text + "\n";
                out.write(text);
            }
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(RouterPoller.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Close files for detection schemes
        outTU.close();
        outSD.close();
        outMAD.close();

        System.out.println("=Printing-Outliers=");
        List keys = new ArrayList(Outliers.keySet());
        Collections.sort(keys);
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i).toString();
            String value = Outliers.get(key).toString();

            System.out.println(key + "\t" + value);
        }
        
        System.exit(0);

    }

    public double calcSD(ArrayList<Router> routers) {
        double sd = 0.0;
        double big_square = 0.0;
        Mean = calcAverage(routers);
        int end = DS.size() - 1;
        System.out.println("--Calculate SD-- ");
        int limit = ((DS.size() - observingSamples));
        //int limit = ((end - observingSamples) + 2);
        for (int i = end; i >=limit; i--) {
            //If there have been outliers, outliers should not be included to the calculation
            if (!Outliers.isEmpty()) {
                if (Outliers.containsKey(Integer.toString(i))) {
                    if (limit > 0) {
                        limit--;
                        System.out.println("Not calculating " + i + "\tlimit " + limit);
                    }
                } else {
                    
                    big_square = big_square + Math.pow((DS.get(i) - Mean), 2);
                }
            } else {
                big_square = big_square + Math.pow((DS.get(i) - Mean), 2);
            }
        }

        sd = Math.sqrt((double) big_square / (double) observingSamples );
        if (sd == 0) {
            sd = Sd;
        }

        System.out.println("SD= " + sd + "======\n");
        return sd;

    }

    public double calcAverage(ArrayList<Router> routers) {

        //Mean value calculation
        double sum = 0.0;
        int curr_sam_no = routers.get(0).getLoadInfo().getLoadOctets().size();
        int end = curr_sam_no - 1;

        if (DS.size() == 0) { //Run only once after Observation period
            System.out.println("...First time Average...");
            for (int j = 0; j <= end; j++) {

                sum = 0.0;
                for (int i = 0; i < routers.size(); i++) {
                    sum = sum + (double) routers.get(i).getLoadInfo().getLoadOctets().get(j);
                }
                DS.add(sum);
            }
            sum = 0.0;
            System.out.println("-----CalcAverage-----");
            for (int i = 0; i < DS.size(); i++) {
                System.out.println(DS.get(i));//"DS(" + i + ") = " +
            }
            for (int i = 0; i < DS.size(); i++) {

                sum = sum + DS.get(i);
            }
        } else {
            
            sum = 0.0;
            int start = (DS.size() - 1);
            int limit = DS.size() - observingSamples;
            for (int i = start; i >= limit; i--) {
                //If there have been outliers, outliers should not be included to the calculation
                if (!Outliers.isEmpty()) {
                    if (Outliers.containsKey(Integer.toString(i))) {
                        if (limit > 0) {
                            limit--;
                            System.out.println("Not calculating " + i + "\tlimit " + limit);
                        }

                    } else {
                        sum = sum + DS.get(i);
                    }
                } else {
                    sum = sum + DS.get(i);
                }
            }
        }
        
        double mean = (double) sum / (double) observingSamples;
        if (sum == 0) {//in case all samples were outlier
            mean = Mean;
        }
        System.out.println("Mean = " + mean);
        return mean;
    }

    public void searchforOutliers(ArrayList<Router> routers) throws IOException {
        //Method to detect for outliers
        int last = routers.get(0).getLoadInfo().getLoadOctets().size() - 1;
        double sum = 0.0;
        //calculate the last global state
        for (int i = 0; i < routers.size(); i++) {
            sum = sum + (double) routers.get(i).getLoadInfo().getLoadOctets().get(last);
        }

        double prob = 0.0;
        boolean sd_result = SDDetect(sum);

        //Only method SD will be used and if sd detection is true, put to outliers
        if (methodSD && sd_result) {
            Outliers.put(Integer.toString(instance), Double.toString(sum));
        }

        if (!methodSD) {

            if (TukeyDetect(sum)) {
                prob = prob + 0.33;
            }
            if (MADeDetect(sum)) {
                prob = prob + 0.33;
            }
            if (sd_result) {
                prob = prob + 0.33;
            }

            if (prob >= 0.66) {
                Outliers.put(Integer.toString(instance), Double.toString(sum));
                System.out.println("Found Outlier with probability of " + prob+"\t[ALARM!!!]");
            }
        }
        
        DS.add(sum);

    }

    private boolean SDDetect(double sum) throws IOException {
        //SD Detection Scheme
        System.out.println("===SD Detection method===");

        //Write upper bound to file 
        String text = Double.toString((Mean + 3 * Sd));
        text = text + "\n";
        outSD.write(text);
        outSD.flush();

        //Check if value is an outlier
        if ((sum > (Mean + 3 * Sd))||(sum > (Mean - 3 * Sd))) {
            System.out.println("!!!!!Found Outlier!!!!! according to SD\t" + sum);
            return true;
        }
        System.out.println("=========================");
        return false;
    }

    private boolean TukeyDetect(double sum) throws IOException {
        //Tukey Detection Scheme
        System.out.println("===Tukey Detection method===");
        ArrayList<Double> original = new ArrayList<Double>();

        //Take observation window set with no outliers
        int start = (DS.size() - 1);
        int limit = DS.size() - observingSamples;
        for (int i = start; i >= limit; i--) {
            if (!Outliers.isEmpty()) {
                if (Outliers.containsKey(Integer.toString(i))) {
                    if (limit > 0) {
                        limit--;
                    }
                } else {
                    original.add(DS.get(i));
                }
            } else {
                original.add(DS.get(i));
            }
        }
        //Sort set
        Collections.sort(original);

        double IQR = 0.0;
        double q3 = 0.0;
        double q1 = 0.0;
        //if size is odd
        if (original.size() % 2 == 1) {
            int subset_size = original.size() / 2;
            //if subset is odd
            if (subset_size % 2 == 1) {
                int index1 = subset_size / 2;
                //Median at each side lies at the center
                q1 = original.get(subset_size - index1 - 1);
                q3 = (original.get(subset_size + 1 + index1));
            } else {
                //subset is even
                int index1 = subset_size / 2 - 1;
                //Median at each side is the mean value of the center values
                q1 = (original.get(index1) + original.get(index1 + 1)) / 2;
                q3 = (original.get(index1 + subset_size + 1) + (original.get(index1 + subset_size + 2))) / 2;
            }
        } else {//set size is even
            int index1 = original.size() / 2;
            int index2 = original.size() / 2 - 1;
            int subset_size = original.size() / 2;
            //if subset is odd
            if (subset_size % 2 == 0) {
                index1 = subset_size / 2 - 1;
                //Median at each side is the mean value of the center values
                q1 = (original.get(index1) + original.get(index1 + 1)) / 2;
                q3 = (original.get(index1 + subset_size) + (original.get(index1 + subset_size + 1))) / 2;
            } else {
                //Subset is odd
                index1 = subset_size / 2;
                //Median at each side lies at the center
                q1 = original.get(index1);
                q3 = original.get(index1 + subset_size);
            }
        }
        //Calculate IQR
        IQR = q3 - q1;

        //Write upper bound to file
        String txt = Double.toString((q3 + 1.5 * IQR)) + "\n";
        outTU.write(txt);
        outTU.flush();
        //check value 
        if ((sum > q3 + 1.5 * IQR) ||(sum < q1 - 1.5 * IQR)) {
            System.out.println("!!!!!Found Outlier!!!!! according to Tukey\t" + sum);
            return true;
        }
        System.out.println("===============================");
        return false;
    }

    private boolean MADeDetect(double sum) throws IOException {
        //MADe Detection Scheme

        System.out.println("===MADe Detection method===");

        ArrayList<Double> original = new ArrayList<Double>();

        //Take observation window set with no outliers
        int start = (DS.size() - 1);
        int limit = DS.size() - observingSamples ;
        for (int i = start; i >= limit; i--) {
            if (!Outliers.isEmpty()) {
                if (Outliers.containsKey(Integer.toString(i))) {
                    if (limit > 0) {
                        limit--;
                    }

                } else {
                    original.add(DS.get(i));
                }
            } else {
                original.add(DS.get(i));
            }
        }
        //Sort set
        Collections.sort(original);
        double median = 0.0;

        if (original.size() % 2 == 1) {
            //size is odd median lies at the center
            median = original.get((original.size() / 2) + 1);
        } else {
            //size is even
            int index1 = original.size() / 2 - 1;
            //Median is the mean value of the center values
            median = (original.get(index1) + original.get(index1 + 1)) / 2;
        }

        ArrayList<Double> Difset = new ArrayList<Double>();
        for (int i = 0; i < original.size(); i++) {
            Difset.add(Math.abs(original.get(i) - median));
        }
        Collections.sort(Difset);

        double medianDif = 0.0;

        if (Difset.size() % 2 == 1) {
            //size is odd median lies at the center
            medianDif = Difset.get((Difset.size() / 2) + 1);
        } else {
            //size is even
            int index1 = Difset.size() / 2 - 1;
            //Median at each side is the mean value of the center values
            medianDif = (Difset.get(index1) + Difset.get(index1 + 1)) / 2;
        }

        double MADe = 1.483 * medianDif;

        //Write upper bound to file
        String text = Double.toString((median + 3 * MADe));
        text = text + "\n";
        outMAD.write(text);
        outMAD.flush();

        if ((sum > (median + 3 * MADe))||(sum < (median - 3 * MADe))) {
            System.out.println("!!!!!Found Outlier!!!!! according to Mad-E\t" + sum);
            return true;
        }
        System.out.println("===============================");
        return false;
    }

    private double firstcalcSd(ArrayList<Router> routers) {
        double sd = 0.0;
        double big_square = 0.0;
        Mean = firstcalcAverage(routers);

        System.out.println("--Calculate SD-- ");


        for (int i = 0; i < DS.size(); i++) {

            big_square = big_square + Math.pow((DS.get(i) - Mean), 2);
        }

        sd = Math.sqrt((double) big_square / (double) DS.size());
        if (sd == 0) {
            sd = Sd;
        }
        Sd = sd;
        System.out.println("SD= " + sd + "======\n");
        return sd;

    }

    private double firstcalcAverage(ArrayList<Router> routers) {

        //Mean value calculation
        double sum = 0.0;

        int end = routers.get(0).getLoadInfo().getLoadOctets().size() - 1;

        if (DS.size() == 0) { //Run only once after Observation period
            
            for (int j = 0; j <= end; j++) {

                sum = 0.0;
                for (int i = 0; i < routers.size(); i++) {
                    sum = sum + (double) routers.get(i).getLoadInfo().getLoadOctets().get(j);
                }
                DS.add(sum);
                
            }
            sum = 0.0;
            System.out.println("-----CalcAverage-----");
           /*
            for (int i = 0; i < DS.size(); i++) {
                System.out.println(DS.get(i));//"DS(" + i + ") = " +
            }*/
            for (int i = 0; i < DS.size(); i++) {

                sum = sum + DS.get(i);
            }
        } else {
            for (int i = 0; i < DS.size(); i++) {
                sum = sum + DS.get(i);

            }
        }
        
        double mean = (double) sum / (double) DS.size();
        if (sum == 0) {//in case all samples were outlier
            mean = Mean;
        }
        System.out.println("Mean = " + mean);
        return mean;

    }

    private void firstcheck(ArrayList<Router> routers) throws IOException {
        double sum = 0.0;
        //SD Detection Scheme
        System.out.println("===First Check===");
        int last = routers.get(0).getLoadInfo().getLoadOctets().size() - 1;
        //calculate sum
        for (int i = 0; i < routers.size(); i++) {
            sum = sum + routers.get(i).getLoadInfo().getLoadOctets().get(last);
        }
        //Write upper bound to file 
        String text = Double.toString((Mean + 3 * Sd));
        text = text + "\n";


        //Check if value is an outlier
        if ((sum > (Mean + 3 * Sd))) {
            System.out.println("!!!!!Found Outlier!!!!! according to SD\t" + sum);

        } else {
            outSD.write(text);
            outSD.flush();
            DS.add(sum);
            
        }
        System.out.println("=========================");


    }
}
