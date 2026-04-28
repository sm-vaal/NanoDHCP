# NanoDHCP
Ever needed a tiny DHCP server in a pinch? Maybe for a virtual network? A homelab? Then this is the right place for you!

## The what
NanoDHCP is a minimal, cross-platform, and easy to configure DHCPv4 server that runs in your command line, like classic UNIX tools! It's developed with Linux in mind, but should run wherever there's a JRE. Don't use this outside small, non-critical isolated networks though. I'm serious.

## Features
- Extremely easy to use
- CLI configuration only
- Cross-platform (JVM-based)
- Verbose debugging mode (cool for learning too!)

## The why
Sometimes you need a quick, simple way to configure IPs and gateways, nothing else. ISC-DHCP can be overkill for those cases, so why not use a simpler program instead? 

On a more personal note, this was mostly born thanks to the _wonderful_ limitations of Hyper-V's virtual switches: they don't have an option to setup a DHCP server like say, Virtualbox or VMware. 

Also, Java was chosen for simplicity and portability: performance is not a limiting factor at this scale.

## The how

### The raw, thinkpad-user way (build yourself and run the .class)
Clone this repo, and run the `./build-dhcp` shell script. This will compile the project, so you need a JDK. 

Then, to run, `cd /build` and `[sudo] java NanoDHCP [options]`.

If you don't want to build it, there is a .jar in the releases page. Download it and run `[sudo] java -jar NanoDHCP.jar [options]`


### The "WHY IS THERE CODE??? MAKE A \*\*\*\*\*\* .EXE FILE AND GIVE IT TO ME" way
If none of what you read before makes sense to you, go to the releases page and download the pre-built executable for your OS. You don't even need a JRE: **boom, zero setup**. Can't fight that huh?

#### A note on running this on Windows
Windows fights you along the way if you want to run this, it _really_ likes to hog port 67. Be warned!

## The server options

|Option                 |Description                                                               |
|-----------------------|--------------------------------------------------------------------------|
|--help                 |Show help (this table)                                                    |
|--force-port <port>    |Bind socket to port (default: 67). Useful to sniff                        |
|--range <ip1> <ip2>    |IP pool range (default: 192.168.1.3 - 192.168.1.254)                      |
|--mask <m>             |Network mask in CIDR (but no slash!!!) (default: /24)                     |
|--gateway <ip>         |Gateway IP to assign (default: 192.168.1.1)                               |
|--dns <ip>             |DNS server IP to assign (default: 1.1.1.1)                                |
|--sniff-only           |Do not respond, only listen to UDP traffic                                |
|--show-ip              |Print all valid IPs for current configuration                             |
|--lease-time <sec>     |Time to lease IPs for, in seconds (default: 24h)                          |
|--force-server-ip <ip> |Forces packets to use a specific server IP (default: local)               |
|--interface <if>       |Uses the server IP of the interface with name <if> (default: first found) |
|--pxe <ip> <filename>  |Tells PXE boot requests to use "filename" at the TFTP server with that ip |
|--verbose              |Enable verbose output                                                     |


## The usage examples
Say, you want to use this in your home network, using Google's DNS, with a lease time of 200 seconds. Then:
`sudo java NanoDHCP --range 192.168.1.10 192.168.1.254 --gateway 192.168.1.1 --mask 24 --dns 8.8.8.8 --lease-time 200`

Or, maybe you just want to listen to DHCP requests to sniff around? Do this!
`sudo java NanoDHCP --sniff-only --verbose`

Maybe this program is like regex, and you forget how to use it every time you need it. then:
`java NanoDHCP --help`

## The limitations (of course)
This server is **NOT meant to replace anything in the real world.** I tried it in my home network and it does work fine, but your mileage may vary, as only the **bare basics** are implemented. 

It's made to be very extensible, so if you want it to do anything more, take a look at the code! Don't be shy!


## The implementation details (nerdy stuff)
As far as specifics go:
- It uses a round-robin address allocation approach, that is, it tries to allocate the next available IP in a circular approach

- MACs are sticky! it will try to assign devices the one they previously had

- There is **zero persistance**, once the server stops running, everything is forgotten. What happens in the JVM, stays in the JVM. No way to load settings from a file either, if you want that, you'll need to make a script to run with your desired arguments.
