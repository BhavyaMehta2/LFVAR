import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter
import org.arl.fjage.WakerBehavior
import org.arl.unet.phy.*
import org.arl.unet.net.*
import org.arl.unet.bb.*

class DataNode extends UnetAgent {

    final String title = 'Data Node'        
    final String description = 'Serves as data collection nodes in the network' 

    enum DataParams implements Parameter { 
        maxNodes,
        addrN,
        hcN,
        depthN,
        vN,
        currentCost
    }

    class Protocols  {
        final static int BEACON = 32
    }

    int txRange = 1000

    AgentID phy
    AgentID router
    AgentLocalRandom rnd

    int addressSink
    int maxNodes

    int addrN
    int hcN
    int depthN
    int vN
    double currentCost
    double currentDepthDiff
    
    int[][] pcnTableN
    int[] pcnEntry
    int pcnTablePointer

    int[][] nonPCNTableN
    int[] nonPCNEntry
    int nonPCNTablePointer

    PDU beacon = PDU.withFormat {
        uint16('addressSink')
        uint16('hopCountSink')
        int16('depthSink')
        uint16('voidStatusSink')
    }
    
    DataNode(int countNodes)
    {
        maxNodes = countNodes
    }

    void setPowerLevel(int ndx, float lvl) {
        if (ndx != Physical.CONTROL && ndx != Physical.DATA) return
        set(phy, BasebandParam.signalPowerLevel, lvl)
    }

    @Override
    void startup() {
        subscribeForService Services.DATAGRAM 
        phy = agentForService Services.PHYSICAL
        router = agentForService Services.ROUTING
        rnd = AgentLocalRandom.current()

        setPowerLevel(Physical.DATA, 0)
    
        AgentID node = agentForService Services.NODE_INFO

        subscribe topic(phy) 
        subscribe topic(router)

        addressSink = 1

        addrN = node.address
        hcN = 99
        depthN = node.location[2]
        vN = 1
        currentCost = Integer.MIN_VALUE;
        currentDepthDiff = Integer.MAX_VALUE;

        pcnTableN = new int[maxNodes+1][4]
        pcnEntry = new int[maxNodes+1]
        pcnTablePointer = 0
        
        nonPCNTableN = new int[maxNodes+1][4]
        nonPCNEntry = new int[maxNodes+1]
        nonPCNTablePointer = 0
    }

    @Override
    void processMessage(Message msg) {
        if (msg instanceof DatagramNtf && msg.protocol == Protocols.BEACON) {
            
            def beaconRecvd = beacon.decode(msg.data)
            int[] parsedBeacon  = new int[4]
            parsedBeacon[0] = beaconRecvd.addressSink 
            parsedBeacon[1] = beaconRecvd.hopCountSink 
            parsedBeacon[2] = beaconRecvd.depthSink
            parsedBeacon[3] = beaconRecvd.voidStatusSink

            log.info Arrays.toString(parsedBeacon)
            
            if(msg.from == addressSink)
            {
                log.info "Beacon received from Sink; Addr:" +msg.from
                int nextNode = addressSink
                vN = 0
                hcN = 1
                addRoute(addressSink, nextNode)
            }
            else if(parsedBeacon[2]>depthN && parsedBeacon[3] == 0)
            {
                log.info "Beacon received from Non-void Node from a lower depth; Addr:" +msg.from
                
                if(pcnEntry[parsedBeacon[0]] == 1)
                {
                    for(int i = 0; i<=maxNodes-1; i++)
                    {
                        if(pcnTableN[i][0] == parsedBeacon[0])
                        {
                            for(int j = 0; j<=3; j++)
                                pcnTableN[i][j] = parsedBeacon[j]
                            
                            break
                        }
                    }
                }
                else
                {
                    for(int j = 0; j<=3; j++)
                        pcnTableN[pcnTablePointer][j] = parsedBeacon[j]
                    
                    pcnTablePointer++
                    pcnEntry[parsedBeacon[0]] = 1;
                }

                vN = 0;

                def costNM = -(depthN - parsedBeacon[2])/(txRange*parsedBeacon[1])

                log.info "Cost: "+parsedBeacon[0]+"->"+addrN+": "+costNM

                if(costNM>currentCost)
                {
                    def nextNode = parsedBeacon[0]
                    hcN = parsedBeacon[1] + 1
                    currentCost = costNM
                    addRoute(addressSink, nextNode)
                }
            }
            else if(parsedBeacon[2]<depthN || parsedBeacon[3] == 1)
            {
                log.info "Beacon received from Void Node or higher depth; Addr:" +msg.from
                
                if(nonPCNEntry[parsedBeacon[0]] == 1)
                {
                    for(int i = 0; i<=maxNodes-1; i++)
                    {
                        if(nonPCNTableN[i][0] == parsedBeacon[0])
                        {
                            for(int j = 0; j<=3; j++)
                                nonPCNTableN[i][j] = parsedBeacon[j]
                            
                            break
                        }
                    }
                }
                else
                {
                    for(int j = 0; j<=3; j++)
                        nonPCNTableN[nonPCNTablePointer][j] = parsedBeacon[j]
                    
                    nonPCNTablePointer++
                    nonPCNEntry[parsedBeacon[0]] = 1;
                }

                def depthDiff = Math.abs(parsedBeacon[2] - depthN)

                log.info "Depth: "+parsedBeacon[0]+"->"+addrN+": "+depthDiff

                if(depthDiff<currentDepthDiff)
                {
                    currentDepthDiff = depthDiff

                    if(vN==1)
                    {
                        def nextNode = parsedBeacon[0]
                        hcN = parsedBeacon[1] + 1
                        addRoute(addressSink, nextNode)
                    }
                }
            }

            log.info "PCN: "+Arrays.deepToString(pcnTableN)
            log.info "Non-PCN: "+Arrays.deepToString(nonPCNTableN)
        }
    }

    void addRoute(int addrSink, int nextNode)
    {   
        add new OneShotBehavior({ 
            router.delroutes
            router.send new RouteDiscoveryNtf(to: addrSink, nextHop: nextNode, hops: hcN)
        })

        long backoff = rnd.nextDouble(0, 1000*maxNodes)

        add new WakerBehavior(backoff, {
            def bytes = beacon.encode(addressSink: addrN, hopCountSink: hcN,
                            depthSink: depthN, voidStatusSink: vN)
            add new OneShotBehavior({ 
                phy << new DatagramReq(protocol:Protocols.BEACON, data: bytes)
            })
        })
    }

    List<Parameter> getParameterList() {      
        allOf(DataParams)
    }
}
