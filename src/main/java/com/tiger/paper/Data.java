package com.tiger.paper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Data {

    public static final int MAX_GEN = 1000;//最大的迭代次数
    public static final int N = 200;//每次搜索领域的个数
    public static final int ll = 20;//禁忌长度
    public static int cityNum = 10;//城市数量，手动设置
    public static int jinji[][] = new int[ll][cityNum];//禁忌表
    public static int[] Ghh = new int[cityNum];//初始路径编码
    public static int bestGh[] = new int[cityNum];//最好的路径编码
    public static int[] LocalGh = new int[cityNum];//当前最好路径编码
    public static int[] tempGh = new int[cityNum];//存放临时编码
    public static int bestT;//最佳的迭代次数
    public static int bestEvaluation;
    public static int LocalEvaluation;
    public static int tempEvaluation;
    public static int point[][] = new int[cityNum][2];//每个城市的坐标
    public static int dist[][] = new int[cityNum][cityNum];//距离矩阵
    public static int t;//当前迭代
    public static Random random;

    private static int[][] cityPosition = {{0, 0}, {12, 32}, {5, 25}, {8, 45}, {33, 17},
            {25, 7}, {15, 15}, {15, 25}, {25, 15}, {41, 12}};

    //读取数据并初始化
    public static void read_data(String filepath) throws FileNotFoundException {
//		String line=null;
//		String substr[]=null;
//		Scanner cin=new Scanner(new BufferedReader(new FileReader(filepath)));
//		for (int i = 0; i < cityNum; i++) {
//			line=cin.nextLine();
//			line.trim();
//			substr=line.split(" ");
//			 point[i][0]=Integer.parseInt(substr[1]);//x坐标
//			 point[i][1]=Integer.parseInt(substr[2]);//y坐标
//		}
//		cin.close();
//		//计算距离矩阵，注意这里的计算方式，才用的是伪欧式距离
//		for (int i = 0; i < cityNum; i++) {
//			dist[i][i]=0;//对角线元素为0
//			for (int j = i+1; j < cityNum; j++) {
//				double rij=Math.sqrt((Math.pow(point[i][0]-point[j][0], 2)+
//						             Math.pow(point[i][1]-point[j][1], 2))/10.0);
//				//rij四舍五入取整
//				int tij=(int) Math.round(rij);
//				if(tij<rij) {
//					dist[i][j]=tij+1;
//					dist[j][i]=dist[i][j];
//				}else {
//					dist[i][j]=tij;
//					dist[j][i]=dist[i][j];
//				}
//			}
//		}
//		dist[cityNum-1][cityNum-1]=0;
        //初始化城市距离
        for (int i = 0; i < cityPosition.length; i++) {
            for (int j = i; j < cityPosition.length; j++) {
                dist[i][j] = (int) Math.sqrt(Math.pow(cityPosition[i][0] - cityPosition[j][0], 2) + Math.pow(cityPosition[i][1] - cityPosition[j][1], 2));
                dist[j][i] = dist[i][j];
            }
        }
        t = 0;
        bestT = 0;
        bestEvaluation = Integer.MAX_VALUE;
        LocalEvaluation = Integer.MAX_VALUE;
        tempEvaluation = Integer.MAX_VALUE;
        random = new Random(System.currentTimeMillis());
    }

    public static void main(String[] args) {
//
//        StringBuilder s = new StringBuilder("[0.916307225090853, 0.903710584902681, 0.85193756908761, 0.983633029018757, 0.853689666202406, 0.925395682455334, 0.884069812746669, 0.871753652260858, 0.956577031997943, 0.999487817983323]");
//        String substring = s.substring(1, s.length() - 1);
//        String[] split = substring.toString().split(", ");
//        ArrayList<Double> doubles = new ArrayList<>();
//        for (String s1 : split) {
//            doubles.add(Double.valueOf(s1));
//        }
        double f = 1.999999639718497;
        double fg = 1.999999639718497;
        double fw = 1.999666093410115;
        double globalMin = 0.981726888883255;
        double temp = 0.999399765460382;
        double globalMax = 0.999399765460382;
        double sparrowIndex;
        if (f > fg) {
            do {
                sparrowIndex = globalMax + randomNormalDistribution() * Math.abs(globalMax - temp);
            } while (sparrowIndex < 0 || sparrowIndex > 1);
        } else if (Math.abs(f - fg) <= 1e-10) {
            double abs = Math.abs(temp - globalMin);
            double v = fw - f + 1e-18;
//            int i = getNumberDecimalDigits(abs);
//            int j = getNumberDecimalDigits(v);
//            int t = Math.abs(i - j);
//            double pow = Math.pow(10, t);
//            if (i < j) {
//                v *= pow;
//            } else if (i > j) {
//                abs *= pow;
//            }
            double v1 = abs / v;
            while (Math.abs(v1) > 10) {
                v1 /= 10;
            }
            do {
                sparrowIndex = temp + Math.random() * v1;
            } while (sparrowIndex < 0 || sparrowIndex > 1);
        }
    }

    private static double randomNormalDistribution() {
        double u = 0.0d;
        double v = 0.0d;
        double w = 0.0d;
        double c = 0.0d;
        do {
            //获得两个（-1,1）的独立随机变量
            u = Math.random() * 2 - 1.0;
            v = Math.random() * 2 - 1.0;
            w = u * u + v * v;
        } while (w == 0.0 || w >= 1.0);
        //这里就是 Box-Muller转换
        c = Math.sqrt((-2 * Math.log(w)) / w);
        //返回2个标准正态分布的随机数，封装进一个数组返回
        //当然，因为这个函数运行较快，也可以扔掉一个
        //return [u*c,v*c];
        return u * c;
    }

    private static int getNumberDecimalDigits(Double balance) {
        int dcimalDigits = 0;
        String balanceStr = Double.toString(balance);
        int indexOf = balanceStr.indexOf(".");
        int e = balanceStr.indexOf('E');
        if (e == -1) {
            dcimalDigits = balanceStr.length() - 1 - indexOf;
        } else {
            dcimalDigits = e - indexOf - 1 + Integer.parseInt(balanceStr.substring(e + 2));
        }
        return dcimalDigits;
    }
}