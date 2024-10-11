import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter;
import org.arl.unet.DatagramReq
import org.arl.fjage.WakerBehavior
import org.arl.fjage.OneShotBehavior

class SinkNode extends UnetAgent {
  final String title = 'Sink Node'        
  final String description = 'Serves as the base for a network of nodes' 

  enum SinkParams implements Parameter { 
    maxNodes,
    addrSink,
    hcSink,
    depthSink,
    vSink
  }

  class Protocols  {
    final static int BEACON = 32
  }

  AgentID phy

  int maxNodes
  int addrSink
  int hcSink
  int depthSink
  int vSink

  PDU beacon = PDU.withFormat {
    uint16('addressSink')
    uint16('hopCountSink')
    uint16('depthSink')
    uint16('voidStatusSink')
  }

  @Override
  void startup() {
    subscribeForService(Services.DATAGRAM)
    phy = agentForService Services.PHYSICAL 
    AgentID node = agentForService Services.NODE_INFO
    
    addrSink = node.address
    hcSink = 0
    depthSink = node.location[2]
    vSink = 0

    if (phy == null) { 
      phy = agentForService Services.PHYSICAL 
    }

    subscribe topic(phy) 
    
    beaconBroadcast()
  }

  void beaconBroadcast() {
    log.info "Broadcasting beacon"
    def bytes = beacon.encode(addressSink: addrSink, hopCountSink: hcSink,
                              depthSink: depthSink, voidStatusSink: vSink)
    add new OneShotBehavior({ 
      phy << new ClearReq()
      phy << new DatagramReq(protocol: Protocols.BEACON, data: bytes)
    })
  }

  @Override
  void processMessage(Message msg) {
  
  }
  
  List<Parameter> getParameterList() {      
    allOf(SinkParams)
  }
}
