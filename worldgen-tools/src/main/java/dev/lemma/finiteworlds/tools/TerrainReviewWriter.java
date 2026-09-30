package dev.lemma.finiteworlds.tools;

import dev.lemma.finiteworlds.core.WorldBlueprint;
import dev.lemma.finiteworlds.core.terrain.TerrainColumn;
import dev.lemma.finiteworlds.core.terrain.TerrainSampler;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Locale;

/** Review the actual block-column sampler, rather than only the macro planning fields. */
public final class TerrainReviewWriter {
    private static final double[] LEVELS = {64, 100, 250, 450, 700, 1000, 1250, 1435};
    private static final Color[] COLORS = {new Color(190,180,135), new Color(70,133,78),
        new Color(132,155,87), new Color(170,159,106), new Color(137,126,111),
        new Color(171,174,176), new Color(221,226,229), Color.WHITE};
    private TerrainReviewWriter() {}

    public static void write(WorldBlueprint world, long seed, Path directory) throws Exception {
        Files.createDirectories(directory);
        TerrainSampler sampler = new TerrainSampler(world, seed);
        double span = world.config().worldSizeBlocks();
        render(world, sampler, directory.resolve("review-continent.png"), -span/2, -span/2, span, 768);
        int peakX=0, peakZ=0;
        for(int z=0;z<world.resolution();z++) for(int x=0;x<world.resolution();x++)
            if(world.elevation(x,z)>world.elevation(peakX,peakZ)){peakX=x;peakZ=z;}
        double cx=peakX*span/(world.resolution()-1)-span/2;
        double cz=peakZ*span/(world.resolution()-1)-span/2;
        render(world,sampler,directory.resolve("review-mountains.png"),cx-4096,cz-4096,8192,1024);
        int ridgeX=peakX,ridgeZ=peakZ;double ridgeHeight=-1;
        for(int z=0;z<world.resolution();z++)for(int x=0;x<world.resolution();x++) {
            double separation=Math.hypot(x-peakX,z-peakZ)*span/(world.resolution()-1);
            if(separation>10000 && world.elevation(x,z)>ridgeHeight) {
                ridgeHeight=world.elevation(x,z);ridgeX=x;ridgeZ=z;
            }
        }
        double rx=ridgeX*span/(world.resolution()-1)-span/2;
        double rz=ridgeZ*span/(world.resolution()-1)-span/2;
        render(world,sampler,directory.resolve("review-cascades.png"),rx-4096,rz-4096,8192,1024);
        perspective(sampler,directory.resolve("review-cascades-perspective.png"),rx-3072,rz-3072,6144);
        perspective(sampler,directory.resolve("review-volcano-perspective.png"),cx-3072,cz-3072,6144);
        profile(sampler,directory.resolve("review-cascade-profile.png"),rx-8192,rx+8192,rz);
        Files.writeString(directory.resolve("review-legend.md"), String.format(Locale.ROOT,
            "# Actual terrain review — seed %d%n%nThe overview covers %.0f blocks; each pixel samples %.2f blocks. " +
            "The mountain crop spans 8192 blocks at 8 blocks per pixel, centered at (%.0f, %.0f). " +
            "These are sampled solid surfaces after local detail and physical river/lake shaping.%n%n" +
            "Hue encodes absolute Minecraft Y: tan shore 64, green 100, olive 250, ochre 450, brown rock 700, " +
            "gray 1000, pale gray 1250, white 1435. Brightness is northwest illumination of physical slopes, " +
            "not height. Blue is sampled ocean water; cyan is sampled river water; turquoise is sampled lake water. " +
            "No river strokes are enlarged: narrow streams may be subpixel on the overview. " +
            "The cross-range profile uses the same block-column sampler. The ordinary Cascades crop is centered (%.0f, %.0f). Perspectives cover 6144 blocks on a 360-cell mesh with equal horizontal and vertical physical scales (no vertical exaggeration).%n",seed,span,span/768,cx,cz,rx,rz));
        profile(sampler,directory.resolve("review-cross-range.png"),cx-8192,cx+8192,cz);
    }

    private static void render(WorldBlueprint world,TerrainSampler sampler,Path path,double left,double top,
                               double span,int size)throws Exception{
        double spacing=span/size;
        TerrainColumn[] columns=new TerrainColumn[size*size];
        for(int z=0;z<size;z++)for(int x=0;x<size;x++)
            columns[z*size+x]=sampler.sampleColumn(left+(x+.5)*spacing,top+(z+.5)*spacing);
        BufferedImage im=new BufferedImage(size,size+90,BufferedImage.TYPE_INT_RGB);
        for(int z=0;z<size;z++)for(int x=0;x<size;x++){
            TerrainColumn c=columns[z*size+x];
            double dx=(visibleHeight(columns[z*size+Math.min(size-1,x+1)])-
                visibleHeight(columns[z*size+Math.max(0,x-1)]))/(2*spacing);
            double dz=(visibleHeight(columns[Math.min(size-1,z+1)*size+x])-
                visibleHeight(columns[Math.max(0,z-1)*size+x]))/(2*spacing);
            double light=(.72+.46*(dx+dz))/Math.sqrt(1+dx*dx+dz*dz);
            double shade=Math.max(.30,Math.min(1.20,.45+light*.65));
            Color color=c.hasWater()?(c.lake()?new Color(35,161,165):c.river()?new Color(55,193,227):
                new Color(24,68,115)):altitude(c.terrainElevation());
            im.setRGB(x,z,new Color((int)Math.min(255,color.getRed()*shade),
                (int)Math.min(255,color.getGreen()*shade),(int)Math.min(255,color.getBlue()*shade)).getRGB());
        }
        Graphics2D g=im.createGraphics();g.setColor(new Color(25,29,33));g.fillRect(0,size,size,90);
        g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(Color.WHITE);
        g.drawString(String.format(Locale.ROOT,"Physical terrain | %.0f blocks | %.1f blocks/pixel | NW light",span,spacing),12,size+20);
        for(int i=0;i<LEVELS.length;i++){
            int x=12+i*(size-24)/LEVELS.length;
            g.setColor(COLORS[i]);g.fillRect(x,size+32,(size-32)/LEVELS.length,13);
            g.setColor(Color.WHITE);g.drawString("Y "+(int)LEVELS[i],x,size+64);
        }
        g.dispose();ImageIO.write(im,"png",path.toFile());
    }
    private static void perspective(TerrainSampler sampler,Path path,double left,double top,double span)throws Exception {
        int count=360,width=1500,height=1080;double spacing=span/count,scale=.115;
        TerrainColumn[] columns=new TerrainColumn[(count+1)*(count+1)];
        for(int z=0;z<=count;z++)for(int x=0;x<=count;x++)
            columns[z*(count+1)+x]=sampler.sampleColumn(left+x*spacing,top+z*spacing);
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(224,235,241));g.fillRect(0,0,width,height);
        for(int diagonal=0;diagonal<2*count;diagonal++)for(int x=Math.max(0,diagonal-count+1);x<=Math.min(count-1,diagonal);x++) {
            int z=diagonal-x;int[] vx={x,x+1,x+1,x},vz={z,z,z+1,z+1};
            int[] px=new int[4],py=new int[4];
            for(int i=0;i<4;i++) {
                TerrainColumn c=columns[vz[i]*(count+1)+vx[i]];
                px[i]=(int)(width/2.0+(vx[i]-vz[i])*spacing*scale);
                py[i]=(int)(230+(vx[i]+vz[i])*spacing*scale*.5-visibleHeight(c)*scale);
            }
            TerrainColumn c=columns[z*(count+1)+x];
            double dx=(visibleHeight(columns[z*(count+1)+x+1])-visibleHeight(c))/spacing;
            double dz=(visibleHeight(columns[(z+1)*(count+1)+x])-visibleHeight(c))/spacing;
            double shade=Math.max(.35,Math.min(1.1,.70+.30*(dx+dz)/Math.sqrt(1+dx*dx+dz*dz)));
            Color color=c.hasWater()?(c.lake()?new Color(35,161,165):new Color(55,193,227)):altitude(c.terrainElevation());
            g.setColor(new Color((int)(color.getRed()*shade),(int)(color.getGreen()*shade),(int)(color.getBlue()*shade)));
            g.fillPolygon(px,py,4);
        }
        g.setColor(Color.DARK_GRAY);g.setFont(new Font("SansSerif",Font.PLAIN,18));
        g.drawString(String.format(Locale.ROOT,"Actual column terrain | %.0f × %.0f blocks | isometric view | no vertical exaggeration",span,span),25,height-50);
        g.drawString(String.format(Locale.ROOT,"X %.0f to %.0f, Z %.0f to %.0f | same elevation hues and physical water as plan maps",left,left+span,top,top+span),25,height-22);
        g.dispose();ImageIO.write(image,"png",path.toFile());
    }
    private static double visibleHeight(TerrainColumn column){
        return column.hasWater()?column.waterSurfaceElevation():column.terrainElevation();
    }
    private static Color altitude(double height){
        if(height<=LEVELS[0])return COLORS[0];
        for(int i=1;i<LEVELS.length;i++)if(height<LEVELS[i]){
            double t=(height-LEVELS[i-1])/(LEVELS[i]-LEVELS[i-1]);Color a=COLORS[i-1],b=COLORS[i];
            return new Color((int)(a.getRed()+(b.getRed()-a.getRed())*t),
                (int)(a.getGreen()+(b.getGreen()-a.getGreen())*t),(int)(a.getBlue()+(b.getBlue()-a.getBlue())*t));
        }
        return COLORS[COLORS.length-1];
    }
    private static void profile(TerrainSampler sampler,Path path,double left,double right,double z)throws Exception{
        int width=1200,height=560;BufferedImage im=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=im.createGraphics();g.setColor(new Color(228,238,246));g.fillRect(0,0,width,height);
        for(int y=0;y<=1500;y+=250){int py=510-(int)(y/1500.0*470);g.setColor(new Color(190,200,209));
            g.drawLine(55,py,width-20,py);g.setColor(Color.DARK_GRAY);g.drawString(""+y,10,py+4);}
        for(int x=55;x<width-20;x++){
            double bx=left+(right-left)*(x-55)/(width-75.0);TerrainColumn c=sampler.sampleColumn(bx,z);
            int py=510-(int)(c.terrainElevation()/1500*470);g.setColor(altitude(c.terrainElevation()));
            g.drawLine(x,py,x,510);if(c.hasWater()){g.setColor(new Color(40,140,200));
                g.drawLine(x,510-(int)(c.waterSurfaceElevation()/1500*470),x,py);}
        }
        g.setColor(Color.DARK_GRAY);g.drawString(String.format(Locale.ROOT,
            "Cross-range surface at Z %.0f | X %.0f to %.0f blocks | Y in blocks",z,left,right),55,540);
        g.dispose();ImageIO.write(im,"png",path.toFile());
    }
}
