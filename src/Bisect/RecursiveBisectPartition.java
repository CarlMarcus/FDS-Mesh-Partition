package Bisect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecursiveBisectPartition {

    public static void main(String[] args) {
        int num =1; //1是二分，2是四分，3是八分，4是16分，5是32分，6是64分
        String path = "C:\\Users\\gongz\\Desktop\\warehouse\\warehouse.fds";
        List<String> list = readFile(path);
        Field field = initField(list);
        Mesh mesh0 = initMesh(field);
        Queue<Mesh> queue = new LinkedList<>();
        queue.offer(mesh0);
        while (num-- > 0) {
            int len = queue.size();
            for (int i=0; i<len; i++) {
                Mesh[] res = partition(field, queue.poll());
                queue.offer(res[0]);
                queue.offer(res[1]);
            }
        }
        Iterator<Mesh> meshes = queue.iterator();
        while (meshes.hasNext()) {
            System.out.println(meshes.next().toString());
        }
    }

    private static List<String> readFile(String filepath) {
        List<String> list = new ArrayList<>();
        String encoding = "GBK";
        File file = new File(filepath);
        try {
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader br = new BufferedReader(read);
                String line;
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
        return list;
    }

    // 把readFile读到的信息全部丢到一个field对象里，后面只从field对象拿数据
    private static Field initField(List<String> input) {
        Field field = new Field();
        Iterator<String> iter = input.iterator();
        while (iter.hasNext()) {
            String str = iter.next();
            // 读场模型的坐标和网格数
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
                        if (i>=3) {
                            field.size[i - 3] = Float.valueOf(matcher.group().trim()).floatValue();
                        }
                        i++;
                    }
                } else {
                    int i = 0;
                    while (matcher.find()) {
                        if (i<6)
                            field.size[i] = Float.valueOf(matcher.group().trim()).floatValue();
                        if (i>=6)
                            field.gridNum[i-6] = Integer.parseInt(matcher.group());
                        i++;
                    }
                }
            }
            // 读取所有可燃物的坐标，存hashmap
            if (str.substring(0, 4).equalsIgnoreCase("OBST")  &&
                    !(str.contains("WALL") || str.contains("SHELF"))){
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                Matcher matcher = pattern.matcher(str);
                int j = 0;
                boolean flag = false;
                double[] obst = new double[6];
                while (matcher.find()) {
                    obst[j++] = Float.valueOf(matcher.group().trim()).floatValue();
                    flag = true;
                }
                if (flag)
                    field.obstruction.add(obst);
            }
            //获取火源位置信息
            if (str.substring(0, 4).equalsIgnoreCase("VENT")  &&
                    (str.contains("BURNER") || str.contains("Burner") || str.contains("burner"))){
                double[] location = new double[6];
                Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
                Matcher matcher = pattern.matcher(str);
                int i = 0;
                while (matcher.find()) {
                    field.fireLocation[i++] = Float.valueOf(matcher.group().trim()).floatValue();
                }
            }
        }
        return field;
    }

    // 最开始的母分区，源自整个field
    private static Mesh initMesh(Field field) {
        return new Mesh(field);
    }

    //必须是经过initMesh生成的mesh及其派生mesh
    //计算当前方向上按当前方案分区时的四个值
    private static Mesh factors(Field field, Mesh mesh, int direction, int cut) {
        if (mesh.gridNum[direction]<=cut || cut<1) {
            throw new ArrayIndexOutOfBoundsException("计算负载影响因子时，切分位置溢出mesh网格");
        }

        double[] meshGridNum = new double[2];
        double[] borderGridNum = new double[2];
        double[] meshObstVolume = new double[2];
        double[] distFromFire = new double[2];
        boolean[] comm = mesh.isComm;

        // 网格在x、y、z方向上的尺寸
        double x = (mesh.size[1]-mesh.size[0])/mesh.gridNum[0];
        double y = (mesh.size[3]-mesh.size[2])/mesh.gridNum[1];
        double z = (mesh.size[5]-mesh.size[4])/mesh.gridNum[2];

        if (direction==0) {
            meshGridNum[0] = cut*mesh.gridNum[1]*mesh.gridNum[2];
            meshGridNum[1] = (mesh.gridNum[0]-cut)*mesh.gridNum[1]*mesh.gridNum[2];
            borderGridNum[0] = mesh.gridNum[1]*mesh.gridNum[2]+(comm[1] ? mesh.gridNum[1]*mesh.gridNum[2]:0)+
                    (comm[2] ? cut*mesh.gridNum[2]:0)+(comm[3] ? cut*mesh.gridNum[2]:0)+
                    (comm[4] ? cut*mesh.gridNum[1]:0)+(comm[5] ? cut*mesh.gridNum[1]:0);
            borderGridNum[1] = mesh.gridNum[1]*mesh.gridNum[2]+(comm[0] ? mesh.gridNum[1]*mesh.gridNum[2]:0)+
                    (comm[2] ? (mesh.gridNum[0]-cut)*mesh.gridNum[2]:0)+(comm[3] ? (mesh.gridNum[0]-cut)*mesh.gridNum[2]:0)+
                    (comm[4] ? (mesh.gridNum[0]-cut)*mesh.gridNum[1]:0)+(comm[5] ? (mesh.gridNum[0]-cut)*mesh.gridNum[1]:0);
            double[] mesh1p1 = new double[]{mesh.size[0],mesh.size[2],mesh.size[4]};
            double[] mesh1p2 = new double[]{mesh.size[0]+cut*x,mesh.size[3],mesh.size[5]};
            double[] mesh2p1 = new double[]{mesh.size[0]+cut*x,mesh.size[2],mesh.size[4]};
            double[] mesh2p2 = new double[]{mesh.size[1],mesh.size[3],mesh.size[5]};
            for (int i=0; i<field.obstruction.size(); i++) {
                double[] obst = field.obstruction.get(i);
                double[] o1 = new double[]{obst[0],obst[2],obst[4]};
                double[] o2 = new double[]{obst[1],obst[3],obst[5]};
                meshObstVolume[0] += obstinMesh(mesh1p1, mesh1p2, o1, o2);
                meshObstVolume[1] += obstinMesh(mesh2p1, mesh2p2, o1, o2);
            }
            double v1 = meshGridNum[0]*x*y*z;
            double v2 = meshGridNum[1]*x*y*z;
            meshObstVolume[0] /= v1;
            meshObstVolume[1] /= v2;

            double[] mesh1xb = new double[]{mesh.size[0],mesh.size[0]+cut*x,mesh.size[2],mesh.size[3],mesh.size[4],mesh.size[5]};
            double[] mesh2xb = new double[]{mesh.size[0]+cut*x,mesh.size[1],mesh.size[2],mesh.size[3],mesh.size[4],mesh.size[5]};
            distFromFire[0] = distance(mesh1xb,field.fireLocation);
            distFromFire[1] = distance(mesh2xb,field.fireLocation);
        }
        if (direction==1) {
            meshGridNum[0] = cut*mesh.gridNum[0]*mesh.gridNum[2];
            meshGridNum[1] = (mesh.gridNum[1]-cut)*mesh.gridNum[0]*mesh.gridNum[2];
            borderGridNum[0] = mesh.gridNum[0]*mesh.gridNum[2]+(comm[3] ? mesh.gridNum[0]*mesh.gridNum[2]:0)+
                    (comm[0] ? cut*mesh.gridNum[2]:0)+(comm[1] ? cut*mesh.gridNum[2]:0)+
                    (comm[4] ? cut*mesh.gridNum[0]:0)+(comm[5] ? cut*mesh.gridNum[0]:0);
            borderGridNum[1] = mesh.gridNum[0]*mesh.gridNum[2]+(comm[2] ? mesh.gridNum[0]*mesh.gridNum[2]:0)+
                    (comm[0] ? (mesh.gridNum[1]-cut)*mesh.gridNum[2]:0)+(comm[1] ? (mesh.gridNum[1]-cut)*mesh.gridNum[2]:0)+
                    (comm[4] ? (mesh.gridNum[1]-cut)*mesh.gridNum[0]:0)+(comm[5] ? (mesh.gridNum[1]-cut)*mesh.gridNum[0]:0);
            double[] mesh1p1 = new double[]{mesh.size[0],mesh.size[2],mesh.size[4]};
            double[] mesh1p2 = new double[]{mesh.size[1],mesh.size[2]+cut*y,mesh.size[5]};
            double[] mesh2p1 = new double[]{mesh.size[0],mesh.size[2]+cut*y,mesh.size[4]};
            double[] mesh2p2 = new double[]{mesh.size[1],mesh.size[3],mesh.size[5]};
            for (int i=0; i<field.obstruction.size(); i++) {
                double[] obst = field.obstruction.get(i);
                double[] o1 = new double[]{obst[0],obst[2],obst[4]};
                double[] o2 = new double[]{obst[1],obst[3],obst[5]};
                meshObstVolume[0] += obstinMesh(mesh1p1, mesh1p2, o1, o2);
                meshObstVolume[1] += obstinMesh(mesh2p1, mesh2p2, o1, o2);
            }
            double v1 = meshGridNum[0]*x*y*z;
            double v2 = meshGridNum[1]*x*y*z;
            meshObstVolume[0] /= v1;
            meshObstVolume[1] /= v2;

            double[] mesh1xb = new double[]{mesh.size[0],mesh.size[1],mesh.size[2],mesh.size[2]+cut*y,mesh.size[4],mesh.size[5]};
            double[] mesh2xb = new double[]{mesh.size[0],mesh.size[1],mesh.size[2]+cut*y,mesh.size[3],mesh.size[4],mesh.size[5]};
            distFromFire[0] = distance(mesh1xb,field.fireLocation);
            distFromFire[1] = distance(mesh2xb,field.fireLocation);
        }
        if (direction==2) {
            meshGridNum[0] = cut*mesh.gridNum[1]*mesh.gridNum[0];
            meshGridNum[1] = (mesh.gridNum[2]-cut)*mesh.gridNum[1]*mesh.gridNum[0];
            borderGridNum[0] = mesh.gridNum[0]*mesh.gridNum[1]+(comm[5] ? mesh.gridNum[0]*mesh.gridNum[1]:0)+
                    (comm[0] ? cut*mesh.gridNum[1]:0)+(comm[1] ? cut*mesh.gridNum[1]:0)+
                    (comm[2] ? cut*mesh.gridNum[0]:0)+(comm[3] ? cut*mesh.gridNum[0]:0);
            borderGridNum[1] = mesh.gridNum[0]*mesh.gridNum[1]+(comm[4] ? mesh.gridNum[0]*mesh.gridNum[1]:0)+
                    (comm[0] ? (mesh.gridNum[2]-cut)*mesh.gridNum[1]:0)+(comm[1] ? (mesh.gridNum[2]-cut)*mesh.gridNum[1]:0)+
                    (comm[2] ? (mesh.gridNum[2]-cut)*mesh.gridNum[0]:0)+(comm[3] ? (mesh.gridNum[2]-cut)*mesh.gridNum[0]:0);
            double[] mesh1p1 = new double[]{mesh.size[0],mesh.size[2],mesh.size[4]};
            double[] mesh1p2 = new double[]{mesh.size[1],mesh.size[3],mesh.size[4]+cut*z};
            double[] mesh2p1 = new double[]{mesh.size[0],mesh.size[2],mesh.size[4]+cut*z};
            double[] mesh2p2 = new double[]{mesh.size[1],mesh.size[3],mesh.size[5]};
            for (int i=0; i<field.obstruction.size(); i++) {
                double[] obst = field.obstruction.get(i);
                double[] o1 = new double[]{obst[0],obst[2],obst[4]};
                double[] o2 = new double[]{obst[1],obst[3],obst[5]};
                meshObstVolume[0] += obstinMesh(mesh1p1, mesh1p2, o1, o2);
                meshObstVolume[1] += obstinMesh(mesh2p1, mesh2p2, o1, o2);
            }
            double v1 = meshGridNum[0]*x*y*z;
            double v2 = meshGridNum[1]*x*y*z;
            meshObstVolume[0] /= v1;
            meshObstVolume[1] /= v2;

            double[] mesh1xb = new double[]{mesh.size[0],mesh.size[1],mesh.size[2],mesh.size[3],mesh.size[4],mesh.size[4]+cut*z};
            double[] mesh2xb = new double[]{mesh.size[0],mesh.size[1],mesh.size[2],mesh.size[3],mesh.size[4]+cut*z,mesh.size[5]};
            distFromFire[0] = distance(mesh1xb,field.fireLocation);
            distFromFire[1] = distance(mesh2xb,field.fireLocation);
        }

        mesh.meshGridNum = ratio(meshGridNum);
        mesh.borderGridNum = ratio(borderGridNum);
        mesh.meshObstVolume = meshObstVolume;
        mesh.distFromFire = ratio(distFromFire);

        return mesh;
    }

    private static double[] loads(Mesh mesh) {
        double[] load = new double[2];
        for (int i=0; i<2; i++) {
            load[i] = mesh.meshGridNum[i]*(1+Math.pow(mesh.borderGridNum[i],(double) 1/3))
                    *(1+3*Math.pow(mesh.meshObstVolume[i],(double) 1/3))/(1+Math.pow(mesh.distFromFire[i],(double) 1/3));
        }
        /*
        System.out.println("meshGridNum:"+mesh.meshGridNum[0]+" "+mesh.meshGridNum[1]+" borderGridNum:"+mesh.borderGridNum[0]
                +" "+mesh.borderGridNum[1]+" meshObstVolume:"+mesh.meshObstVolume[0]+" "+mesh.meshObstVolume[1]+" distFromFire:"+
                mesh.distFromFire[0]+" "+mesh.distFromFire[1]+" load:"+load[0]+" "+load[1]);
         */
        return load;
    }

    private static Mesh[] partition(Field field,Mesh mesh) {
        //确定在哪个方向上切分，0表示x轴上切分，1表示y轴上切分，2表示z轴上切分
        int direction = direction(mesh);

        //找到切分负载最均衡的位置
        int res = 1;
        TreeMap<Double, Integer> map = new TreeMap<>();
        // 假设这方向有20个网格，那cut只有19个选择，即cut左边是1~19个格子，cut值代表左边有多少格子
        for (int cut=1; cut<mesh.gridNum[direction]; cut++) {
            double[] load = loads(factors(field, mesh, direction, cut));
            /*
            System.out.println("gridnum:"+mesh.meshGridNum[0]+" & "+mesh.meshGridNum[1]+" bordernum:"+mesh.borderGridNum[0]+" & "+mesh.borderGridNum[1]
            +" obst:"+mesh.meshObstVolume[0]+" & "+mesh.meshObstVolume[1]+" firedist:"+mesh.distFromFire[0]+" & "+mesh.distFromFire[1]+" load:"
            +load[0]+" & "+load[1]);
            */
            map.put(Math.abs(load[0]-load[1])/(load[0]+load[1]), cut);
        }
        res= map.get(map.firstKey());
        //生成这个切分点切出来的两个child mesh，并把两个子mesh的大小坐标，网格数，哪些面是边界的信息补齐
        double[] size1 = mesh.size.clone();
        size1[2*direction+1] = mesh.size[2*direction]+res*(mesh.size[2*direction+1]-mesh.size[2*direction])/mesh.gridNum[direction];
        int[] gridNum1 = mesh.gridNum.clone();
        gridNum1[direction] = res;
        boolean[] isComm1 = mesh.isComm.clone();
        isComm1[2*direction] = true;

        double[] size2 = mesh.size.clone();
        size2[2*direction] = mesh.size[2*direction]+res*(mesh.size[2*direction+1]-mesh.size[2*direction])/mesh.gridNum[direction];
        int[] gridNum2 = mesh.gridNum.clone();
        gridNum2[direction] = mesh.gridNum[direction]-res;
        boolean[] isComm2 = mesh.isComm.clone();
        isComm2[2*direction+1] = true;

        Mesh mesh1 = new Mesh(size1,gridNum1,isComm1);
        Mesh mesh2 = new Mesh(size2,gridNum2,isComm2);
        return new Mesh[]{mesh1, mesh2};
    }

    //求相交体积，没有返回0
    private static double obstinMesh(double[] m1, double[] m2, double[] o1, double[] o2) {
        //判断obst和mesh有没有相交
        if (Math.max(m1[0], o1[0])<Math.min(m2[0],o2[0]) && Math.max(m1[1],o1[1])<Math.min(m2[1],o2[1])
                && Math.max(m1[2],o1[2])<Math.min(m2[2],o2[2]))
            return (Math.min(m2[0],o2[0])-Math.max(m1[0], o1[0])) * (Math.min(m2[1],o2[1])-Math.max(m1[1],o1[1]))
                    * (Math.min(m2[2],o2[2])-Math.max(m1[2],o1[2]));
        return 0;
    }

    private static double distance(double[] a, double[] b) {
        return Math.sqrt(Math.pow((a[1]+a[0])/2-(b[1]+b[0])/2,2)+
                Math.pow((a[2]+a[3])/2-(b[2]+b[3])/2,2)+Math.pow((a[4]+a[5])/2-(b[4]+b[5])/2,2));
    }

    private static double[] ratio(double[] x) {
        double sum = 0;
        for (int i=0; i< x.length; i++) {
            sum+= x[i];
        }
        double[] res = new double[x.length];
        for (int i=0; i<x.length; i++)
            res[i] = x[i]/sum;
        return res;
    }

    private static int direction(Mesh mesh) {
        if (mesh.gridNum[2]>mesh.meshGridNum[1] && mesh.gridNum[2]>mesh.gridNum[0]) {
            return 2;
        } else if (mesh.gridNum[0]>mesh.gridNum[1] && mesh.gridNum[0]>mesh.gridNum[2]) {
            return 0;
        } else {
            return 1;
        }
    }

}
