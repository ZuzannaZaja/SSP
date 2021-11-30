# D-ITG
* http://traffic.comics.unina.it/software/ITG/
* http://traffic.comics.unina.it/software/ITG/manual/D-ITG-2.8.1-manual.pdf
## docker and mininet
```
docker run -it --rm --privileged -e DISPLAY \
             -v /tmp/.X11-unix:/tmp/.X11-unix \
             -v /lib/modules:/lib/modules \
             iwaseyusuke/mininet
```
plus
```
$ apt-get update
$ apt-get -y install make g++ wget
$ apt-get -y install unzip
$ wget   http://traffic.comics.unina.it/software/ITG/codice/D-ITG-2.8.1-r1023-src.zip
$ unzip D-ITG-2.8.1-r1023-src.zip
$ cd D-ITG-2.8.1-r1023/src
$ make
```

## odbiorca
```
$ cd /root/D-ITG-2.8.1-r1023/bin
$ mn 
mininet> h2 ./ITGRecv &
```

`h2  kill -9 $(ps | grep ITGRecv | awk {'print $1'})`

## nadawca
### jeden przepływ
```
$ cd /root/D-ITG-2.8.1-r1023/bin
$ mn
mininet> h1 ./ITGSend -T UDP -a 10.0.0.2 -rp 20001 -u 500 1000 -C 10 -t 10000 -l sender.log -x receiver.log 
```
parametry:
* -T -- typ UDP
* -a -- docelowy adres
* -rp -- docelowy port
* -u min max -- rozmiar pakietu z rozkłdu jednostajnego (min, max)
* -C -- stałe tempo wysyłnia pakietów w pps
* -t -- czas trwania przepływu w ms
* -l -- zapis przebiegu przepływu/przepływów do pliku (`h1 ./ITGDec sender.log`)
* -x -- analogicznie do `-l`, tylko po stronie odbiorcy (`h2 ./ITGDec receiver.log`)

podsumowanie:
```
mininet> h1 ./ITGDec sender.log
ITGDec version 2.8.1 (r1023)
Compile-time options: bursty multiport
\----------------------------------------------------------
Flow number: 1
From 10.0.0.1:47338
To    10.0.0.2:20001
----------------------------------------------------------
Total time               =      9.934911 s
Total packets            =            98
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =         72514
Average bitrate          =     58.391263 Kbit/s
Average packet rate      =      9.864205 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =      0.000000 pkt
----------------------------------------------------------

__________________________________________________________
****************  TOTAL RESULTS   ******************
__________________________________________________________
Number of flows          =             1
Total time               =      9.934911 s
Total packets            =            98
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =         72514
Average bitrate          =     58.391263 Kbit/s
Average packet rate      =      9.864205 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =             0 pkt
Error lines              =             0
----------------------------------------------------------
```

## Wiele przepływów
Plik ze scenariuszem przepływów. Każda linia to osobny przepływ.
Parametr d definiuje czas (w ms) po upłynięciu którego zaczyna się dany przepływ.
```
mininet> h1 cat ../script
-T UDP -a 10.0.0.2 -rp 20001 -u 500 1000 -C 10 -t 5000 -d 0
-T UDP -a 10.0.0.2 -rp 20002 -u 500 1000 -C 10 -t 1000 -d 1500
-T UDP -a 10.0.0.2 -rp 20003 -u 500 1000 -C 10 -t 2000 -d 2000
-T UDP -a 10.0.0.2 -rp 20004 -u 500 1000 -C 10 -t 1300 -d 3700
```
Wywołanie nadawcy z plikiem scenariusza.
```
h1 ./ITGSend ../script -l sender.log -x receiver.log
```
Raport z widocznymi czterema przepływami.
```
mininet> h1 ./ITGDec sender.log
ITGDec version 2.8.1 (r1023)
Compile-time options: bursty multiport
/----------------------------------------------------------
Flow number: 2
From 10.0.0.1:37208
To    10.0.0.2:20002
----------------------------------------------------------
Total time               =      0.910707 s
Total packets            =            10
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =          7519
Average bitrate          =     66.049783 Kbit/s
Average packet rate      =     10.980480 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =      0.000000 pkt
----------------------------------------------------------
----------------------------------------------------------
Flow number: 3
From 10.0.0.1:38155
To    10.0.0.2:20003
----------------------------------------------------------
Total time               =      1.949430 s
Total packets            =            20
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =         15470
Average bitrate          =     63.485224 Kbit/s
Average packet rate      =     10.259409 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =      0.000000 pkt
----------------------------------------------------------
----------------------------------------------------------
Flow number: 4
From 10.0.0.1:49383
To    10.0.0.2:20004
----------------------------------------------------------
Total time               =      1.216157 s
Total packets            =            13
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =          9371
Average bitrate          =     61.643357 Kbit/s
Average packet rate      =     10.689409 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =      0.000000 pkt
----------------------------------------------------------
----------------------------------------------------------
Flow number: 1
From 10.0.0.1:44280
To    10.0.0.2:20001
----------------------------------------------------------
Total time               =      4.903757 s
Total packets            =            49
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =         36490
Average bitrate          =     59.529867 Kbit/s
Average packet rate      =      9.992339 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =      0.000000 pkt
----------------------------------------------------------

__________________________________________________________
****************  TOTAL RESULTS   ******************
__________________________________________________________
Number of flows          =             4
Total time               =      4.903757 s
Total packets            =            92
Minimum delay            =      0.000000 s
Maximum delay            =      0.000000 s
Average delay            =      0.000000 s
Average jitter           =      0.000000 s
Delay standard deviation =      0.000000 s
Bytes received           =         68850
Average bitrate          =    112.322042 Kbit/s
Average packet rate      =     18.761125 pkt/s
Packets dropped          =             0 (0.00 %)
Average loss-burst size  =             0 pkt
Error lines              =             0
----------------------------------------------------------
```
