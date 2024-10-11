class LocationGen{
    public HashMap generate(int nodes, int radius, int depthBase, int maxDepthData)
    {
        // Random random = new Random()
        def nodeLocation = [:]

        // nodeLocation[1] = [ 0.m, 0.m, -depthBase.m]

        // for(int i = 2; i<=nodes; i++)
        // {
        //     nodeLocation[i] = [ random.nextInt(2*radius)-radius, random.nextInt(2*radius)-radius, -random.nextInt(maxDepthData).m]
        // }

        // print(nodeLocation)

        nodeLocation[1] = [2500.m, 2500.m, 0.m]
        nodeLocation[2] = [2800.m, 2500.m, -900.m]
        nodeLocation[3] = [2800.m, 3420.m, -850.m]
        nodeLocation[4] = [3100.m, 2500.m, -1000.m]
        nodeLocation[5] = [3600.m, 2500.m, -1600.m]
        nodeLocation[6] = [3500.m, 3000.m, -2100.m]
        nodeLocation[7] = [3100.m, 3300.m, -2060.m]
        nodeLocation[8] = [2700.m, 2700.m, -2150.m]

        return nodeLocation
    }
}