import org.arl.unet.addr.AddressResolution
import org.arl.unet.net.Router
import org.arl.unet.net.RouteDiscoveryProtocol
import org.arl.fjage.*
import org.arl.unet.*
import org.arl.unet.phy.*
import org.arl.unet.sim.*
import org.arl.unet.sim.channels.*

int countNodes = 8
int depthBase = 0
int maxDepthData = 2000
int radius = 2000
def nodes = 1..countNodes                   

def loc = new LocationGen()
def nodeLocation = loc.generate(countNodes, radius, depthBase, maxDepthData);

platform = RealTimePlatform
channel = [
  model:                BasicAcousticChannel,
  carrierFrequency:     25.kHz,
  bandwidth:            4096.Hz,
  spreading:            2,
  temperature:          25.C,
  salinity:             35.ppt,
  noiseLevel:           73.dB,
  waterDepth:           3020.m
]

setup1 = { c -> // Define setup1 closure for node configuration
  c.add 'echo', new SinkNode()
  c.add 'arp', new AddressResolution()
  c.add 'routing', new Router()
  c.add 'rdp', new RouteDiscoveryProtocol()
}

setup2 = { c -> // Define setup2 closure for node configuration
  c.add 'echo', new DataNode(countNodes)
  c.add 'arp', new AddressResolution()
  c.add 'routing', new Router()
  c.add 'rdp', new RouteDiscoveryProtocol()
}

simulate {
  nodes.each{n ->
    if(n==1)
      node "N1", address:n, location: nodeLocation[n], web:8080+n, stack:setup1
    else
      node "N"+n, address:n, location: nodeLocation[n], web:8080+n, stack:setup2
  }
}

println sprintf('%6d\t\t%6d\t\t%7.3f\t\t%7.3f',
    [trace.txCount, trace.rxCount, trace.offeredLoad, trace.throughput])