package pl.edu.agh.kt;

import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.topology.ITopologyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//TODO: this may not be needed because we use the internal service to monitor topology changes
public class FamtarTopologyListener implements ITopologyListener
{
    protected static final Logger logger = LoggerFactory.getLogger(FamtarTopologyListener.class);

    @Override
    public void topologyChanged(List<LDUpdate> linkUpdates)
    {
//        // TODO: inform Routing about topology changes
//        // TODO: are link speeds reported here?
//        logger.debug("Received topology status");
//        final FamtarTopology famtarTopology = FamtarTopology.getInstance();
//        for (ILinkDiscovery.LDUpdate update : linkUpdates) {
//            switch (update.getOperation()) {
//                case LINK_UPDATED:
//                    logger.debug("Link updated. Update {}", update.toString());
//                    famtarTopology.addLink(update);
//                    break;
//                case LINK_REMOVED:
//                    logger.debug("Link removed. Update {}", update.toString());
//                    famtarTopology.removeLink(update);
//                    break;
//                case SWITCH_UPDATED:
//                    logger.debug("Switch updated. Update {}", update.toString());
//                    break;
//                case SWITCH_REMOVED:
//                    logger.debug("Switch removed. Update {}", update.toString());
//                    break;
//                default:
//                    break;
//            }
//            //TODO: trigger a different update here
////            if (linkUpdates.size() > 0) {
////                famtarTopology.calculatePaths();
////            }
//        }
    }
}
