import ru.aiforge.riftworld.world.DeepOcean;
public class DeepOceanCoreTest {
  public static void main(String[] a) {
    for(int cx=-4;cx<=4;cx++) for(int cz=-4;cz<=4;cz++) {
      var r=DeepOcean.region(cx,cz);
      if(!DeepOcean.openCoreChunk(r.x(),r.z())) throw new AssertionError("centre");
      if(DeepOcean.openCoreChunk(r.x()+500,r.z())) throw new AssertionError("outside");
      for(int x=r.x()-410;x<=r.x()+410;x+=11) for(int z=r.z()-410;z<=r.z()+410;z+=13) {
        boolean expected=DeepOcean.openCoreChunk(x,z);
        int x0=Math.floorDiv(x,16)*16, z0=Math.floorDiv(z,16)*16;
        if(expected!=DeepOcean.openCoreChunk(x0,z0)||expected!=DeepOcean.openCoreChunk(x0+15,z0+15)) throw new AssertionError("chunk seam");
      }
    }
    System.out.println("OK: open cores cover all centres, exclude distant chunks and agree at signed chunk corners");
  }
}
