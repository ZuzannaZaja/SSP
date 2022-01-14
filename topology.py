from mininet.topo import Topo


class FamtarTopo(Topo):

    def __init__(self, number_of_switches=7):
        Topo.__init__(self)

        leftHost = self.addHost('h1')
        rightHost = self.addHost('h2')
        switches = [self.addSwitch('s_{}'.format(i)) for i in range(1, number_of_switches+1)]

        # TODO: add link speeds here?
        switch_links = [(1, 2), (1, 6), (1, 7), (2, 3), (2, 7), (3, 4), (5, 4), (6, 5), (6, 7), (7, 3), (7, 5), (7, 4)]
        for left, right in switch_links:
            self.addLink(switches[left-1], switches[right-1])

        self.addLink(leftHost, switches[0])
        self.addLink(rightHost, switches[3])


topos = {'famtar_topo': lambda: FamtarTopo()}
