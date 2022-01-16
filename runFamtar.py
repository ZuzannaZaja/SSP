from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel

class FamtarTopo(Topo):

    def __init__(self, number_of_switches=7):
        Topo.__init__(self)

        leftHost = self.addHost('h1')
        rightHost = self.addHost('h2')
        switches = [self.addSwitch('s_{}'.format(i)) for i in range(1, number_of_switches+1)]

        # TODO: add link speeds here?
        switch_links = [(1, 2, 100), (1, 6, 1000), (1, 7, 10), (2, 3, 1000), (2, 7, 1000), (3, 4, 100), (5, 4, 100), (6, 5, 1000), (6, 7, 100), (7, 3, 100), (7, 5, 1000), (7, 4, 100)]
        for left, right, rate in switch_links:
            self.addLink(switches[left-1], switches[right-1],bw=rate)

        self.addLink(leftHost, switches[0], bw=10000)
        self.addLink(rightHost, switches[3], bw=10000)


topos = {'famtar_topo': lambda: FamtarTopo()}



def runNet():
	"""Run basic topology without controller"""
	topo = FamtarTopo()
	net = Mininet(topo=topo,host=CPULimitedHost, link=TCLink)
    	net.start()
    	print "Dumping host connections"
    	dumpNodeConnections(net.hosts)
    	print "Testing network connectivity"
	net.pingAll()
	net.stop()

def runController():
	"""Run basic topology with controller"""
	topo = FamtarTopo()
    	net = Mininet(topo=topo,host=CPULimitedHost, link=TCLink)
   	net.start()
    	print "Dumping host connections"
    	dumpNodeConnections(net.hosts)
    	print "Testing network connectivity"
    	net.pingAll()
    	print "Testing bandwidth between h1 and h4"
    	##h1, h4 = net.get('h1', 'h4')
    	##net.iperf((h1, h4))
    	net.stop()
def runAll():
	"""Run basic topology with controller and traffic generator"""


if __name__=='__main__':
    setLogLevel('info')
    runNet()
