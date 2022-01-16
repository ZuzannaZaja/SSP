
"""How to start mininet:"""
"""To start famta_topo with default controller and without generator: sudo python runFamtar.py 1"""
"""To start famta_topo with remote controller and without generator: sudo python runFamtar.py 2"""
"""To start famta_topo with remote controller and traffic generator: sudo python runFamtar.py 3"""


from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.log import setLogLevel
import sys

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
	print "Dodawanie przeplywow"

	net.switches[0].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
	net.switches[0].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')
	net.switches[6].dpctl('add-flow', 'in_port=1,idle_timeout=0,actions=output:6')
	net.switches[6].dpctl('add-flow', 'in_port=6,idle_timeout=0,actions=output:1')
	net.switches[3].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
	net.switches[3].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')
	
	print "Generacja ruchu"

        net.hosts[0].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGRecv &')
        net.hosts[1].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGRecv &')
        net.hosts[0].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGSend ./flows -l sender.log -x receiver.log ')
        net.hosts[0].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGDec sender.log')
        net.hosts[1].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGDec receiver.log')
        net.stop()


	print "Ping"
	net.pingAll()
	net.stop()

def runController():
	"""Run basic topology with controller"""
	c0 = RemoteController(name='c0',ip='127.0.0.1', port= 6653)
	topo = FamtarTopo()
    	net = Mininet(topo=topo,host=CPULimitedHost, controller=c0, link=TCLink)
   	net.start()
    	print "Dumping host connections"
    	dumpNodeConnections(net.hosts)
    	print "Testing network connectivity"
    	net.pingAll()
    	print "Testing bandwidth between h1 and h4"
	print "Dodawanie przeplywow"

        net.switches[0].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
	net.switches[0].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')
	net.switches[6].dpctl('add-flow', 'in_port=1,idle_timeout=0,actions=output:6')
	net.switches[6].dpctl('add-flow', 'in_port=6,idle_timeout=0,actions=output:1')
	net.switches[3].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
	net.switches[3].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')

  	
    	net.stop()


def runAll():
	"""Run basic topology with controller and traffic generator"""

        c0 = RemoteController(name='c0',ip='127.0.0.1', port= 6653)
        topo = FamtarTopo()
        net = Mininet(topo=topo,host=CPULimitedHost, controller=c0, link=TCLink)
        net.start()

        print "Dumping host connections"
        dumpNodeConnections(net.hosts)
        print "Testing network connectivity"
	print "Dodawanie przeplywow"

	net.switches[0].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
	net.switches[0].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')
	net.switches[6].dpctl('add-flow', 'in_port=1,idle_timeout=0,actions=output:6')
	net.switches[6].dpctl('add-flow', 'in_port=6,idle_timeout=0,actions=output:1')
	net.switches[3].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
	net.switches[3].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')


	print "Generacja ruchu"
	net.hosts[0].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGRecv &')
	net.hosts[1].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGRecv &')
	net.hosts[0].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGSend ./flows -l sender.log -x receiver.log')
        net.hosts[0].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGDec sender.log')
	net.hosts[1].cmd('/home/floodlight/D-ITG-2.8.1-r1023/bin/ITGDec receiver.log')
        net.stop()


if __name__=='__main__':

	setLogLevel('info')

	if int(sys.argv[1]) == 1:
		runNet()
	if int(sys.argv[1]) == 2:
		runController()
	if int(sys.argv[1]) ==3:
		runAll()


