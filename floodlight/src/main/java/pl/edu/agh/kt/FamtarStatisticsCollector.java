package pl.edu.agh.kt;


import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FamtarStatisticsCollector
{
    private static final Logger logger = LoggerFactory.getLogger(FamtarStatisticsCollector.class);
    private IOFSwitch sw;
    private ConcurrentHashMap<Integer, Long> measurements = new ConcurrentHashMap<Integer, Long>();

    public class PortStatisticsPoller extends TimerTask
    {
        private final Logger logger = LoggerFactory.getLogger(PortStatisticsPoller.class);

        @Override
        public void run()
        {
            //TODO: clean logic here
        }

        public void run1()
        {
            //TODO: make it run constantly
            //TODO: get max bitrate on link here or somewhere else
            logger.debug("run() begin");
            synchronized (FamtarStatisticsCollector.this) {
                if (sw == null) { // no switch
                    logger.error("run() end (no switch)");
                    return;
                }

                ListenableFuture<?> future;
                List<OFStatsReply> values = null;
                OFStatsRequest<?> req = null;

                req = sw.getOFFactory().buildPortStatsRequest().build();
                try {
                    if (req != null) {
                        future = sw.writeStatsRequest(req);
                        values = (List<OFStatsReply>) future.get(
                                PORT_STATISTICS_POLLING_INTERVAL * 1000 / 2,
                                TimeUnit.MILLISECONDS);
                    }

                    //TODO handle port traffic here and trigger cost changes
                    OFPortStatsReply psr = (OFPortStatsReply) values.get(0);
                    logger.info("Switch id: {}", sw.getId());
                    for (OFPortStatsEntry pse : psr.getEntries()) {
                        int portNumber = pse.getPortNo().getPortNumber();
//                        Match portNumber = pse.getMatch();
                        if (true) {
                            long txPackets = pse.getTxPackets().getValue();
//                            long txBytes = pse.getByteCount().getValue();
                            logger.info("\tmatch: {}, txBytes: {}", portNumber, txPackets);


                            if (measurements.get(portNumber) != null) {
                                Long last = measurements.get(portNumber);
                                double rate = (txPackets - last) / 3.0 / 1e9;
                                //TODO link cost update logic here
                                //FamtarTopology.getInstance().updateLinkCost();
                                logger.info("\tmatch: {}, txBitRate: {}", portNumber, rate);
                            } else {
                                measurements.put(portNumber, txPackets);
                            }
                        }
                    }

                } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                    logger.error("Error during statistics polling", ex);
                }
            }
            logger.debug("run() end");
        }
    }

    public static final int PORT_STATISTICS_POLLING_INTERVAL = 3000; // in ms
    private static FamtarStatisticsCollector singleton;

    private FamtarStatisticsCollector(IOFSwitch sw)
    {
        this.sw = sw;
        new Timer().scheduleAtFixedRate(new PortStatisticsPoller(), 0, PORT_STATISTICS_POLLING_INTERVAL);
    }

    public static FamtarStatisticsCollector getInstance(IOFSwitch sw)
    {
//        logger.debug("getInstance() begin");
        synchronized (FamtarStatisticsCollector.class) {
            if (singleton == null) {
//                logger.debug("Creating FamtarStatisticsCollector singleton");
                singleton = new FamtarStatisticsCollector(sw);
            }
        }

//        logger.debug("getInstance() end");
        return singleton;
    }
}
