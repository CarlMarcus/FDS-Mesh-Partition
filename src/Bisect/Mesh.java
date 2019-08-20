package Bisect;

public class Mesh {

    double[] size;// double[6];
    int[] gridNum;// int[3];
    boolean[] isComm;// {false,false,false,false,false,false}; 0/1垂直x轴，2/3垂直y轴，4/5垂直z轴，都是先正后负

    //如果对该分区进行二分，两个子分区对应的四个因子存放在下面四个数组
    double[] meshGridNum = new double[2]; //网格总数目
    double[] borderGridNum = new double[2]; //边界网格数
    double[] meshObstVolume = new double[]{0,0}; //可燃物体积
    double[] distFromFire = new double[2]; //距离火源的位置

    Mesh() {}
    Mesh(double[] size, int[] gridNum, boolean[] isComm) {
        this.size = size;
        this.gridNum = gridNum;
        this.isComm = isComm;
    }
    Mesh(Field field) {
        this.size = field.size;
        this.gridNum = field.gridNum;
        this.isComm = new boolean[]{false,false,false,false,false,false};
    }

    @Override
    public String toString() {
        return "&MESH IJK="+perf(this.gridNum[0])+","+perf(this.gridNum[1])+","+perf(this.gridNum[2])+","
                +"XB="+perfect(this.size[0])+", "+perfect(this.size[1])+", "+perfect(this.size[2])+", "+perfect(this.size[3])+", "
                +perfect(this.size[4])+", "+perfect(this.size[5])+"/";
    }

    public int perf(double num) {
        return (int) Math.round(num);
    }
    public float perfect(double num) {
        return (float)Math .round(num*10)/10;
    }

}
