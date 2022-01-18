# SSP
Famtar implementation in SDN 
To DO:
- topologia w .py i pdf 17.11  - [Link do topologii](https://drive.google.com/file/d/1A1mx-bOT8FbdT1UVJ90aDUqZbWwauzwm/view?usp=sharing)

Uruchomienie topologii:

```
sudo mn --custom topology.py --topo famtar_topo --controller=remote,ip=127.0.0.1,port=6653
```
# Jak uruchamiać minineta 
## Ręcznie
```
sudo mn --custom topology.py --topo famtar_topo  --link=tc
+controller jeśli masz działający, tutaj uruchamia sie domyślny

```
Topologia zawiera ustawione przepustowości 10, 100 lub 1000 Mbits/s, stąd --link=tc (jak chcesz zmienic to w topology.py zmien trzecią wartość w linksach

### Dodawanie przepływów na ścieżce h1-sw1-sw7-sw4-h2
```
//wklej to do minineta
sh ovs-ofctl add-flow s_1 in_port=4,actions=output:3
sh ovs-ofctl add-flow s_7 in_port=1,actions=output:6
sh ovs-ofctl add-flow s_4 in_port=3,actions=output:4
sh ovs-ofctl add-flow s_4 in_port=4,actions=output:3
sh ovs-ofctl add-flow s_7 in_port=6,actions=output:1
sh ovs-ofctl add-flow s_1 in_port=3,actions=output:4
```
### Generowanie ruchu
W tym momencie zakladam ze masz zainstalowany generator z generator.md
( /home/floodlight/D-ITG-2.8.1-r1023/bin/ITGRecv ta ścieżka zależy gdzie zainstalowałes generator)
```
h2 /home/floodlight/D-ITG-2.8.1-r1023/bin/ITGRecv & //odbieranie na h2
h1 /home/floodlight/D-ITG-2.8.1-r1023/bin/ITGSend ./flows -l sender.log -x receiver.log  //wysyłanie z h1 przepływów, które są zdefiniowane w pliku flows
h2 /home/floodlight/D-ITG-2.8.1-r1023/bin/ITGDec receiver.log  //wyświetla statystyki

```

## Test automatyczny
W pliku runFamtar.py znajdują sie testy, które po prostu się uruchamia z parametrem a one wlączają minineta, ustawiają przepływy, generują ruch i wyświetlają wyniki.

```
python runFamtar.py 1 //uruchamia sieć z domyslnym kontrolerem, ustawia przeplywy, generuje ruch z flows, wyświetla wyniki robi pingi i wychodzi z sieci
python runFamtar.py 3 //uruchamia sieć z remote kontrolerem, ustawia przeplywy, generuje ruch flows, wyświetla wyniki i wychodzi z sieci

```

W testach jest dodany fragment, który dodaje przepływy do tablicy ( usun jak juz nie potrzebujesz )
```
net.switches[0].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
net.switches[0].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')
net.switches[6].dpctl('add-flow', 'in_port=1,idle_timeout=0,actions=output:6')
net.switches[6].dpctl('add-flow', 'in_port=6,idle_timeout=0,actions=output:1')
net.switches[3].dpctl('add-flow', 'in_port=4,idle_timeout=0,actions=output:3')
net.switches[3].dpctl('add-flow', 'in_port=3,idle_timeout=0,actions=output:4')

```

