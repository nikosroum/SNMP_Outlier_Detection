package ep2300;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class RouterLoadInfo implements Serializable {

    public ArrayList<Double> LoadOctets;
    private long lastSumOctetsRetrieved;
    private long last_timestamp;

    public long getLast_timestamp() {
        return last_timestamp;
    }

    public void setLast_timestamp(long last_timestamp) {
        this.last_timestamp = last_timestamp;
    }

    public long getLastSumOctetsRetrieved() {
        return lastSumOctetsRetrieved;
    }

    public void setLastSumOctetsRetrieved(long lastSumOctetsRetrieved) {
        this.lastSumOctetsRetrieved = lastSumOctetsRetrieved;
    }

    public RouterLoadInfo() {
        this.LoadOctets = new ArrayList<Double>();
        this.lastSumOctetsRetrieved = 0;
        this.last_timestamp = 0;
    }

    public ArrayList<Double> getLoadOctets() {

        return LoadOctets;
    }

    public void setLoadOctets(ArrayList<Double> LoadOctets) {
        this.LoadOctets = LoadOctets;
    }

    public double getAverage() {

        Double sum = 0.0;
        Iterator<Double> iterator = LoadOctets.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            sum += iterator.next();
            i++;
        }

        return (double) sum / i;
    }
}
