# Famtar SDN
## Threads
1. OSPF process and Flow Table management
2. Check interface stats and trigger OSPF updates

## Thread 1
```
while famtar_on:
	//on packet_in -- identify flow
	prefix = get_prefix(packet_in)
	path = ospf.get_shortest_path(prefix)
	//add appropriate flow table entries on switches on the path starting from the end to prevent unnecessary packet_ins
	for node in reverse(path):
		node.flow_table.add(flow, timeout)
```

## Thread 2
```
threshold_high = 0.9 * port_speed
threshold_low = 0.7 * port_speed

previous = 0
while famtar_on:
	port_stats = port_stats_request(interval)
	for port_stat in port_stats:
		rate = (port_stat.getTxBytes() - previous) * 8 / interval
		if rate >= threshold_high:
			port.set_max_cost()
			recalculate_ospf()
		if rate <= threshold_low:
			port.set_default_cost()
			recalculate_ospf()
		previous = port_stat.getTxBytes()
```
