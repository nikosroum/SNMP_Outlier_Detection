/**************/
EP2300 - Project
Roumpoutsos Nikolaos 
Sapountzis Ioannis 
/**************/
Description:
an outlier detection schem based on SNMP using Java(WebNMS API)

--List of files:
	Detect.java
	Discover.java
	Router.java
	RouterLoadInfo.java
	RouterPoller.java
	SnmpGET.java
	SnmpGETNEXT.java
	SnmpLoadPoll.java
	SnmpTopDisc.java
	SnmpWalk.java

--Instructions
	#Compile

		javac ep2300/*.java

	#Run

		-For Network Topology Discovery
		java ep2300/Discover <ip_address>

		-For Outliers Detection
		java ep2300/Detect <observation_window> <time_interval>  <Max_live_monitor_samples> [-SD]
		run with -SD to use only SD method