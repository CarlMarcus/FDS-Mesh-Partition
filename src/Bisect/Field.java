package Bisect;

import java.util.ArrayList;

public class Field {

    double[] size = new double[6]; //整个场模型三个方向头尾坐标
    int[] gridNum = new int[3]; //场模型三个方向的网格数
    ArrayList<double[]> obstruction = new ArrayList<>(); //可燃物三方向坐标数组的集合
    double[] fireLocation = new double[6]; //火源位置坐标

    Field(){}

}
