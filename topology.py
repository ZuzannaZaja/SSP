from mininet.topo import Topo


class FamtarTopo(Topo):

    def __init__(self, number_of_switches=7):
        Topo.__init__(self)

        leftHost = self.addHost('h1')
        rightHost = self.addHost('h2')
        switches = [self.addSwitch('s_{}'.format(i)) for i in range(number_of_switches)]

        # TODO: add link speeds here?
        switch_links = [(0, 1), (0, 5), (0, 6), (1, 2), (1, 6), (2, 3), (4, 3), (5, 4), (5, 6), (6, 2), (6, 4), (6, 3)] 
        for left, right in switch_links:
            self.addLink(switches[left], switches[right])

        self.addLink(leftHost, switches[0])
        self.addLink(rightHost, switches[3])


topos = {'famtar_topo': lambda: FamtarTopo()}
