"""sudo mn --custom topology.py --topo famtar_topo --link=tc"""
from mininet.topo import Topo


class FamtarTopo(Topo):

    def __init__(self, number_of_switches=7):
        Topo.__init__(self)

        leftHost = self.addHost('h1')
        rightHost = self.addHost('h2')
        switches = [self.addSwitch('s_{}'.format(i)) for i in range(1, number_of_switches+1)]
	
	switch_links = [(1, 2, 10), (1, 6, 10), (1, 7, 10), (2, 3, 10), (2, 7, 10), (3, 4, 10), (5, 4, 10), (6, 5, 10), (6, 7, 10), (7, 3, 10), (7, 5, 10), (7, 4, 10)]
        for left, right, rate in switch_links:
            self.addLink(switches[left-1], switches[right-1], bw=rate)

        self.addLink(leftHost, switches[0], bw=10)
        self.addLink(rightHost, switches[3], bw=10)

topos = {'famtar_topo': lambda: FamtarTopo()}
