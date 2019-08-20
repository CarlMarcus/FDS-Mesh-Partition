package Rough;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoughPartition {

    public static void main(String[] args) throws Exception {
        String str = "C:\\Users\\gongz\\Desktop\\test3\\test3.fds";
        run(str,8);
    }

    private static void run(String path, int number) {
        Field field = initField(readFile(path), number);
        int[] result = partition(field);
        System.out.print("分区方案： ");
        for (int i=0;i<3;i++){
            System.out.print(result[i]+" ");
        }
        //
        System.out.println();
        field.meshCut=result;
        field = calcuFactor(field);
        double[] load = calcuLoad(field);
        double avgLoad = 0;
        for(int i=0; i<load.length; i++) {

            System.out.println("************************");
            System.out.println(partOfsum(field.meshGridNum)[i]);
            System.out.println(partOfsum(field.borderGridNum)[i]);
            System.out.println(field.meshObstVolume[i]);
            System.out.println(partOfsum(field.distFromFire)[i]);

            System.out.println("mesh load: "+load[i]);
            avgLoad += load[i];
        }
        avgLoad = avgLoad/number;
        System.out.println("avg  load: "+avgLoad);
        System.out.println("期望负载偏差:"+tolerance(load)/avgLoad/2+"  //0.5*(max-min)/avg");
    }

    private static int[] partition(Field field) {
        double min = Double.MAX_VALUE;
        int[] location=new int[]{0,0,0};
        double loadDiff;
        if (field.meshNum==2) {
            for (int i=1;i<field.gridNum[0];i++) {
                field.meshCut[0]=i;
                loadDiff = tolerance(calcuLoad(calcuFactor(field)));
                if (min > loadDiff) {
                    min = loadDiff;
                    location[0]=i;
                }
            }
        }
        if (field.meshNum==4) {
            for (int i = 1; i < field.gridNum[0]; i++) {
                for (int j = 1; j < field.gridNum[1]; j++) {
                    field.meshCut[0] = i;
                    field.meshCut[1] = j;
                    loadDiff = tolerance(calcuLoad(calcuFactor(field)));
                    if (min > loadDiff) {
                        min = loadDiff;
                        location[0] = i;
                        location[1] = j;
                    }
                }
            }
        }
        if (field.meshNum==8) {
            for (int i=1; i<field.gridNum[0]; i++) {
                for(int j=1; j<field.gridNum[1]; j++) {
                    for (int k=1; k<field.gridNum[2]; k++) {
                        field.meshCut[0]=i;
                        field.meshCut[1]=j;
                        field.meshCut[2]=k;
                        loadDiff = tolerance(calcuLoad(calcuFactor(field)));
                        if (min>loadDiff) {
                            min = loadDiff;
                            location[0]=i;
                            location[1]=j;
                            location[2]=k;
                        }
                    }
                }
            }
        }
        return location;
    }

    private static double[] calcuLoad(Field field) {
        int num = field.meshNum;
        double[] load = new double[num];
        double[] meshGridNum = partOfsum(field.meshGridNum);
        double[] borderGridNum = partOfsum(field.borderGridNum);
        double[] distFromFire = partOfsum(field.distFromFire);
        double[] meshObstVolume = field.meshObstVolume;
        for (int i=0; i<num; i++) {
            load[i]= (1+2*meshObstVolume[i])*meshGridNum[i]*Math.pow(borderGridNum[i],0.5)/Math.pow(distFromFire[i],0.333333);
        }
        return load;
    }

    private static Field calcuFactor(Field field) {
        double gridVol = (field.size[1]-field.size[0])*(field.size[3]-field.size[2])*(field.size[5]-field.size[4])
                /(double) (field.gridNum[0]*field.gridNum[1]*field.gridNum[2]);
        if (field.meshNum==2) {
            field.meshObstVolume= new double[] {0,0};
            field.meshGridNum = new int[]{field.meshCut[0] * field.gridNum[1] * field.gridNum[2],
                    (field.gridNum[0] - field.meshCut[0]) * field.gridNum[1] * field.gridNum[2]};
            field.borderGridNum = new int[]{field.gridNum[1]*field.gridNum[2], field.gridNum[1]*field.gridNum[2]};
            double anchor = (field.meshCut[0]*(field.size[1]-field.size[0]))/field.gridNum[0]+field.size[0];
            for (int i=0; i<field.obstruction.size(); i++) {
                double[] tmp = field.obstruction.get(i);
                if (tmp[0]>=anchor) {
                    field.meshObstVolume[1] += (tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                } else if (anchor>tmp[0] && anchor<=tmp[1]) {
                    field.meshObstVolume[0] += (anchor-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    field.meshObstVolume[1] += (tmp[1]-anchor)*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                } else {
                    field.meshObstVolume[0] += (tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                }
            }
            for (int i=0; i<field.meshObstVolume.length; i++)
                field.meshObstVolume[i] = field.meshObstVolume[i]/(field.meshGridNum[i]*gridVol);

            double distMesh0 = Math.sqrt(Math.pow((field.size[0]+anchor)/2-(field.fireLocation[1]+field.fireLocation[0])/2,2)
                    +Math.pow((field.size[3]+field.size[2])/2-(field.fireLocation[3]+field.fireLocation[2])/2, 2)
                    +Math.pow((field.size[5]+field.fireLocation[4])/2-(field.fireLocation[5]+field.fireLocation[4])/2, 2));
            double distMesh1 = Math.sqrt(Math.pow((field.size[1]+anchor)/2-(field.fireLocation[1]+field.fireLocation[0])/2,2)
                    +Math.pow((field.size[3]+field.size[2])/2-(field.fireLocation[3]+field.fireLocation[2])/2, 2)
                    +Math.pow((field.size[5]+field.fireLocation[4])/2-(field.fireLocation[5]+field.fireLocation[4])/2, 2));
            field.distFromFire = new double[]{distMesh0, distMesh1};
        }

        if (field.meshNum==4) {
            field.meshObstVolume= new double[] {0,0,0,0};
            field.meshGridNum = new int[]{field.meshCut[0]*field.meshCut[1]*field.gridNum[2],
                    (field.gridNum[0]-field.meshCut[0])*field.meshCut[1]*field.gridNum[2],
                    field.meshCut[0]*(field.gridNum[1]-field.meshCut[1])*field.gridNum[2],
                    (field.gridNum[0]-field.meshCut[0])*(field.gridNum[1]-field.meshCut[1])*field.gridNum[2]};
            field.borderGridNum = new int[]{
                    (field.meshCut[0]+field.meshCut[1])*field.gridNum[2],
                    (field.gridNum[0]-field.meshCut[0]+field.meshCut[1])*field.gridNum[2],
                    (field.meshCut[0]+field.gridNum[1]-field.meshCut[1])*field.gridNum[2],
                    (field.gridNum[0]+field.gridNum[1]-field.meshCut[0]-field.meshCut[1])*field.gridNum[2]
            };
            double anx = (field.meshCut[0]*(field.size[1]-field.size[0]))/field.gridNum[0]+field.size[0];
            double any = (field.meshCut[1]*(field.size[3]-field.size[2]))/field.gridNum[1]+field.size[2];
            for (int i=0; i<field.obstruction.size(); i++) {
                double[] tmp = field.obstruction.get(i);
                if (anx>tmp[1]) {
                    if (any>tmp[3]) {
                        field.meshObstVolume[0]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    } else if (any<=tmp[3] && any>tmp[2]){
                        field.meshObstVolume[0]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                    } else {
                        field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    }
                } else if (anx<=tmp[1] && anx>tmp[0]) {
                    if (any>tmp[3]) {
                        field.meshObstVolume[0]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[1]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    } else if (any<=tmp[3] && any>tmp[2]) {
                        field.meshObstVolume[0]+=(anx-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[1]+=(tmp[1]-anx)*(any-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[2]+=(anx-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        field.meshObstVolume[3]+=(tmp[1]-anx)*(tmp[3]-any)*(tmp[5]-tmp[4]);
                    } else {
                        field.meshObstVolume[2]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[3]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    }
                } else {
                    if (any>tmp[3]) {
                        field.meshObstVolume[1]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    } else if (any<=tmp[3] && any>tmp[2]) {
                        field.meshObstVolume[1]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[3]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                    } else {
                        field.meshObstVolume[3]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    }
                }
            }
            for (int i=0; i<field.meshObstVolume.length; i++)
                field.meshObstVolume[i] =  field.meshObstVolume[i]/(field.meshGridNum[i]*gridVol);

            double x=(field.fireLocation[0]+field.fireLocation[1])/2;
            double y=(field.fireLocation[2]+field.fireLocation[3])/2;
            double z=(field.fireLocation[4]+field.fireLocation[5])/2;
            double dist1=Math.sqrt(Math.pow((field.size[0]+anx)/2-x,2)+Math.pow((field.size[2]+any)/2-y,2)
                    +Math.pow((field.size[4]+field.size[5])/2-z,2));
            double dist2=Math.sqrt(Math.pow((anx+field.size[1])/2-x,2)+Math.pow((field.size[2]+any)/2-y,2)
                    +Math.pow((field.size[4]+field.size[5])/2-z,2));
            double dist3=Math.sqrt(Math.pow((field.size[0]+anx)/2-x,2)+Math.pow((any+field.size[3])/2-y,2)
                    +Math.pow((field.size[4]+field.size[5])/2-z,2));
            double dist4=Math.sqrt(Math.pow((anx+field.size[1])/2-x,2)+Math.pow((any+field.size[3])/2-y,2)
                    +Math.pow((field.size[4]+field.size[5])/2-z,2));
            field.distFromFire = new double[]{dist1,dist2,dist3,dist4};
        }

        if (field.meshNum==8) {
            field.meshObstVolume= new double[] {0,0,0,0,0,0,0,0};
            field.meshGridNum = new int[]{
                    field.meshCut[0]*field.meshCut[1]*field.meshCut[2],
                    (field.gridNum[0]-field.meshCut[0])*field.meshCut[1]*field.meshCut[2],
                    field.meshCut[0]*(field.gridNum[1]-field.meshCut[1])*field.meshCut[2],
                    (field.gridNum[0]-field.meshCut[0])*(field.gridNum[1]-field.meshCut[1])*field.meshCut[2],
                    field.meshCut[0]*field.meshCut[1]*(field.gridNum[2]-field.meshCut[2]),
                    (field.gridNum[0]-field.meshCut[0])*field.meshCut[1]*(field.gridNum[2]-field.meshCut[2]),
                    field.meshCut[0]*(field.gridNum[1]-field.meshCut[1])*(field.gridNum[2]-field.meshCut[2]),
                    (field.gridNum[0]-field.meshCut[0])*(field.gridNum[1]-field.meshCut[1])*(field.gridNum[2]-field.meshCut[2])
            };
            field.borderGridNum = new int[]{
                    field.meshCut[0]*field.meshCut[1]+field.meshCut[1]*field.meshCut[2]+field.meshCut[2]*field.meshCut[0],
                    (field.gridNum[0]-field.meshCut[0])*field.meshCut[1]+field.meshCut[1]*field.meshCut[2]+(field.gridNum[0]-field.meshCut[0])*field.meshCut[2],
                    field.meshCut[0]*(field.gridNum[1]-field.meshCut[1])+(field.gridNum[1]-field.meshCut[1])*field.meshCut[2]+field.meshCut[2]*field.meshCut[0],
                    (field.gridNum[0]-field.meshCut[0])*(field.gridNum[1]-field.meshCut[1])+(field.gridNum[1]-field.meshCut[1])*field.meshCut[2]+field.meshCut[2]*(field.gridNum[0]-field.meshCut[0]),
                    field.meshCut[0]*field.meshCut[1]+field.meshCut[1]*(field.gridNum[2]-field.meshCut[2])+(field.gridNum[2]-field.meshCut[2])*field.meshCut[0],
                    (field.gridNum[0]-field.meshCut[0])*field.meshCut[1]+field.meshCut[1]*(field.gridNum[2]-field.meshCut[2])+(field.gridNum[2]-field.meshCut[2])*(field.gridNum[0]-field.meshCut[0]),
                    field.meshCut[0]*(field.gridNum[1]-field.meshCut[1])+(field.gridNum[1]-field.meshCut[1])*(field.gridNum[2]-field.meshCut[2])+(field.gridNum[2]-field.meshCut[2])*field.meshCut[0],
                    (field.gridNum[0]-field.meshCut[0])*(field.gridNum[1]-field.meshCut[1])+(field.gridNum[1]-field.meshCut[1])*(field.gridNum[2]-field.meshCut[2])+(field.gridNum[2]-field.meshCut[2])*(field.gridNum[0]-field.meshCut[0])
            };
            double anx = (field.meshCut[0]*(field.size[1]-field.size[0]))/field.gridNum[0]+field.size[0];
            double any = (field.meshCut[1]*(field.size[3]-field.size[2]))/field.gridNum[1]+field.size[2];
            double anz = (field.meshCut[2]*(field.size[5]-field.size[4]))/field.gridNum[2]+field.size[4];
            for (int i=0; i<field.obstruction.size(); i++) {
                double[] tmp = field.obstruction.get(i);
                if (anx>tmp[1]) {
                    if (any>tmp[3]) {
                        if (anz>tmp[5]) {
                            field.meshObstVolume[0]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[0]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[4]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        } else {
                            field.meshObstVolume[4]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        }
                    } else if (any<=tmp[3] && any>tmp[2]){
                        if (anz>tmp[5]) {
                            field.meshObstVolume[0]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[0]+=(tmp[1]-tmp[0])*(any-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(anz-tmp[4]);
                            field.meshObstVolume[4]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-anz);
                            field.meshObstVolume[6]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-anz);
                        } else {
                            field.meshObstVolume[4]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[6]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        }
                    } else {
                        field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        if (anz>tmp[5]) {
                            field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[2]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[6]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-anz);
                        }
                    }
                } else if (anx<=tmp[1] && anx>tmp[0]) {
                    if (any>tmp[3]) {
                        if (anz>tmp[5]) {
                            field.meshObstVolume[0]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[1]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[0]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[1]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[4]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-anz);
                            field.meshObstVolume[5]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(tmp[5]-anz);
                        } else {
                            field.meshObstVolume[4]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[5]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        }
                    } else if (any<=tmp[3] && any>tmp[2]) {
                        if (anz>tmp[5]) {
                            field.meshObstVolume[0]+=(anx-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[1]+=(tmp[1]-anx)*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[2]+=(anx-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                            field.meshObstVolume[3]+=(tmp[1]-anx)*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[0]+=(anx-tmp[0])*(any-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[1]+=(tmp[1]-anx)*(any-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[2]+=(anx-tmp[0])*(tmp[3]-any)*(anz-tmp[4]);
                            field.meshObstVolume[3]+=(tmp[1]-anx)*(tmp[3]-any)*(anz-tmp[4]);
                            field.meshObstVolume[4]+=(anx-tmp[0])*(any-tmp[2])*(tmp[5]-anz);
                            field.meshObstVolume[5]+=(tmp[1]-anx)*(any-tmp[2])*(tmp[5]-anz);
                            field.meshObstVolume[6]+=(anx-tmp[0])*(tmp[3]-any)*(tmp[5]-anz);
                            field.meshObstVolume[7]+=(tmp[1]-anx)*(tmp[3]-any)*(tmp[5]-anz);
                        } else {
                            field.meshObstVolume[4]+=(anx-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[5]+=(tmp[1]-anx)*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[6]+=(anx-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                            field.meshObstVolume[7]+=(tmp[1]-anx)*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        }
                    } else {
                        field.meshObstVolume[2]+=(anx-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        field.meshObstVolume[3]+=(tmp[1]-anx)*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                    }
                } else {
                    if (any>tmp[3]) {
                        if (anz>tmp[5]) {
                            field.meshObstVolume[1]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[1]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[5]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-anz);
                        } else {
                            field.meshObstVolume[5]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        }
                    } else if (any<=tmp[3] && any>tmp[2]) {
                        if (anz<tmp[5]) {
                            field.meshObstVolume[1]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[3]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[1]+=(tmp[1]-tmp[0])*(any-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[3]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(anz-tmp[4]);
                            field.meshObstVolume[5]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-anz);
                            field.meshObstVolume[7]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-anz);
                        } else {
                            field.meshObstVolume[5]+=(tmp[1]-tmp[0])*(any-tmp[2])*(tmp[5]-tmp[4]);
                            field.meshObstVolume[7]+=(tmp[1]-tmp[0])*(tmp[3]-any)*(tmp[5]-tmp[4]);
                        }
                    } else {
                        if (anz>tmp[5]) {
                            field.meshObstVolume[3]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        } else if (anz<=tmp[5] && anz>tmp[4]) {
                            field.meshObstVolume[3]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(anz-tmp[4]);
                            field.meshObstVolume[7]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-anz);
                        } else {
                            field.meshObstVolume[7]+=(tmp[1]-tmp[0])*(tmp[3]-tmp[2])*(tmp[5]-tmp[4]);
                        }
                    }
                }
            }
            for (int i=0; i<field.meshObstVolume.length; i++)
                field.meshObstVolume[i] = (double) field.meshObstVolume[i]/(field.meshGridNum[i]*gridVol);

            double x=(field.fireLocation[0]+field.fireLocation[1])/2;
            double y=(field.fireLocation[2]+field.fireLocation[3])/2;
            double z=(field.fireLocation[4]+field.fireLocation[5])/2;
            double dist1=Math.sqrt(Math.pow((field.size[0]+anx)/2-x,2)+Math.pow((field.size[2]+any)/2-y,2)
                    +Math.pow((field.size[4]+anz)/2-z,2));
            double dist2=Math.sqrt(Math.pow((anx+field.size[1])/2-x,2)+Math.pow((field.size[2]+any)/2-y,2)
                    +Math.pow((field.size[4]+anz)/2-z,2));
            double dist3=Math.sqrt(Math.pow((field.size[0]+anx)/2-x,2)+Math.pow((any+field.size[3])/2-y,2)
                    +Math.pow((field.size[4]+anz)/2-z,2));
            double dist4=Math.sqrt(Math.pow((anx+field.size[1])/2-x,2)+Math.pow((any+field.size[3])/2-y,2)
                    +Math.pow((field.size[4]+anz)/2-z,2));
            double dist5=Math.sqrt(Math.pow((field.size[0]+anx)/2-x,2)+Math.pow((field.size[2]+any)/2-y,2)
                    +Math.pow((anz+field.size[5])/2-z,2));
            double dist6=Math.sqrt(Math.pow((anx+field.size[1])/2-x,2)+Math.pow((field.size[2]+any)/2-y,2)
                    +Math.pow((anz+field.size[5])/2-z,2));
            double dist7=Math.sqrt(Math.pow((field.size[0]+anx)/2-x,2)+Math.pow((any+field.size[3])/2-y,2)
                    +Math.pow((anz+field.size[5])/2-z,2));
            double dist8=Math.sqrt(Math.pow((anx+field.size[1])/2-x,2)+Math.pow((any+field.size[3])/2-y,2)
                    +Math.pow((anz+field.size[5])/2-z,2));
            field.distFromFire = new double[]{dist1,dist2,dist3,dist4,dist5,dist6,dist7,dist8};
        }
        return field;
    }

    private static Field initField(List<String> input, int num) {
        if (num!=2 && num!=4 && num!=8) {
            System.out.println("系统目前仅支持2、4、8个分区，请重新选择分区数");
            return null;
        }
        Field field = new Field(num);
        Iterator<String> iter = input.iterator();
        int seq = 0;
        while (iter.hasNext()) {
            String str = iter.next();
            if (str.substring(0, 4).equalsIgnoreCase("MESH")) {
                int ijx = str.indexOf("IJK");
                int xb = str.indexOf("XB");
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                Matcher matcher = pattern.matcher(str);
                if (ijx < xb) {
                    int i = 0;
                    while (matcher.find()) {
                        if (i<3)
                            field.gridNum[i] = Integer.parseInt(matcher.group());
                        if (i>=3)
                            field.size[i-3] = Float.valueOf(matcher.group().trim()).doubleValue();
                        i++;
                    }
                } else {
                    int i = 0;
                    while (matcher.find()) {
                        if (i<6)
                            field.size[i] = Float.valueOf(matcher.group().trim()).doubleValue();
                        if (i>=6)
                            field.gridNum[i-6] = Integer.parseInt(matcher.group());
                        i++;
                    }
                }
            }
            if (str.substring(0, 4).equalsIgnoreCase("OBST")  &&
                    !(str.contains("WALL") || str.contains("SHELF"))){
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                Matcher matcher = pattern.matcher(str);
                int j = 0;
                boolean flag = false;
                double[] obst = new double[6];
                while (matcher.find()) {
                    obst[j++] = Float.valueOf(matcher.group().trim()).doubleValue();
                    flag = true;
                }
                if (flag)
                    field.obstruction.put(seq++, obst);
            }
            if (str.substring(0, 4).equalsIgnoreCase("VENT")  &&
                    (str.contains("BURNER") ||
                            str.contains("Burner") ||
                            str.contains("burner"))){
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                Matcher matcher = pattern.matcher(str);
                int i = 0;
                while (matcher.find()) {
                    field.fireLocation[i++] = Float.valueOf(matcher.group().trim()).doubleValue();
                }
            }
        }
        field.meshCut = new int[]{1, 1, 1};
        field.meshGridNum = new int[num];
        field.borderGridNum = new int[num];
        field.meshObstVolume = new double[num];
        field.distFromFire = new double[num];
        return field;
    }

    private static List<String> readFile(String filepath) {
        /*
         * @param fds file path
         * @return fds文件每行string的集合 List
         *  读取fds文件，以行为单位存入一个ArrayList内
         */
        List<String> list = new ArrayList<>();
        String encoding = "GBK";
        File file = new File(filepath);
        try {
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader br = new BufferedReader(read);
                String line = null;
                Pattern ptstart = Pattern.compile("^&");
                while ((line = br.readLine()) != null) {
                    if (ptstart.matcher(line).find()) {
                        if (line.contains("/"))
                            list.add(line.substring(1, line.indexOf("/")));
                        else {
                            StringBuilder tmp = new StringBuilder(line.substring(1));
                            while (!(line=br.readLine()).contains("/")) {
                                tmp.append(", ");
                                tmp.append(line);
                            }
                            tmp.append(", ");
                            tmp.append(line.substring(0, line.indexOf("/")));
                            list.add(tmp.toString());
                        }
                    }
                }
                br.close();
                read.close();
            } else {
                System.out.println("找不到指定的文件");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("文件读取出错");
        }
        System.out.println("本系统仅支持2、4、8个分区");
        return list;
    }

    public static double[] partOfsum(double[] arr) {
        double sum = 0;
        for (double ele: arr)
            sum += ele;
        double[] res = new double[arr.length];
        for (int i=0; i<arr.length; i++)
            res[i] = arr[i]/sum;
        return res;
    }

    public static double[] partOfsum(int[] arr) {
        double sum = 0;
        for (int ele: arr)
            sum += ele;
        double[] res = new double[arr.length];
        for (int i=0; i<arr.length; i++)
            res[i] = (double) arr[i]/sum;
        return res;
    }

    private static double tolerance(double[] load) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        for (double s: load) {
            if (s<min)
                min = s;
            if (s>max)
                max = s;
        }
        return Math.abs(max-min);
    }

    static class Field {
        double[] size = new double[6]; //整个场模型三个方向头尾坐标
        int[] gridNum = new int[3]; //场模型三个方向的网格数
        HashMap<Integer, double[]> obstruction = new HashMap<>(); //可燃物三方向坐标数组的集合
        double[] fireLocation = new double[6]; //火源位置坐标
        int[] meshCut = new int[3]; //三个方向上网格划分线的位置
        int meshNum; //划分多少个子区域
        // 负载均衡 4因子
        int[] meshGridNum;
        int[] borderGridNum;
        double[] meshObstVolume;
        double[] distFromFire;
        Field(int num) {
            this.meshNum = num;
        }
    }

}
