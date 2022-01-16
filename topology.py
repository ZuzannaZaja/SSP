"""sudo mn --custom topology.py --topo famtar_topo --link=tc"""
from mininet.topo import Topo


class FamtarTopo(Topo):

    def __init__(self, number_of_switches=7):
        Topo.__init__(self)

        leftHost = self.addHost('h1')
        rightHost = self.addHost('h2')
        switches = [self.addSwitch('s_{}'.format(i)) for i in range(1, number_of_switches+1)]
	
	switch_links = [(1, 2, 100), (1, 6, 1000), (1, 7, 10), (2, 3, 1000), (2, 7, 1000), (3, 4, 100), (5, 4, 100), (6, 5, 1000), (6, 7, 100), (7, 3, 100), (7, 5, 1000), (7, 4, 100)]
        for left, right, rate in switch_links:
            self.addLink(switches[left-1], switches[right-1], bw=rate)

        self.addLink(leftHost, switches[0])
        self.addLink(rightHost, switches[3])

topos = {'famtar_topo': lambda: FamtarTopo()}
