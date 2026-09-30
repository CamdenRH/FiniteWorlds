package dev.lemma.finiteworlds.core.hydrology;

import dev.lemma.finiteworlds.core.WorldBlueprint;

/** A stream-power proxy: wet, large, steep catchments cut deeper mountain valleys. */
public final class CatchmentIncision {
    private CatchmentIncision() {}
    public static double depthAt(WorldBlueprint world, RiverSegment segment, int index, double surface) {
        HydrologyGrid h = world.hydrology();
        int size=h.resolution(), count=segment.cellPath().size();
        int cell=segment.cellPath().get(index), x=cell%size,z=cell/size;
        if(h.lakeId(x,z)>=0)return 0;
        int before=segment.cellPath().get(Math.max(0,index-2));
        int after=segment.cellPath().get(Math.min(count-1,index+2));
        double distance=Math.max(h.blocksPerCell(),Math.hypot(before%size-after%size,before/size-after/size)*h.blocksPerCell());
        double slope=Math.abs(h.conditionedElevation(before%size,before/size)-h.conditionedElevation(after%size,after/size))/distance;
        double runoff=world.climate().effectiveDischarge(x,z);
        double power=Math.min(1.5,Math.pow(Math.max(0,runoff)/5000.0,.35));
        double mountain=smooth(180,650,surface);
        double gradient=smooth(.003,.09,slope);
        double taper=1;
        RiverNode start=h.riverNodes().get(segment.startNodeId()),end=h.riverNodes().get(segment.endNodeId());
        if(start.lakeInlet()||start.lakeOutlet())taper*=smooth(0,384,index*h.blocksPerCell());
        if(end.lakeInlet()||end.lakeOutlet())taper*=smooth(0,384,(count-1-index)*h.blocksPerCell());
        return mountain*gradient*(18+105*power)*taper;
    }
    private static double smooth(double low,double high,double v){
        double t=Math.max(0,Math.min(1,(v-low)/(high-low)));return t*t*(3-2*t);
    }
}
